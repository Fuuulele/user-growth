package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TaskUpdateRequest {

    @NotNull(message = "任务ID不能为空")
    @Min(value = 1, message = "任务ID必须大于0")
    private Integer taskId;

    private String taskName;

    @Min(value = 1, message = "积分奖励必须大于0")
    private Integer pointsReward;

    @Pattern(regexp = "DAILY|ONE_TIME|ACTIVITY", message = "任务类型必须是 DAILY / ONE_TIME / ACTIVITY")
    private String taskType;

    @Min(value = 0, message = "状态值不合法")
    private Integer status;
}