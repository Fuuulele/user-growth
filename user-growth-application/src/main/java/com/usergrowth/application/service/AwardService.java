package com.usergrowth.application.service;

import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.api.dto.ExchangeResultVO;

import java.util.List;

public interface AwardService {

    List<AwardVO> getRewardList();
    boolean exchange(Long userId, Long awardId);
    boolean exchangeAsync(Long userId, Long awardId);

    /**
     * 查询兑换结果
     */
    ExchangeResultVO getExchangeResult(Long exchangeId);

}
