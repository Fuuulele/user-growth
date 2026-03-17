package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.ExchangeRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExchangeRecordMapper extends BaseMapper<ExchangeRecordPO> {
}
