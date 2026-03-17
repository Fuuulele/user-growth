package com.usergrowth.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("award_inventory_split")
public class AwardInventorySplitPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long awardId;
    private Integer subStock;

    @Version
    private Integer version;
}
