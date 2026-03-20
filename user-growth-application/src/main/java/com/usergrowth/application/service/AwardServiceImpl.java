package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.api.dto.ExchangeMessage;
import com.usergrowth.api.dto.ExchangeResultVO;
import com.usergrowth.infrastructure.mapper.*;
import com.usergrowth.infrastructure.mq.ExchangeTransactionProducer;
import com.usergrowth.infrastructure.po.*;
import com.usergrowth.infrastructure.redis.AwardStockRedisService;
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
    private final AwardStockRedisService awardStockRedisService;
    private final ExchangeTransactionProducer transactionProducer;

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
        if (award.getExpireTime() != null
                && LocalDateTime.now().isAfter(award.getExpireTime())) {
            throw new RuntimeException("活动已结束");
        }

        // 3. 校验用户积分是否充足
        PointAccountPO account = pointAccountMapper.selectByUserId(userId);
        if (account == null || account.getBalance() < award.getPointsCost()) {
            throw new RuntimeException("积分不足");
        }

        // 4. 生成兑换ID（雪花算法）
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

        // 6. 根据是否允许超卖走不同路径
        if (award.getAllowOversell() == 1) {
            // ========== 允许超卖路径：Redis DECR ==========
            return exchangeWithOversell(userId, awardId, award, exchangeId);
        } else {
            // ========== 不允许超卖路径：库存拆分 + 乐观锁 ==========
            return exchangeWithoutOversell(userId, awardId, award, exchangeId);
        }
    }

    /**
     * 允许超卖场景：Redis DECR 扣减库存
     * 优点：性能极高，无锁竞争
     * 缺点：Redis 宕机或主从切换可能导致超卖
     */
    private boolean exchangeWithOversell(Long userId, Long awardId,
                                         AwardPO award, Long exchangeId) {
        // 1. Redis DECR 扣减库存
        Long remaining = awardStockRedisService.decrStock(awardId);
        if (remaining == null || remaining < 0) {
            // 库存不足，回滚 Redis 库存
            awardStockRedisService.incrStock(awardId);
            // 更新兑换记录为失败
            updateExchangeStatus(exchangeId, 2);
            throw new RuntimeException("库存不足，兑换失败");
        }

        // 2. 数据库事务：扣减积分 + 发放奖品
        boolean pointsDeducted = deductPoints(userId, award.getPointsCost(), 3);
        if (!pointsDeducted) {
            // 积分扣减失败，回滚 Redis 库存
            awardStockRedisService.incrStock(awardId);
            updateExchangeStatus(exchangeId, 2);
            throw new RuntimeException("积分扣减失败，请重试");
        }

        // 3. 发放奖品
        UserAwardPO userAward = new UserAwardPO();
        userAward.setUserId(userId);
        userAward.setAwardId(awardId);
        userAward.setExchangeId(exchangeId);
        userAwardMapper.insert(userAward);

        // 4. 更新兑换记录为成功
        updateExchangeStatus(exchangeId, 1);

        // 5. 删除积分余额缓存
        redisTemplate.delete("point:balance:" + userId);

        log.info("超卖场景兑换成功，userId={}，awardId={}，剩余库存={}",
                userId, awardId, remaining);
        return true;
    }

    /**
     * 不允许超卖场景：库存拆分 + 乐观锁
     */
    private boolean exchangeWithoutOversell(Long userId, Long awardId,
                                            AwardPO award, Long exchangeId) {
        // 1. 扣减库存（乐观锁 + 随机路由）
        boolean stockDeducted = deductInventory(awardId, 5);
        if (!stockDeducted) {
            updateExchangeStatus(exchangeId, 2);
            throw new RuntimeException("库存不足，兑换失败");
        }

        // 2. 扣减积分
        boolean pointsDeducted = deductPoints(userId, award.getPointsCost(), 3);
        if (!pointsDeducted) {
            updateExchangeStatus(exchangeId, 2);
            throw new RuntimeException("积分扣减失败，请重试");
        }

        // 3. 发放奖品
        UserAwardPO userAward = new UserAwardPO();
        userAward.setUserId(userId);
        userAward.setAwardId(awardId);
        userAward.setExchangeId(exchangeId);
        userAwardMapper.insert(userAward);

        // 4. 更新兑换记录为成功
        updateExchangeStatus(exchangeId, 1);

        // 5. 删除积分余额缓存
        redisTemplate.delete("point:balance:" + userId);

        log.info("强一致性场景兑换成功，userId={}，awardId={}", userId, awardId);
        return true;
    }

    /**
     * 更新兑换记录状态
     */
    private void updateExchangeStatus(Long exchangeId, Integer status) {
        ExchangeRecordPO update = new ExchangeRecordPO();
        update.setId(exchangeId);
        update.setStatus(status);
        exchangeRecordMapper.updateById(update);
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

    /**
     * 异步兑换：MQ 事务消息，立即返回，消费者异步处理
     */
    public boolean exchangeAsync(Long userId, Long awardId) {
        // 1. 查询奖品
        AwardPO award = awardMapper.selectById(awardId);
        if (award == null || award.getStatus() != 1) {
            throw new RuntimeException("奖品不存在或已下架");
        }

        // 2. 有效期校验
        if (award.getExpireTime() != null
                && LocalDateTime.now().isAfter(award.getExpireTime())) {
            throw new RuntimeException("活动已结束");
        }

        // 3. 积分余额预检（快速失败，非精确）
        PointAccountPO account = pointAccountMapper.selectByUserId(userId);
        if (account == null || account.getBalance() < award.getPointsCost()) {
            throw new RuntimeException("积分不足");
        }

        // 4. 构建 MQ 消息
        ExchangeMessage message = new ExchangeMessage();
        message.setExchangeId(snowflakeIdGenerator.nextId());
        message.setUserId(userId);
        message.setAwardId(awardId);
        message.setPointsCost(award.getPointsCost());
        message.setAllowOversell(award.getAllowOversell());

        // 5. 发送事务消息（内部执行本地事务：插入 exchange_record）
        boolean sent = transactionProducer.sendTransactionMessage(message);
        if (!sent) {
            throw new RuntimeException("兑换请求提交失败，请重试");
        }

        log.info("兑换请求已提交MQ，exchangeId={}，请等待处理结果",
                message.getExchangeId());
        return true;
    }

    @Override
    public ExchangeResultVO getExchangeResult(Long exchangeId) {
        // 1. 查询兑换记录
        ExchangeRecordPO record = exchangeRecordMapper.selectById(exchangeId);
        if (record == null) {
            throw new RuntimeException("兑换记录不存在，exchangeId=" + exchangeId);
        }

        // 2. 查询奖品名称
        AwardPO award = awardMapper.selectById(record.getAwardId());

        // 3. 组装 VO
        ExchangeResultVO vo = new ExchangeResultVO();
        vo.setExchangeId(String.valueOf(record.getId()));
        vo.setPointsCost(record.getPointsCost());
        vo.setCreatedAt(record.getCreatedAt() != null
                ? record.getCreatedAt().toString() : "");
        vo.setAwardName(award != null ? award.getRewardName() : "未知奖品");

        // 4. 状态转换
        switch (record.getStatus()) {
            case 0 -> vo.setStatus("PENDING");
            case 1 -> vo.setStatus("SUCCESS");
            case 2 -> vo.setStatus("FAIL");
            default -> vo.setStatus("UNKNOWN");
        }

        return vo;
    }
}