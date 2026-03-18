package com.usergrowth.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwardStockRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_KEY = "award:stock:";

    /**
     * 初始化奖品库存到 Redis（运营配置奖品时调用）
     */
    public void initStock(Long awardId, Integer stock) {
        String key = STOCK_KEY + awardId;
        redisTemplate.opsForValue().set(key, stock);
        log.info("奖品库存初始化到Redis，awardId={}，stock={}", awardId, stock);
    }

    /**
     * Redis DECR 扣减库存，返回扣减后的值
     * 返回值 >= 0 说明扣减成功，< 0 说明库存不足
     */
    public Long decrStock(Long awardId) {
        String key = STOCK_KEY + awardId;
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 查询 Redis 中的剩余库存
     */
    public Integer getStock(Long awardId) {
        String key = STOCK_KEY + awardId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return null;
        return Integer.parseInt(value.toString());
    }

    /**
     * 库存超卖后补偿：回滚库存 +1
     */
    public void incrStock(Long awardId) {
        String key = STOCK_KEY + awardId;
        redisTemplate.opsForValue().increment(key);
        log.info("库存回滚+1，awardId={}", awardId);
    }
}