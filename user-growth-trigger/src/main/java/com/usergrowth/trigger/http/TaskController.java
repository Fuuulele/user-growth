package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.TaskVO;
import com.usergrowth.application.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 获取任务列表
     * GET /api/task/list
     * 暂时用 userId 请求参数模拟用户上下文，后续接入登录体系后替换
     */
    @GetMapping("/api/task/list")
    public R<List<TaskVO>> getTaskList(@RequestParam Long userId) {
        return R.ok(taskService.queryTaskList(userId));
    }
    //接口一：获取任务列表
    //GET http://localhost:8080/api/task/list?userId=1001

    /**
     * 完成任务上报
     * POST /api/task/complete
     */
    @PostMapping("/api/task/complete")
    public R<Boolean> completeTask(@RequestParam Long userId,
                                   @RequestParam Integer taskId,
                                   @RequestParam(required = false, defaultValue = "") String targetId) {
        return R.ok(taskService.completeTask(userId, taskId, targetId));
    }
    //接口二：完成任务上报
    //POST http://localhost:8080/api/task/complete?userId=1001&taskId=1
}