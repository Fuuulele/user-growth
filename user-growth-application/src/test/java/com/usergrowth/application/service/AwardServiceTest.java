package com.usergrowth.application.service;

import com.usergrowth.infrastructure.mapper.*;
import com.usergrowth.infrastructure.mq.ExchangeTransactionProducer;
import com.usergrowth.infrastructure.po.AwardPO;
import com.usergrowth.infrastructure.po.AwardInventorySplitPO;
import com.usergrowth.infrastructure.po.PointAccountPO;
import com.usergrowth.infrastructure.redis.AwardStockRedisService;
import com.usergrowth.infrastructure.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import com.usergrowth.infrastructure.po.ExchangeRecordPO;
import com.usergrowth.infrastructure.po.UserAwardPO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("兑换服务单元测试")
class AwardServiceTest {

    @Mock private AwardMapper awardMapper;
    @Mock private AwardInventorySplitMapper inventorySplitMapper;
    @Mock private ExchangeRecordMapper exchangeRecordMapper;
    @Mock private UserAwardMapper userAwardMapper;
    @Mock private PointAccountMapper pointAccountMapper;
    @Mock private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private AwardStockRedisService awardStockRedisService;
    @Mock private ExchangeTransactionProducer transactionProducer;

    @InjectMocks
    private AwardServiceImpl awardService;

    private AwardPO normalAward;      // 不允许超卖
    private AwardPO oversellAward;    // 允许超卖
    private PointAccountPO richAccount; // 积分充足的账户

    @BeforeEach
    void setUp() {
        normalAward = new AwardPO();
        normalAward.setId(1L);
        normalAward.setRewardName("7天会员");
        normalAward.setPointsCost(50);
        normalAward.setAllowOversell(0);
        normalAward.setStatus(1);
        normalAward.setExpireTime(LocalDateTime.of(2099, 12, 31, 23, 59, 59));

        oversellAward = new AwardPO();
        oversellAward.setId(4L);
        oversellAward.setRewardName("30天会员");
        oversellAward.setPointsCost(30);
        oversellAward.setAllowOversell(1);
        oversellAward.setStatus(1);
        oversellAward.setExpireTime(LocalDateTime.of(2099, 12, 31, 23, 59, 59));

        richAccount = new PointAccountPO();
        richAccount.setUserId(1001L);
        richAccount.setBalance(10000);
        richAccount.setVersion(1);
    }

    // ==================== exchange 同步兑换测试 ====================

    @Test
    @DisplayName("不允许超卖场景：库存充足，兑换成功")
    void exchange_noOversell_success() {
        when(awardMapper.selectById(1L)).thenReturn(normalAward);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(richAccount);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        when(exchangeRecordMapper.insert(any(ExchangeRecordPO.class))).thenReturn(1);

        // 模拟库存拆分有货
        AwardInventorySplitPO split = new AwardInventorySplitPO();
        split.setId(1L);
        split.setAwardId(1L);
        split.setSubStock(10);
        split.setVersion(0);
        when(inventorySplitMapper.selectAvailableByAwardId(1L))
                .thenReturn(List.of(split));
        when(inventorySplitMapper.deductStock(1L, 0)).thenReturn(1);
        when(pointAccountMapper.addPoints(1001L, -50, 1)).thenReturn(1);
        when(userAwardMapper.insert(any(UserAwardPO.class))).thenReturn(1);
        when(exchangeRecordMapper.updateById(any(ExchangeRecordPO.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = awardService.exchange(1001L, 1L);

        assertTrue(result);
        verify(inventorySplitMapper, times(1)).deductStock(1L, 0);
        verify(pointAccountMapper, times(1)).addPoints(1001L, -50, 1);
        verify(userAwardMapper, times(1)).insert(any(UserAwardPO.class));
    }

    @Test
    @DisplayName("允许超卖场景：Redis DECR 成功，兑换成功")
    void exchange_oversell_redisSuccess() {
        when(awardMapper.selectById(4L)).thenReturn(oversellAward);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(richAccount);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        when(exchangeRecordMapper.insert(any(ExchangeRecordPO.class))).thenReturn(1);
        when(awardStockRedisService.decrStock(4L)).thenReturn(999L);
        when(pointAccountMapper.addPoints(1001L, -30, 1)).thenReturn(1);
        when(userAwardMapper.insert(any(UserAwardPO.class))).thenReturn(1);
        when(exchangeRecordMapper.updateById(any(ExchangeRecordPO.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = awardService.exchange(1001L, 4L);

        assertTrue(result);
        verify(awardStockRedisService, times(1)).decrStock(4L);
        verify(awardStockRedisService, never()).incrStock(anyLong());
    }

    @Test
    @DisplayName("允许超卖场景：Redis 库存不足，回滚并抛出异常")
    void exchange_oversell_redisStockOut_throwException() {
        when(awardMapper.selectById(4L)).thenReturn(oversellAward);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(richAccount);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        when(exchangeRecordMapper.insert(any(ExchangeRecordPO.class))).thenReturn(1);
        // Redis 库存不足，返回 -1
        when(awardStockRedisService.decrStock(4L)).thenReturn(-1L);
        when(exchangeRecordMapper.updateById(any(ExchangeRecordPO.class))).thenReturn(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> awardService.exchange(1001L, 4L));
        assertEquals("库存不足，兑换失败", ex.getMessage());
        // 验证回滚了 Redis 库存
        verify(awardStockRedisService, times(1)).incrStock(4L);
    }

    @Test
    @DisplayName("积分不足时抛出异常")
    void exchange_insufficientPoints_throwException() {
        when(awardMapper.selectById(1L)).thenReturn(normalAward);

        PointAccountPO poorAccount = new PointAccountPO();
        poorAccount.setUserId(1001L);
        poorAccount.setBalance(10); // 只有 10 积分，需要 50
        poorAccount.setVersion(1);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(poorAccount);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> awardService.exchange(1001L, 1L));
        assertEquals("积分不足", ex.getMessage());
    }

    @Test
    @DisplayName("奖品不存在时抛出异常")
    void exchange_awardNotFound_throwException() {
        when(awardMapper.selectById(99L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> awardService.exchange(1001L, 99L));
        assertEquals("奖品不存在或已下架", ex.getMessage());
    }

    @Test
    @DisplayName("活动已过期时抛出异常")
    void exchange_expired_throwException() {
        normalAward.setExpireTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        when(awardMapper.selectById(1L)).thenReturn(normalAward);
        //when(pointAccountMapper.selectByUserId(1001L)).thenReturn(richAccount);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> awardService.exchange(1001L, 1L));
        assertEquals("活动已结束", ex.getMessage());
    }

    @Test
    @DisplayName("不允许超卖场景：库存不足抛出异常")
    void exchange_noOversell_stockOut_throwException() {
        when(awardMapper.selectById(1L)).thenReturn(normalAward);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(richAccount);
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        when(exchangeRecordMapper.insert(any(ExchangeRecordPO.class))).thenReturn(1);
        // 没有可用库存
        when(inventorySplitMapper.selectAvailableByAwardId(1L))
                .thenReturn(List.of());
        when(exchangeRecordMapper.updateById(any(ExchangeRecordPO.class))).thenReturn(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> awardService.exchange(1001L, 1L));
        assertEquals("库存不足，兑换失败", ex.getMessage());
    }
}