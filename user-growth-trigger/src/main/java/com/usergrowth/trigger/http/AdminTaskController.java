package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.TaskCreateRequest;
import com.usergrowth.api.dto.TaskUpdateRequest;
import com.usergrowth.api.dto.TaskVO;
import com.usergrowth.application.service.AdminTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    /** 创建任务 POST /api/b/task/create */
    @PostMapping("/api/b/task/create")
    public R<Integer> createTask(@Valid @ModelAttribute TaskCreateRequest request) {
        return R.ok(adminTaskService.createTask(request));
    }

    /** 更新任务 PUT /api/b/task/update */
    @PutMapping("/api/b/task/update")
    public R<Boolean> updateTask(@Valid @ModelAttribute TaskUpdateRequest request) {
        return R.ok(adminTaskService.updateTask(request));
    }

    /** 删除任务 DELETE /api/b/task/delete */
    @DeleteMapping("/api/b/task/delete")
    public R<Boolean> deleteTask(
            @RequestParam
            @NotNull(message = "任务ID不能为空")
            @Min(value = 1, message = "任务ID必须大于0")
            Integer taskId) {
        return R.ok(adminTaskService.deleteTask(taskId));
    }

    /** 查询所有任务 GET /api/b/task/list */
    @GetMapping("/api/b/task/list")
    public R<List<TaskVO>> listAllTasks() {
        return R.ok(adminTaskService.listAllTasks());
    }
}