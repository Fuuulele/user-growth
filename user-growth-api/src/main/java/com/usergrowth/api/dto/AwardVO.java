package com.usergrowth.api.dto;

import lombok.Data;

@Data
public class AwardVO {
    private Long id;
    private String rewardName;
    private String rewardDesc;
    private String cover;
    private Integer pointsCost;
    private Integer stock;
    private Integer totalStock;
}
