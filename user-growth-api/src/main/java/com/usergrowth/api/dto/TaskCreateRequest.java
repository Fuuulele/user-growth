package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "积分奖励不能为空")
    @Min(value = 1, message = "积分奖励必须大于0")
    private Integer pointsReward;

    @NotBlank(message = "任务类型不能为空")
    @Pattern(regexp = "DAILY|ONE_TIME|ACTIVITY", message = "任务类型必须是 DAILY / ONE_TIME / ACTIVITY")
    private String taskType;
}