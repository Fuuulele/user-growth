package com.usergrowth.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExchangeMessage implements Serializable {

    /** 兑换记录ID（雪花算法，作为全局幂等ID） */
    private Long exchangeId;

    /** 用户ID */
    private Long userId;

    /** 奖品ID */
    private Long awardId;

    /** 消耗积分 */
    private Integer pointsCost;

    /** 是否允许超卖 */
    private Integer allowOversell;
}