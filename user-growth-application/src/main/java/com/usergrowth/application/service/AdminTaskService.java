package com.usergrowth.application.service;

import com.usergrowth.api.dto.TaskCreateRequest;
import com.usergrowth.api.dto.TaskUpdateRequest;
import com.usergrowth.api.dto.TaskVO;

import java.util.List;

public interface AdminTaskService {

    /** 创建任务 */
    Integer createTask(TaskCreateRequest request);

    /** 更新任务 */
    boolean updateTask(TaskUpdateRequest request);

    /** 删除任务（逻辑删除，status=0） */
    boolean deleteTask(Integer taskId);

    /** 查询任务列表（含禁用） */
    List<TaskVO> listAllTasks();
}