package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.infrastructure.mapper.*;
import com.usergrowth.infrastructure.po.*;
import com.usergrowth.infrastructure.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements AwardService {

    private final AwardMapper awardMapper;
    private final AwardInventorySplitMapper inventorySplitMapper;
    private final ExchangeRecordMapper exchangeRecordMapper;
    private final UserAwardMapper userAwardMapper;
    private final PointAccountMapper pointAccountMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<AwardVO> getRewardList() {
        List<AwardPO> awards = awardMapper.selectList(
                new LambdaQueryWrapper<AwardPO>()
                        .eq(AwardPO::getStatus, 1)
                        .orderByAsc(AwardPO::getId)
        );
        return awards.stream().map(po -> {
            AwardVO vo = new AwardVO();
            vo.setId(po.getId());
            vo.setRewardName(po.getRewardName());
            vo.setRewardDesc(po.getRewardDesc());
            vo.setCover(po.getCover());
            vo.setPointsCost(po.getPointsCost());
            vo.setTotalStock(po.getTotalStock());
            // 实时库存：汇总子库存表
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
    public boolean exchange(Long userId, Long awardId) {
        // 1. 查询奖品信息
        AwardPO award = awardMapper.selectById(awardId);
        if (award == null || award.getStatus() != 1) {
            throw new RuntimeException("奖品不存在或已下架");
        }

        // 2. 校验活动有效期
        if (award.getExpireTime() != null && LocalDateTime.now().isAfter(award.getExpireTime())) {
            throw new RuntimeException("活动已结束");
        }

        // 3. 校验用户积分是否充足
        PointAccountPO account = pointAccountMapper.selectByUserId(userId);
        if (account == null || account.getBalance() < award.getPointsCost()) {
            throw new RuntimeException("积分不足");
        }

        // 4. 生成幂等ID（雪花算法）
        long exchangeId = snowflakeIdGenerator.nextId();

        // 5. 写入兑换记录（状态=待处理）
        ExchangeRecordPO record = new ExchangeRecordPO();
        record.setId(exchangeId);
        record.setUserId(userId);
        record.setAwardId(awardId);
        record.setIdempotentId(String.valueOf(exchangeId));
        record.setStatus(0);
        record.setPointsCost(award.getPointsCost());
        exchangeRecordMapper.insert(record);

        // 6. 扣减库存（乐观锁+随机路由，最多重试5次）
        boolean stockDeducted = deductInventory(awardId, 5);
        if (!stockDeducted) {
            throw new RuntimeException("库存不足，兑换失败");
        }

        // 7. 扣减用户积分（乐观锁）
        boolean pointsDeducted = deductPoints(userId, award.getPointsCost(), 3);
        if (!pointsDeducted) {
            throw new RuntimeException("积分扣减失败，请重试");
        }

        // 8. 发放奖品
        UserAwardPO userAward = new UserAwardPO();
        userAward.setUserId(userId);
        userAward.setAwardId(awardId);
        userAward.setExchangeId(exchangeId);
        userAwardMapper.insert(userAward);

        // 9. 更新兑换记录状态为成功
        ExchangeRecordPO update = new ExchangeRecordPO();
        update.setId(exchangeId);
        update.setStatus(1);
        exchangeRecordMapper.updateById(update);

        log.info("积分兑换成功，userId={}, awardId={}, exchangeId={}", userId, awardId, exchangeId);

        // 兑换成功后删除余额缓存
        redisTemplate.delete("point:balance:" + userId);
        return true;
    }

    // 随机路由扣减子库存，乐观锁重试
    private boolean deductInventory(Long awardId, int maxRetry) {
        for (int i = 0; i < maxRetry; i++) {
            List<AwardInventorySplitPO> splits =
                    inventorySplitMapper.selectAvailableByAwardId(awardId);
            if (splits.isEmpty()) return false;
            // 随机选一条子库存
            AwardInventorySplitPO split = splits.get(new Random().nextInt(splits.size()));
            int affected = inventorySplitMapper.deductStock(split.getId(), split.getVersion());
            if (affected > 0) return true;
        }
        return false;
    }

    // 乐观锁扣减积分
    private boolean deductPoints(Long userId, Integer points, int maxRetry) {
        for (int i = 0; i < maxRetry; i++) {
            PointAccountPO account = pointAccountMapper.selectByUserId(userId);
            if (account == null || account.getBalance() < points) return false;
            int affected = pointAccountMapper.addPoints(userId, -points, account.getVersion());
            if (affected > 0) return true;
        }
        return false;
    }
}