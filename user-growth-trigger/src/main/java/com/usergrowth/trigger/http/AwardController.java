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
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

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
    @SentinelResource(value = "exchange",
            blockHandler = "exchangeBlockHandler",
            fallback = "exchangeFallback")
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
    @SentinelResource(value = "exchangeAsync",
            blockHandler = "exchangeBlockHandler",
            fallback = "exchangeFallback")
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

    /**
     * 限流处理：触发 QPS 限制时调用
     * 参数列表必须与原方法一致，最后加 BlockException
     */
    public R<Boolean> exchangeBlockHandler(ExchangeRequest request, BlockException e) {
        return R.fail(429, "活动太火爆了，请稍后再试～");
    }

    /**
     * 降级处理：业务异常触发熔断时调用
     * 参数列表必须与原方法一致，最后加 Throwable
     */
    public R<Boolean> exchangeFallback(ExchangeRequest request, Throwable t) {
        // BlockException 由 blockHandler 处理，这里只处理业务异常
        if (t instanceof com.alibaba.csp.sentinel.slots.block.BlockException) {
            return R.fail(429, "活动太火爆了，请稍后再试～");
        }
        log.error("兑换服务降级，userId={}, awardId={}, 异常={}",
                request.getUserId(), request.getAwardId(), t.getMessage());
        return R.fail(503, "兑换服务暂时不可用，请稍后再试～");
    }
}