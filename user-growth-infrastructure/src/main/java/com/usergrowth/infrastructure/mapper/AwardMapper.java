package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AwardMapper extends BaseMapper<AwardPO> {

    @Select("SELECT * FROM award WHERE allow_oversell = 1 AND status = 1")
    List<AwardPO> selectOversellAwards();

}
