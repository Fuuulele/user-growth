package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AwardUpdateRequest {

    @NotNull(message = "奖品ID不能为空")
    @Min(value = 1, message = "奖品ID必须大于0")
    private Long awardId;

    private String rewardName;
    private String rewardDesc;
    private String cover;

    @Min(value = 1, message = "兑换积分必须大于0")
    private Integer pointsCost;

    private String expireTime;

    private Integer status;
}