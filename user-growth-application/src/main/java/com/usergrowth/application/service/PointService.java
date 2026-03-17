package com.usergrowth.application.service;

import com.usergrowth.api.dto.PageResult;
import com.usergrowth.api.dto.PointFlowVO;

public interface PointService {

    /**
     * 查询积分余额
     */
    Integer getBalance(Long userId);

    /**
     * 查询积分流水（分页）
     */
    PageResult<PointFlowVO> getPointHistory(Long userId, Integer page, Integer size);
}
