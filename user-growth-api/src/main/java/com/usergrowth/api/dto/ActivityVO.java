package com.usergrowth.api.dto;

import lombok.Data;

@Data
public class ActivityVO {
    private Long id;
    private String name;
    private String description;
    private String startTime;
    private String endTime;
    private String status;
}