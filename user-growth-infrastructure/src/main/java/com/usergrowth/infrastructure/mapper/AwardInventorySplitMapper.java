package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.AwardInventorySplitPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AwardInventorySplitMapper extends BaseMapper<AwardInventorySplitPO> {

    @Select("SELECT * FROM award_inventory_split WHERE award_id = #{awardId} AND sub_stock > 0")
    List<AwardInventorySplitPO> selectAvailableByAwardId(@Param("awardId") Long awardId);

    // 乐观锁扣减子库存
    @Update("UPDATE award_inventory_split SET sub_stock = sub_stock - 1, version = version + 1 " +
            "WHERE id = #{id} AND sub_stock > 0 AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("version") Integer version);
}
