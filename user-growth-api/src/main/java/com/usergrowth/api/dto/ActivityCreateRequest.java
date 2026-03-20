package com.usergrowth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivityCreateRequest {

    @NotBlank(message = "活动名称不能为空")
    private String name;

    private String description = "";

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;
}