package com.usergrowth.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfig {

    /**
     * 任务列表本地缓存
     * 最大 100 条，5 分钟过期
     */
    @Bean
    public Cache<String, Object> taskListCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }
}