package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.api.dto.ExchangeRequest;
import com.usergrowth.application.service.AwardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * 积分兑换
     * POST /api/award/exchange
     */
    @PostMapping("/api/award/exchange")
    public R<Boolean> exchange(@Valid @ModelAttribute ExchangeRequest request) {
        return R.ok(awardService.exchange(
                request.getUserId(),
                request.getAwardId()));
    }

    /**
     * 积分兑换（异步MQ版）
     * POST /api/award/exchange/async
     */
    @PostMapping("/api/award/exchange/async")
    public R<Boolean> exchangeAsync(@Valid @ModelAttribute ExchangeRequest request) {
        return R.ok(awardService.exchangeAsync(
                request.getUserId(),
                request.getAwardId()));
    }
}