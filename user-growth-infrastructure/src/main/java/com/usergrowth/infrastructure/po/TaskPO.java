package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class TaskPO {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String taskName;
    private Integer pointsReward;
    private String taskType;
    private Integer status;
    private LocalDateTime createdAt;
}
