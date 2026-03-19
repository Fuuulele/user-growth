package com.usergrowth.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usergrowth.api.dto.ExchangeMessage;
import com.usergrowth.infrastructure.mapper.AwardInventorySplitMapper;
import com.usergrowth.infrastructure.mapper.ExchangeRecordMapper;
import com.usergrowth.infrastructure.mapper.PointAccountMapper;
import com.usergrowth.infrastructure.mapper.UserAwardMapper;
import com.usergrowth.infrastructure.po.AwardInventorySplitPO;
import com.usergrowth.infrastructure.po.ExchangeRecordPO;
import com.usergrowth.infrastructure.po.PointAccountPO;
import com.usergrowth.infrastructure.po.UserAwardPO;
import com.usergrowth.infrastructure.redis.AwardStockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.EXCHANGE_TOPIC,
        selectorExpression = MqConstants.EXCHANGE_TAG,
        consumerGroup = MqConstants.EXCHANGE_CONSUMER_GROUP
)
public class ExchangeConsumer implements RocketMQListener<String> {

    private final ExchangeRecordMapper exchangeRecordMapper;
    private final PointAccountMapper pointAccountMapper;
    private final AwardInventorySplitMapper inventorySplitMapper;
    private final UserAwardMapper userAwardMapper;
    private final AwardStockRedisService awardStockRedisService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String message) {
        try {
            ExchangeMessage msg = objectMapper.readValue(message, ExchangeMessage.class);
            log.info("消费兑换消息，exchangeId={}, userId={}, awardId={}",
                    msg.getExchangeId(), msg.getUserId(), msg.getAwardId());

            // 1. 幂等检查：已成功或已失败则跳过
            ExchangeRecordPO record = exchangeRecordMapper.selectById(msg.getExchangeId());
            if (record == null || record.getStatus() != 0) {
                log.info("兑换记录不存在或已处理，跳过，exchangeId={}", msg.getExchangeId());
                return;
            }

            // 2. 根据是否允许超卖走不同路径
            boolean success;
            if (msg.getAllowOversell() == 1) {
                success = processOversell(msg);
            } else {
                success = processNoOversell(msg);
            }

            // 3. 更新兑换记录状态
            ExchangeRecordPO update = new ExchangeRecordPO();
            update.setId(msg.getExchangeId());
            update.setStatus(success ? 1 : 2);
            exchangeRecordMapper.updateById(update);

            log.info("兑换处理完成，exchangeId={}，result={}", msg.getExchangeId(), success);

        } catch (Exception e) {
            log.error("兑换消息消费异常：{}", message, e);
            throw new RuntimeException(e); // 触发重试
        }
    }

    /** 允许超卖：Redis DECR */
    private boolean processOversell(ExchangeMessage msg) {
        Long remaining = awardStockRedisService.decrStock(msg.getAwardId());
        if (remaining == null || remaining < 0) {
            awardStockRedisService.incrStock(msg.getAwardId());
            log.warn("Redis库存不足，awardId={}", msg.getAwardId());
            return false;
        }
        return deductPointsAndGrantAward(msg);
    }

    /** 不允许超卖：库存拆分 + 乐观锁 */
    private boolean processNoOversell(ExchangeMessage msg) {
        for (int i = 0; i < 5; i++) {
            List<AwardInventorySplitPO> splits =
                    inventorySplitMapper.selectAvailableByAwardId(msg.getAwardId());
            if (splits.isEmpty()) {
                log.warn("库存不足，awardId={}", msg.getAwardId());
                return false;
            }
            AwardInventorySplitPO split = splits.get(new Random().nextInt(splits.size()));
            int affected = inventorySplitMapper.deductStock(split.getId(), split.getVersion());
            if (affected > 0) {
                return deductPointsAndGrantAward(msg);
            }
        }
        return false;
    }

    /** 扣积分 + 发放奖品 */
    private boolean deductPointsAndGrantAward(ExchangeMessage msg) {
        // 乐观锁扣减积分
        for (int i = 0; i < 3; i++) {
            PointAccountPO account = pointAccountMapper.selectByUserId(msg.getUserId());
            if (account == null || account.getBalance() < msg.getPointsCost()) {
                log.warn("积分不足，userId={}", msg.getUserId());
                return false;
            }
            int affected = pointAccountMapper.addPoints(
                    msg.getUserId(), -msg.getPointsCost(), account.getVersion());
            if (affected > 0) break;
            if (i == 2) return false;
        }

        // 发放奖品
        UserAwardPO userAward = new UserAwardPO();
        userAward.setUserId(msg.getUserId());
        userAward.setAwardId(msg.getAwardId());
        userAward.setExchangeId(msg.getExchangeId());
        userAwardMapper.insert(userAward);

        // 删除积分余额缓存，保证下次查询读最新值
        redisTemplate.delete("point:balance:" + msg.getUserId());

        return true;
    }

}