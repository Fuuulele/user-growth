package com.usergrowth.application.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.usergrowth.api.common.TaskStatusEnum;
import com.usergrowth.api.dto.TaskVO;
import com.usergrowth.infrastructure.mapper.PointAccountMapper;
import com.usergrowth.infrastructure.mapper.PointFlowMapper;
import com.usergrowth.infrastructure.mapper.TaskMapper;
import com.usergrowth.infrastructure.mapper.TaskRecordMapper;
import com.usergrowth.infrastructure.po.PointAccountPO;
import com.usergrowth.infrastructure.po.PointFlowPO;
import com.usergrowth.infrastructure.po.TaskPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PointFlowMapper pointFlowMapper;

    @Override
    public List<TaskVO> queryTaskList(Long userId) {
        // 1. 查询所有启用的任务
        List<TaskPO> taskList = taskMapper.selectList(
                new LambdaQueryWrapper<TaskPO>()
                        .eq(TaskPO::getStatus, 1)
                        .orderByAsc(TaskPO::getId)
        );

        // 2. 查询用户今天已完成的任务ID集合
        Set<Integer> completedIds = taskRecordMapper
                .selectCompletedTaskIds(userId, LocalDate.now())
                .stream()
                .collect(Collectors.toSet());

        // 3. 组装 VO，合并完成状态
        return taskList.stream().map(task -> {
            TaskVO vo = new TaskVO();
            vo.setTaskID(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setPointsReward(task.getPointsReward());
            vo.setTaskType(task.getTaskType());
            // 每日任务：今天已完成则标记 COMPLETED
            // 一次性任务：完成过任何一天都算 COMPLETED
            if (completedIds.contains(task.getId())) {
                vo.setTaskStatus(TaskStatusEnum.COMPLETED.getCode());
            } else {
                vo.setTaskStatus(TaskStatusEnum.AVAILABLE.getCode());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeTask(Long userId, Integer taskId, String targetId) {
        // 1. 查询任务是否存在且启用
        TaskPO task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 1) {
            throw new RuntimeException("任务不存在或已禁用");
        }

        // 2. 构建幂等 Key：MD5(userId_taskId_bizDate)
        String bizDate = LocalDate.now().toString();
        String idempotentKey = DigestUtil.md5Hex(userId + "_" + taskId + "_" + bizDate);

        // 3. 幂等插入任务记录，INSERT IGNORE，重复返回 0
        int rows = taskRecordMapper.insertIgnore(
                userId, taskId,
                targetId == null ? "" : targetId,
                LocalDate.now(),
                task.getPointsReward(),
                idempotentKey
        );

        // 4. rows == 0 说明已经完成过，直接返回 true（幂等）
        if (rows == 0) {
            log.info("任务已完成，幂等返回，userId={}, taskId={}", userId, taskId);
            return true;
        }

        // 5. 发放积分：乐观锁重试，最多3次
        boolean success = addPointsWithRetry(userId, task.getPointsReward(), 3);
        if (!success) {
            throw new RuntimeException("积分发放失败，请重试");
        }

        // 6. 写入积分流水
        PointFlowPO flow = new PointFlowPO();
        flow.setUserId(userId);
        flow.setTaskName(task.getTaskName());
        flow.setTaskType(task.getTaskType());
        flow.setPointsEarned(task.getPointsReward());
        flow.setCompletedAt(LocalDateTime.now());
        pointFlowMapper.insert(flow);

        log.info("任务完成，积分发放成功，userId={}, taskId={}, points={}",
                userId, taskId, task.getPointsReward());
        return true;
    }

    /**
     * 乐观锁重试发放积分
     * 若账户不存在则自动初始化
     */
    private boolean addPointsWithRetry(Long userId, Integer points, int maxRetry) {
        for (int i = 0; i < maxRetry; i++) {
            PointAccountPO account = pointAccountMapper.selectByUserId(userId);
            if (account == null) {
                // 账户不存在，初始化账户
                PointAccountPO newAccount = new PointAccountPO();
                newAccount.setUserId(userId);
                newAccount.setBalance(points);
                newAccount.setVersion(0);
                pointAccountMapper.insert(newAccount);
                return true;
            }
            // 乐观锁更新
            int affected = pointAccountMapper.addPoints(userId, points, account.getVersion());
            if (affected > 0) {
                return true;
            }
            log.warn("乐观锁冲突，第{}次重试，userId={}", i + 1, userId);
        }
        return false;
    }
}