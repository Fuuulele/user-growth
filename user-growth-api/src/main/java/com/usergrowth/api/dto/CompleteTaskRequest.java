package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompleteTaskRequest {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须大于0")
    private Long userId;

    @NotNull(message = "任务ID不能为空")
    @Min(value = 1, message = "任务ID必须大于0")
    private Integer taskId;

    private String targetId = "";
}