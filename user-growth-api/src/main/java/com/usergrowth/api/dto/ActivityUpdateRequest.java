package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivityUpdateRequest {

    @NotNull(message = "活动ID不能为空")
    @Min(value = 1, message = "活动ID必须大于0")
    private Long activityId;

    private String name;
    private String description;
    private String startTime;
    private String endTime;
    private Integer status;
}