package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AwardMapper extends BaseMapper<AwardPO> {

    @Select("SELECT * FROM award WHERE allow_oversell = 1 AND status = 1")
    List<AwardPO> selectOversellAwards();

    @Select("SELECT er.id, er.status, er.points_cost, er.created_at, a.reward_name " +
            "FROM exchange_record er " +
            "LEFT JOIN award a ON er.award_id = a.id " +
            "WHERE er.id = #{exchangeId}")
    Map<String, Object> selectExchangeResult(@Param("exchangeId") Long exchangeId);

}
