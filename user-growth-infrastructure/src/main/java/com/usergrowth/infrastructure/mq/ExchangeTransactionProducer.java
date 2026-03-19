package com.usergrowth.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usergrowth.api.dto.ExchangeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeTransactionProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送事务消息
     * @param message 兑换消息体
     * @return 是否发送成功
     */
    public boolean sendTransactionMessage(ExchangeMessage message) {
        try {
            String destination = MqConstants.EXCHANGE_TOPIC + ":" + MqConstants.EXCHANGE_TAG;
            String json = objectMapper.writeValueAsString(message);

            Message<String> mqMessage = MessageBuilder
                    .withPayload(json)
                    .setHeader("exchangeId", message.getExchangeId())
                    .build();

            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
                    destination, mqMessage, message);

            log.info("事务消息发送结果：exchangeId={}，status={}",
                    message.getExchangeId(), result.getLocalTransactionState());

            return result.getSendStatus() == SendStatus.SEND_OK;
        } catch (Exception e) {
            log.error("事务消息发送失败：exchangeId={}", message.getExchangeId(), e);
            return false;
        }
    }
}