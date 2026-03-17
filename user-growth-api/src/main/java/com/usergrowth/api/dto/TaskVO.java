package com.usergrowth.api.dto;

import lombok.Data;

@Data
public class TaskVO {
    private Integer taskID;
    private String taskName;
    private Integer pointsReward;
    private String taskType;
    private String taskStatus;
}
