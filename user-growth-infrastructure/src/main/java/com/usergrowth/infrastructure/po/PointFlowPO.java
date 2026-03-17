package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("point_flow")
public class PointFlowPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskName;
    private String taskType;
    private Integer pointsEarned;
    private LocalDateTime completedAt;
}