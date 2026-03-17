package com.usergrowth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usergrowth.infrastructure.po.UserAwardPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAwardMapper extends BaseMapper<UserAwardPO> {
}
