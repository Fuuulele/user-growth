package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.CompleteTaskRequest;
import com.usergrowth.api.dto.TaskVO;
import com.usergrowth.application.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 获取任务列表
     * GET /api/task/list?userId=
     */
    @GetMapping("/api/task/list")
    public R<List<TaskVO>> getTaskList(
            @RequestParam
            @NotNull(message = "用户ID不能为空")
            @Min(value = 1, message = "用户ID必须大于0")
            Long userId) {
        return R.ok(taskService.queryTaskList(userId));
    }

    /**
     * 完成任务上报
     * POST /api/task/complete
     */
    @PostMapping("/api/task/complete")
    public R<Boolean> completeTask(@Valid @ModelAttribute CompleteTaskRequest request) {
        return R.ok(taskService.completeTask(
                request.getUserId(),
                request.getTaskId(),
                request.getTargetId()));
    }
}