package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exchange_record")
public class ExchangeRecordPO {

    @TableId
    private Long id;
    private Long userId;
    private Long awardId;
    private String idempotentId;
    private Integer status;
    private Integer pointsCost;

    @TableField("create_at")
    private LocalDateTime createdAt;
    @TableField("update_at")
    private LocalDateTime updatedAt;
}
