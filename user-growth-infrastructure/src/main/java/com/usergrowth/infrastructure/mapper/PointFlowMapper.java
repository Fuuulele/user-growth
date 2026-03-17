package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.PointFlowPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointFlowMapper extends BaseMapper<PointFlowPO> {
}