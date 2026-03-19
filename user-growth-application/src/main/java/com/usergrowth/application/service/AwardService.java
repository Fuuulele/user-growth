package com.usergrowth.application.service;

import com.usergrowth.api.dto.AwardVO;
import java.util.List;

public interface AwardService {

    List<AwardVO> getRewardList();
    boolean exchange(Long userId, Long awardId);
    boolean exchangeAsync(Long userId, Long awardId);

}
