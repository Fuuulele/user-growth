package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("award")
public class AwardPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String rewardName;
    private String rewardDesc;
    private String cover;
    private Integer pointsCost;
    private Integer totalStock;
    private Integer allowOversell;
    private Integer status;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
