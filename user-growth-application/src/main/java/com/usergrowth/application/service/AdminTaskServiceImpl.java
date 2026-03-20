package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.usergrowth.api.dto.TaskCreateRequest;
import com.usergrowth.api.dto.TaskUpdateRequest;
import com.usergrowth.api.dto.TaskVO;
import com.usergrowth.infrastructure.mapper.TaskMapper;
import com.usergrowth.infrastructure.po.TaskPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTaskServiceImpl implements AdminTaskService {

    private final TaskMapper taskMapper;

    @Override
    public Integer createTask(TaskCreateRequest request) {
        TaskPO task = new TaskPO();
        task.setTaskName(request.getTaskName());
        task.setPointsReward(request.getPointsReward());
        task.setTaskType(request.getTaskType());
        task.setStatus(1);
        taskMapper.insert(task);
        log.info("创建任务成功，taskId={}, taskName={}", task.getId(), task.getTaskName());
        return task.getId();
    }

    @Override
    public boolean updateTask(TaskUpdateRequest request) {
        TaskPO task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在，taskId=" + request.getTaskId());
        }
        if (request.getTaskName() != null) task.setTaskName(request.getTaskName());
        if (request.getPointsReward() != null) task.setPointsReward(request.getPointsReward());
        if (request.getTaskType() != null) task.setTaskType(request.getTaskType());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        taskMapper.updateById(task);
        log.info("更新任务成功，taskId={}", request.getTaskId());
        return true;
    }

    @Override
    public boolean deleteTask(Integer taskId) {
        TaskPO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在，taskId=" + taskId);
        }
        task.setStatus(0);
        taskMapper.updateById(task);
        log.info("删除任务成功（逻辑删除），taskId={}", taskId);
        return true;
    }

    @Override
    public List<TaskVO> listAllTasks() {
        List<TaskPO> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TaskPO>().orderByAsc(TaskPO::getId)
        );
        return tasks.stream().map(task -> {
            TaskVO vo = new TaskVO();
            vo.setTaskID(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setPointsReward(task.getPointsReward());
            vo.setTaskType(task.getTaskType());
            vo.setTaskStatus(task.getStatus() == 1 ? "ENABLED" : "DISABLED");
            return vo;
        }).collect(Collectors.toList());
    }
}