package com.usergrowth.infrastructure.config;

import com.usergrowth.infrastructure.mapper.AwardMapper;
import com.usergrowth.infrastructure.po.AwardPO;
import com.usergrowth.infrastructure.redis.AwardStockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwardStockInitializer implements ApplicationRunner {

    private final AwardMapper awardMapper;
    private final AwardStockRedisService awardStockRedisService;

    /**
     * 应用启动后自动将允许超卖的奖品库存同步到 Redis
     */
    @Override
    public void run(ApplicationArguments args) {
        List<AwardPO> awards = awardMapper.selectOversellAwards();
        for (AwardPO award : awards) {
            // 只有 Redis 中不存在时才初始化，避免重启覆盖已扣减的库存
            Integer existingStock = awardStockRedisService.getStock(award.getId());
            if (existingStock == null) {
                awardStockRedisService.initStock(award.getId(), award.getTotalStock());
                log.info("超卖奖品库存初始化，awardId={}，stock={}",
                        award.getId(), award.getTotalStock());
            } else {
                log.info("超卖奖品库存已存在，跳过初始化，awardId={}，currentStock={}",
                        award.getId(), existingStock);
            }
        }
    }
}