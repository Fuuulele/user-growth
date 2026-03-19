package com.usergrowth.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usergrowth.api.dto.ExchangeMessage;
import com.usergrowth.infrastructure.mapper.ExchangeRecordMapper;
import com.usergrowth.infrastructure.po.ExchangeRecordPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class ExchangeTransactionListener implements RocketMQLocalTransactionListener {

    private final ExchangeRecordMapper exchangeRecordMapper;
    private final ObjectMapper objectMapper;

    /**
     * 执行本地事务：将兑换记录插入数据库（状态=待处理）
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(
            Message msg, Object arg) {
        try {
            ExchangeMessage message = (ExchangeMessage) arg;

            // 检查是否已存在（幂等）
            ExchangeRecordPO existing = exchangeRecordMapper.selectById(message.getExchangeId());
            if (existing != null) {
                log.info("兑换记录已存在，幂等处理，exchangeId={}", message.getExchangeId());
                return RocketMQLocalTransactionState.COMMIT;
            }

            // 插入兑换记录，状态=0 待处理
            ExchangeRecordPO record = new ExchangeRecordPO();
            record.setId(message.getExchangeId());
            record.setUserId(message.getUserId());
            record.setAwardId(message.getAwardId());
            record.setIdempotentId(String.valueOf(message.getExchangeId()));
            record.setStatus(0);
            record.setPointsCost(message.getPointsCost());
            exchangeRecordMapper.insert(record);

            log.info("本地事务执行成功，兑换记录已创建，exchangeId={}", message.getExchangeId());
            return RocketMQLocalTransactionState.COMMIT;

        } catch (Exception e) {
            log.error("本地事务执行失败，exchangeId={}",
                    msg.getHeaders().get("exchangeId"), e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 消息回查：检查本地事务是否执行成功
     * 当 MQ Server 未收到 Commit/Rollback 时触发
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        Long exchangeId = Long.parseLong(
                msg.getHeaders().get("exchangeId").toString());

        ExchangeRecordPO record = exchangeRecordMapper.selectById(exchangeId);
        if (record != null) {
            log.info("消息回查：本地事务已执行，COMMIT，exchangeId={}", exchangeId);
            return RocketMQLocalTransactionState.COMMIT;
        }

        log.warn("消息回查：本地事务未执行，ROLLBACK，exchangeId={}", exchangeId);
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}