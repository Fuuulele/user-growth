package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task_record")
public class TaskRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer taskId;
    private String targetId;
    private LocalDate bizDate;
    private Integer pointsEarned;
    private String idempotentKey;
    private LocalDateTime createdAt;
}
