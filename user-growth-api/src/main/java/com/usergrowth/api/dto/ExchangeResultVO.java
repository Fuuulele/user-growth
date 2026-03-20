package com.usergrowth.api.dto;

import lombok.Data;

@Data
public class ExchangeResultVO {

    /** 兑换记录ID */
    private String exchangeId;

    /** 兑换状态：PENDING=处理中 SUCCESS=成功 FAIL=失败 */
    private String status;

    /** 奖品名称 */
    private String awardName;

    /** 消耗积分 */
    private Integer pointsCost;

    /** 创建时间 */
    private String createdAt;
}