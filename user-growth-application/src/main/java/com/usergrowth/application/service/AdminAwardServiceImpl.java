package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.usergrowth.api.dto.*;
import com.usergrowth.infrastructure.mapper.AwardInventorySplitMapper;
import com.usergrowth.infrastructure.mapper.AwardMapper;
import com.usergrowth.infrastructure.po.AwardInventorySplitPO;
import com.usergrowth.infrastructure.po.AwardPO;
import com.usergrowth.infrastructure.redis.AwardStockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAwardServiceImpl implements AdminAwardService {

    private final AwardMapper awardMapper;
    private final AwardInventorySplitMapper inventorySplitMapper;
    private final AwardStockRedisService awardStockRedisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAward(AwardCreateRequest request) {
        // 1. 插入奖品记录
        AwardPO award = new AwardPO();
        award.setRewardName(request.getRewardName());
        award.setRewardDesc(request.getRewardDesc());
        award.setCover(request.getCover());
        award.setPointsCost(request.getPointsCost());
        award.setTotalStock(request.getTotalStock());
        award.setAllowOversell(request.getAllowOversell());
        award.setStatus(1);
        if (request.getExpireTime() != null && !request.getExpireTime().isEmpty()) {
            award.setExpireTime(LocalDateTime.parse(request.getExpireTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        awardMapper.insert(award);

        // 2. 根据超卖配置初始化库存
        if (request.getAllowOversell() == 1) {
            // 允许超卖：同步到 Redis
            awardStockRedisService.initStock(award.getId(), request.getTotalStock());
            log.info("超卖奖品库存初始化到Redis，awardId={}，stock={}",
                    award.getId(), request.getTotalStock());
        } else {
            // 不允许超卖：库存拆分存入 DB
            int splitCount = request.getSplitCount() != null ? request.getSplitCount() : 1;
            initInventorySplit(award.getId(), request.getTotalStock(), splitCount);
        }

        log.info("创建奖品成功，awardId={}，name={}", award.getId(), award.getRewardName());
        return award.getId();
    }

    @Override
    public boolean updateAward(AwardUpdateRequest request) {
        AwardPO award = awardMapper.selectById(request.getAwardId());
        if (award == null) {
            throw new RuntimeException("奖品不存在，awardId=" + request.getAwardId());
        }
        if (request.getRewardName() != null) award.setRewardName(request.getRewardName());
        if (request.getRewardDesc() != null) award.setRewardDesc(request.getRewardDesc());
        if (request.getCover() != null) award.setCover(request.getCover());
        if (request.getPointsCost() != null) award.setPointsCost(request.getPointsCost());
        if (request.getStatus() != null) award.setStatus(request.getStatus());
        if (request.getExpireTime() != null && !request.getExpireTime().isEmpty()) {
            award.setExpireTime(LocalDateTime.parse(request.getExpireTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        awardMapper.updateById(award);
        log.info("更新奖品成功，awardId={}", request.getAwardId());
        return true;
    }

    @Override
    public boolean deleteAward(Long awardId) {
        AwardPO award = awardMapper.selectById(awardId);
        if (award == null) {
            throw new RuntimeException("奖品不存在，awardId=" + awardId);
        }
        award.setStatus(0);
        awardMapper.updateById(award);
        // 若允许超卖，清除 Redis 库存
        if (award.getAllowOversell() == 1) {
            awardStockRedisService.initStock(awardId, 0);
            log.info("奖品下架，清零Redis库存，awardId={}", awardId);
        }
        log.info("奖品下架成功，awardId={}", awardId);
        return true;
    }

    @Override
    public List<AwardVO> listAllAwards() {
        List<AwardPO> awards = awardMapper.selectList(
                new LambdaQueryWrapper<AwardPO>().orderByAsc(AwardPO::getId)
        );
        return awards.stream().map(po -> {
            AwardVO vo = new AwardVO();
            vo.setId(po.getId());
            vo.setRewardName(po.getRewardName());
            vo.setRewardDesc(po.getRewardDesc());
            vo.setCover(po.getCover());
            vo.setPointsCost(po.getPointsCost());
            vo.setTotalStock(po.getTotalStock());
            Integer stock = inventorySplitMapper.selectList(
                    new LambdaQueryWrapper<AwardInventorySplitPO>()
                            .eq(AwardInventorySplitPO::getAwardId, po.getId())
            ).stream().mapToInt(AwardInventorySplitPO::getSubStock).sum();
            vo.setStock(stock);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStock(StockUpdateRequest request) {
        AwardPO award = awardMapper.selectById(request.getAwardId());
        if (award == null) {
            throw new RuntimeException("奖品不存在，awardId=" + request.getAwardId());
        }

        // 更新总库存
        award.setTotalStock(request.getTotalStock());
        awardMapper.updateById(award);

        if (award.getAllowOversell() == 1) {
            // 超卖奖品：更新 Redis
            awardStockRedisService.initStock(request.getAwardId(), request.getTotalStock());
            log.info("超卖库存更新，awardId={}，newStock={}",
                    request.getAwardId(), request.getTotalStock());
        } else {
            // 非超卖奖品：删除旧拆分记录，重新拆分
            inventorySplitMapper.delete(
                    new LambdaQueryWrapper<AwardInventorySplitPO>()
                            .eq(AwardInventorySplitPO::getAwardId, request.getAwardId())
            );
            int splitCount = request.getSplitCount() != null ? request.getSplitCount() : 1;
            initInventorySplit(request.getAwardId(), request.getTotalStock(), splitCount);
            log.info("非超卖库存重新拆分，awardId={}，splitCount={}",
                    request.getAwardId(), splitCount);
        }
        return true;
    }

    @Override
    public Integer queryStock(Long awardId) {
        AwardPO award = awardMapper.selectById(awardId);
        if (award == null) {
            throw new RuntimeException("奖品不存在，awardId=" + awardId);
        }
        if (award.getAllowOversell() == 1) {
            // 超卖奖品从 Redis 查
            Integer redisStock = awardStockRedisService.getStock(awardId);
            return redisStock != null ? redisStock : 0;
        } else {
            // 非超卖奖品从 DB 查子库存合计
            return inventorySplitMapper.selectList(
                    new LambdaQueryWrapper<AwardInventorySplitPO>()
                            .eq(AwardInventorySplitPO::getAwardId, awardId)
            ).stream().mapToInt(AwardInventorySplitPO::getSubStock).sum();
        }
    }

    /**
     * 初始化库存拆分记录
     * 将总库存 totalStock 拆成 splitCount 份存入 award_inventory_split
     */
    private void initInventorySplit(Long awardId, int totalStock, int splitCount) {
        int base = totalStock / splitCount;
        int remainder = totalStock % splitCount;
        for (int i = 0; i < splitCount; i++) {
            AwardInventorySplitPO split = new AwardInventorySplitPO();
            split.setAwardId(awardId);
            // 前 remainder 份多分 1 个，保证总量不变
            split.setSubStock(i < remainder ? base + 1 : base);
            inventorySplitMapper.insert(split);
        }
        log.info("库存拆分完成，awardId={}，totalStock={}，splitCount={}",
                awardId, totalStock, splitCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean splitStock(StockSplitRequest request) {
        AwardPO award = awardMapper.selectById(request.getAwardId());
        if (award == null) {
            throw new RuntimeException("奖品不存在，awardId=" + request.getAwardId());
        }
        if (award.getAllowOversell() == 1) {
            throw new RuntimeException("允许超卖的奖品不需要库存拆分");
        }

        // 删除旧拆分记录
        inventorySplitMapper.delete(
                new LambdaQueryWrapper<AwardInventorySplitPO>()
                        .eq(AwardInventorySplitPO::getAwardId, request.getAwardId())
        );

        // 按新的 splitCount 重新拆分
        initInventorySplit(request.getAwardId(), award.getTotalStock(), request.getSplitCount());

        log.info("库存重新拆分，awardId={}，splitCount={}",
                request.getAwardId(), request.getSplitCount());
        return true;
    }
}