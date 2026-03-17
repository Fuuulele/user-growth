package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("point_account")
public class PointAccountPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer balance;
    @Version
    private Integer version;
    private LocalDateTime updatedAt;
}