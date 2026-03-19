package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeRequest {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须大于0")
    private Long userId;

    @NotNull(message = "奖品ID不能为空")
    @Min(value = 1, message = "奖品ID必须大于0")
    private Long awardId;
}