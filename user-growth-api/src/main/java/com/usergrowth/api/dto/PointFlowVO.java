package com.usergrowth.api.dto;

import lombok.Data;

@Data
public class PointFlowVO {
    private String taskName;
    private Integer pointsEarned;
    private String completedAt;
    private String taskType;
}
