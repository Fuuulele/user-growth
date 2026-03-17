package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.application.service.AwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    /**
     * 获取奖品列表
     * GET /api/award/list
     */
    @GetMapping("/api/award/list")
    public R<List<AwardVO>> getRewardList() {
        return R.ok(awardService.getRewardList());
    }
    //接口五：获取奖品列表
    //GET http://localhost:8080/api/award/list

    /**
     * 积分兑换
     * POST /api/award/exchange
     */
    @PostMapping("/api/award/exchange")
    public R<Boolean> exchange(@RequestParam Long userId,
                               @RequestParam Long awardId) {
        return R.ok(awardService.exchange(userId, awardId));
    }
    //接口六：积分兑换
    //POST http://localhost:8080/api/award/exchange?userId=1001&awardId=1
}