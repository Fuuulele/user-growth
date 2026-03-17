package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AwardMapper extends BaseMapper<AwardPO> {
}
