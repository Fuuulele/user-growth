package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.PointAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointAccountMapper extends BaseMapper<PointAccountPO> {

    @Select("SELECT * FROM point_account WHERE user_id = #{userId}")
    PointAccountPO selectByUserId(@Param("userId") Long userId);

    /**
     * 乐观锁更新积分余额
     */
    @Update("UPDATE point_account SET balance = balance + #{points}, version = version + 1 " +
            "WHERE user_id = #{userId} AND version = #{version}")
    int addPoints(@Param("userId") Long userId,
                  @Param("points") Integer points,
                  @Param("version") Integer version);
}