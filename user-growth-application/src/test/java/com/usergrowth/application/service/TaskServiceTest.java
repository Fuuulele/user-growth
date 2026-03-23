package com.usergrowth.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.usergrowth.infrastructure.mapper.PointAccountMapper;
import com.usergrowth.infrastructure.mapper.PointFlowMapper;
import com.usergrowth.infrastructure.mapper.TaskMapper;
import com.usergrowth.infrastructure.mapper.TaskRecordMapper;
import com.usergrowth.infrastructure.po.PointAccountPO;
import com.usergrowth.infrastructure.po.PointFlowPO;
import com.usergrowth.infrastructure.po.TaskPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("任务服务单元测试")
class TaskServiceTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskRecordMapper taskRecordMapper;
    @Mock private PointAccountMapper pointAccountMapper;
    @Mock private PointFlowMapper pointFlowMapper;
    @Mock private Cache<String, Object> taskListCache;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TaskServiceImpl taskService;

    private TaskPO dailyTask;
    private TaskPO oneTimeTask;

    @BeforeEach
    void setUp() {
        dailyTask = new TaskPO();
        dailyTask.setId(1);
        dailyTask.setTaskName("每日签到");
        dailyTask.setPointsReward(10);
        dailyTask.setTaskType("DAILY");
        dailyTask.setStatus(1);

        oneTimeTask = new TaskPO();
        oneTimeTask.setId(5);
        oneTimeTask.setTaskName("完善个人简历");
        oneTimeTask.setPointsReward(100);
        oneTimeTask.setTaskType("ONE_TIME");
        oneTimeTask.setStatus(1);
    }

    // ==================== completeTask 测试 ====================

    @Test
    @DisplayName("正常完成每日任务，积分发放成功")
    void completeTask_daily_success() {
        // 准备数据
        when(taskMapper.selectById(1)).thenReturn(dailyTask);
        when(taskRecordMapper.insertIgnore(anyLong(), anyInt(), anyString(),
                any(), anyInt(), anyString())).thenReturn(1);

        PointAccountPO account = new PointAccountPO();
        account.setUserId(1001L);
        account.setBalance(100);
        account.setVersion(1);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(account);
        when(pointAccountMapper.addPoints(1001L, 10, 1)).thenReturn(1);
        when(pointFlowMapper.insert(any(PointFlowPO.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // 执行
        boolean result = taskService.completeTask(1001L, 1, "");

        // 验证
        assertTrue(result);
        verify(taskRecordMapper, times(1))
                .insertIgnore(anyLong(), anyInt(), anyString(), any(), anyInt(), anyString());
        verify(pointAccountMapper, times(1)).addPoints(1001L, 10, 1);
        verify(pointFlowMapper, times(1)).insert(any(PointFlowPO.class));
    }

    @Test
    @DisplayName("幂等测试：相同参数重复调用，INSERT IGNORE 返回0，直接返回成功不重复发积分")
    void completeTask_idempotent_returnTrue() {
        when(taskMapper.selectById(1)).thenReturn(dailyTask);
        // INSERT IGNORE 返回 0 表示已存在
        when(taskRecordMapper.insertIgnore(anyLong(), anyInt(), anyString(),
                any(), anyInt(), anyString())).thenReturn(0);

        boolean result = taskService.completeTask(1001L, 1, "");

        assertTrue(result);
        // 验证积分发放方法没有被调用
        verify(pointAccountMapper, never()).addPoints(anyLong(), anyInt(), anyInt());
        verify(pointFlowMapper, never()).insert(any(PointFlowPO.class));
    }

    @Test
    @DisplayName("任务不存在时抛出异常")
    void completeTask_taskNotFound_throwException() {
        when(taskMapper.selectById(99)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.completeTask(1001L, 99, ""));
        assertEquals("任务不存在或已禁用", ex.getMessage());
    }

    @Test
    @DisplayName("任务已禁用时抛出异常")
    void completeTask_taskDisabled_throwException() {
        dailyTask.setStatus(0);
        when(taskMapper.selectById(1)).thenReturn(dailyTask);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.completeTask(1001L, 1, ""));
        assertEquals("任务不存在或已禁用", ex.getMessage());
    }

    @Test
    @DisplayName("一次性任务已完成，拒绝重复完成")
    void completeTask_oneTime_alreadyCompleted_returnTrue() {
        when(taskMapper.selectById(5)).thenReturn(oneTimeTask);
        // 模拟历史已完成
        when(taskRecordMapper.selectAllCompletedTaskIds(1001L))
                .thenReturn(List.of(5));

        boolean result = taskService.completeTask(1001L, 5, "");

        assertTrue(result);
        // 验证没有走到 insertIgnore
        verify(taskRecordMapper, never()).insertIgnore(
                anyLong(), anyInt(), anyString(), any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("积分账户不存在时自动初始化")
    void completeTask_accountNotExist_autoCreate() {
        when(taskMapper.selectById(1)).thenReturn(dailyTask);
        when(taskRecordMapper.insertIgnore(anyLong(), anyInt(), anyString(),
                any(), anyInt(), anyString())).thenReturn(1);
        // 账户不存在
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(null);
        when(pointAccountMapper.insert(any(PointAccountPO.class))).thenReturn(1);
        when(pointFlowMapper.insert(any(PointFlowPO.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = taskService.completeTask(1001L, 1, "");

        assertTrue(result);
        // 验证调用了 insert 初始化账户
        verify(pointAccountMapper, times(1)).insert(any(PointAccountPO.class));
    }

    @Test
    @DisplayName("乐观锁冲突重试3次失败后抛出异常")
    void completeTask_optimisticLock_maxRetry_throwException() {
        when(taskMapper.selectById(1)).thenReturn(dailyTask);
        when(taskRecordMapper.insertIgnore(anyLong(), anyInt(), anyString(),
                any(), anyInt(), anyString())).thenReturn(1);

        PointAccountPO account = new PointAccountPO();
        account.setUserId(1001L);
        account.setBalance(100);
        account.setVersion(1);
        when(pointAccountMapper.selectByUserId(1001L)).thenReturn(account);
        // 每次乐观锁更新都失败（模拟并发冲突）
        when(pointAccountMapper.addPoints(anyLong(), anyInt(), anyInt())).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.completeTask(1001L, 1, ""));
        assertEquals("积分发放失败，请重试", ex.getMessage());
        // 验证重试了3次
        verify(pointAccountMapper, times(3))
                .addPoints(anyLong(), anyInt(), anyInt());
    }
}