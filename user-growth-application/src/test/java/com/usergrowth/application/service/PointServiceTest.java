package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usergrowth.api.dto.PageResult;
import com.usergrowth.api.dto.PointFlowVO;
import com.usergrowth.infrastructure.mapper.PointAccountMapper;
import com.usergrowth.infrastructure.mapper.PointFlowMapper;
import com.usergrowth.infrastructure.po.PointAccountPO;
import com.usergrowth.infrastructure.po.PointFlowPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("积分服务单元测试")
class PointServiceTest {

    @Mock private PointAccountMapper pointAccountMapper;
    @Mock private PointFlowMapper pointFlowMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PointServiceImpl pointService;

    // ==================== getBalance 测试 ====================

    @Test
    @DisplayName("Redis 缓存命中，直接返回缓存余额")
    void getBalance_cacheHit_returnCachedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("point:balance:1001")).thenReturn(200);

        Integer balance = pointService.getBalance(1001L);

        assertEquals(200, balance);
        // 验证没有查数据库
        verify(pointAccountMapper, never()).selectByUserId(anyLong());
    }

    @Test
    @DisplayName("Redis 缓存未命中，查数据库并写入缓存")
    void getBalance_cacheMiss_queryDbAndCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("point:balance:1001")).thenReturn(null);

        PointAccountPO account = new PointAccountPO();
        account.setUserId(1001L);
        account.setBalance(150);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(account);

        Integer balance = pointService.getBalance(1001L);

        assertEquals(150, balance);
        // 验证写入了缓存
        verify(valueOperations, times(1))
                .set(eq("point:balance:1001"), eq(150), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("账户不存在时返回余额 0")
    void getBalance_accountNotExist_returnZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(null);

        Integer balance = pointService.getBalance(1001L);

        assertEquals(0, balance);
        verify(valueOperations, times(1))
                .set(eq("point:balance:1001"), eq(0), eq(30L), eq(TimeUnit.MINUTES));
    }

    // ==================== getPointHistory 测试 ====================

    @Test
    @DisplayName("分页查询积分流水，返回正确分页结果")
    void getPointHistory_returnPageResult() {
        PointFlowPO flow = new PointFlowPO();
        flow.setTaskName("每日签到");
        flow.setPointsEarned(10);
        flow.setTaskType("DAILY");
        flow.setCompletedAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0));

        IPage<PointFlowPO> page = new Page<>(1, 10);
        page.setRecords(List.of(flow));
        page.setTotal(1L);

        when(pointFlowMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<PointFlowVO> result = pointService.getPointHistory(1001L, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getTotalPages());
        assertEquals("每日签到", result.getList().get(0).getTaskName());
        assertEquals(10, result.getList().get(0).getPointsEarned());
    }

    @Test
    @DisplayName("无流水记录时返回空列表")
    void getPointHistory_emptyResult() {
        IPage<PointFlowPO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0L);

        when(pointFlowMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<PointFlowVO> result = pointService.getPointHistory(1001L, 1, 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getTotalPages());
    }
}