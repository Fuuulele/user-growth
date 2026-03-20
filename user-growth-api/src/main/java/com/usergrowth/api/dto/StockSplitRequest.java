package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockSplitRequest {

    @NotNull(message = "奖品ID不能为空")
    @Min(value = 1, message = "奖品ID必须大于0")
    private Long awardId;

    @NotNull(message = "拆分份数不能为空")
    @Min(value = 1, message = "拆分份数必须大于0")
    private Integer splitCount;
}