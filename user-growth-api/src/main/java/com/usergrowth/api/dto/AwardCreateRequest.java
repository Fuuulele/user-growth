package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AwardCreateRequest {

    @NotBlank(message = "奖品名称不能为空")
    private String rewardName;

    private String rewardDesc = "";

    private String cover = "";

    @NotNull(message = "兑换积分不能为空")
    @Min(value = 1, message = "兑换积分必须大于0")
    private Integer pointsCost;

    @NotNull(message = "总库存不能为空")
    @Min(value = 1, message = "总库存必须大于0")
    private Integer totalStock;

    @NotNull(message = "是否允许超卖不能为空")
    private Integer allowOversell;

    /** 库存拆分份数，不允许超卖时必填 */
    private Integer splitCount;

    /** 活动截止时间，格式：yyyy-MM-dd HH:mm:ss */
    private String expireTime;
}