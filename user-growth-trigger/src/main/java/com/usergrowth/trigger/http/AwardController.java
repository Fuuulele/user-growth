package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.api.dto.ExchangeRequest;
import com.usergrowth.api.dto.ExchangeResultVO;
import com.usergrowth.application.service.AwardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
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

    /**
     * 查询兑换结果（用于异步兑换后轮询）
     * GET /api/award/exchange/result
     */
    @GetMapping("/api/award/exchange/result")
    public R<ExchangeResultVO> getExchangeResult(
            @RequestParam
            @NotNull(message = "兑换ID不能为空")
            Long exchangeId) {
        return R.ok(awardService.getExchangeResult(exchangeId));
    }
}