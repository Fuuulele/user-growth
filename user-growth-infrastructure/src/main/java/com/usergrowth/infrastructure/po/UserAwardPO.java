package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_award")
public class UserAwardPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long awardId;
    private Long exchangeId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
