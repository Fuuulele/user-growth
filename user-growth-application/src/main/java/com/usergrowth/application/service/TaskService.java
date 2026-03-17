package com.usergrowth.application.service;

import com.usergrowth.api.dto.TaskVO;

import java.util.List;

public interface TaskService {

    /**
     * 获取任务列表（含当前用户完成状态）
     */
    List<TaskVO> queryTaskList(Long userId);

    /**
     * 完成任务上报，发放积分
     */
    boolean completeTask(Long userId, Integer taskId, String targetId);
}