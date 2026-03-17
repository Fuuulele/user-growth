package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.TaskRecordPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskRecordMapper extends BaseMapper<TaskRecordPO> {

    /**
     * 幂等插入，重复则忽略
     */
    @Insert("INSERT IGNORE INTO task_record " +
            "(user_id, task_id, target_id, biz_date, points_earned, idempotent_key) " +
            "VALUES (#{userId}, #{taskId}, #{targetId}, #{bizDate}, #{pointsEarned}, #{idempotentKey})")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("taskId") Integer taskId,
                     @Param("targetId") String targetId,
                     @Param("bizDate") LocalDate bizDate,
                     @Param("pointsEarned") Integer pointsEarned,
                     @Param("idempotentKey") String idempotentKey);

    /**
     * 查询用户当天已完成的任务ID列表
     */
    @Select("SELECT task_id FROM task_record " +
            "WHERE user_id = #{userId} AND biz_date = #{bizDate}")
    List<Integer> selectCompletedTaskIds(@Param("userId") Long userId,
                                         @Param("bizDate") LocalDate bizDate);
}