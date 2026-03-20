package com.usergrowth.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockUpdateRequest {

    @NotNull(message = "奖品ID不能为空")
    @Min(value = 1, message = "奖品ID必须大于0")
    private Long awardId;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 1, message = "库存数量必须大于0")
    private Integer totalStock;

    /** 重新拆分的份数 */
    @Min(value = 1, message = "拆分份数必须大于0")
    private Integer splitCount;
}