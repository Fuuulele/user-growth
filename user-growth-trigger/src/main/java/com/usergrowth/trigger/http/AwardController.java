package com.usergrowth.trigger.http;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.AwardVO;
import com.usergrowth.api.dto.ExchangeRequest;
import com.usergrowth.api.dto.ExchangeResultVO;
import com.usergrowth.application.service.AwardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
     * 积分兑换（同步）
     * POST /api/award/exchange
     *
     * @SentinelResource 三个属性说明：
     *   value        — 资源名，与 SentinelConfig 中的规则绑定
     *   blockHandler — 触发流控/熔断时调用，签名 = 原方法参数 + BlockException，必须同类
     *   fallback     — 业务抛出普通异常时调用，签名 = 原方法参数 + Throwable，必须同类
     *
     * 修复点：
     *   ① blockHandler / fallback 各自独立，不再混用同一个方法
     *   ② fallback 里不再判断 instanceof BlockException（BlockException 不会流入 fallback）
     *   ③ fallback 区分业务异常（400）和系统异常（503）
     */
    @PostMapping("/api/award/exchange")
    @SentinelResource(
            value = "exchange",
            blockHandler = "exchangeBlockHandler",
            fallback = "exchangeFallback"
    )
    public R<Boolean> exchange(@Valid @ModelAttribute ExchangeRequest request) {
        return R.ok(awardService.exchange(
                request.getUserId(),
                request.getAwardId()));
    }

    /**
     * 积分兑换（异步 MQ 版）
     * POST /api/award/exchange/async
     *
     * 修复点：
     *   ① 独立的 blockHandler / fallback，文案与同步兑换区分
     *   ② async 是"投递 MQ 排队"，限流时提示"排队人数过多"而非"兑换失败"
     */
    @PostMapping("/api/award/exchange/async")
    @SentinelResource(
            value = "exchangeAsync",
            blockHandler = "exchangeAsyncBlockHandler",
            fallback = "exchangeAsyncFallback"
    )
    public R<Boolean> exchangeAsync(@Valid @ModelAttribute ExchangeRequest request) {
        return R.ok(awardService.exchangeAsync(
                request.getUserId(),
                request.getAwardId()));
    }

    /**
     * 查询兑换结果（轮询）
     * GET /api/award/exchange/result?exchangeId=
     */
    @GetMapping("/api/award/exchange/result")
    public R<ExchangeResultVO> getExchangeResult(
            @RequestParam
            @NotNull(message = "兑换ID不能为空")
            @Min(value = 1, message = "兑换ID必须大于0")
            Long exchangeId) {
        return R.ok(awardService.getExchangeResult(exchangeId));
    }

    // =========================================================
    // 同步兑换 handler（exchange）
    // =========================================================

    /**
     * 同步兑换 — 限流 / 熔断 handler
     *
     * 触发条件：QPS 超限 或 熔断器打开
     * 签名规则：原方法全部参数 + BlockException（末尾），必须同类，必须 public
     */
    public R<Boolean> exchangeBlockHandler(ExchangeRequest request, BlockException e) {
        log.warn("同步兑换触发限流，resource=exchange，userId={}, awardId={}，rule={}",
                request.getUserId(), request.getAwardId(), e.getRule());
        return R.fail(429, "活动太火爆了，请稍后再试～");
    }

    /**
     * 同步兑换 — 业务降级 fallback
     *
     * 触发条件：业务方法抛出任意 Throwable（不含 BlockException）
     * 签名规则：原方法全部参数 + Throwable（末尾），必须同类，必须 public
     *
     * 修复点：
     *   不再判断 instanceof BlockException —— BlockException 由 blockHandler 处理，
     *   永远不会到达 fallback，加了也是死代码
     */
    public R<Boolean> exchangeFallback(ExchangeRequest request, Throwable t) {
        // 业务异常（积分不足、库存不足、奖品不存在等）返回 400，让前端展示具体原因
        if (t instanceof RuntimeException) {
            log.warn("同步兑换业务异常，userId={}, awardId={}, msg={}",
                    request.getUserId(), request.getAwardId(), t.getMessage());
            return R.fail(400, t.getMessage());
        }
        // 系统异常（数据库超时、网络抖动等）返回 503
        log.error("同步兑换系统异常，userId={}, awardId={}",
                request.getUserId(), request.getAwardId(), t);
        return R.fail(503, "兑换服务暂时不可用，请稍后再试～");
    }

    // =========================================================
    // 异步兑换 handler（exchangeAsync）
    // =========================================================

    /**
     * 异步兑换 — 限流 / 熔断 handler
     *
     * 与同步兑换区分：async 是"投递 MQ 排队"，限流含义是"排队通道已满"
     */
    public R<Boolean> exchangeAsyncBlockHandler(ExchangeRequest request, BlockException e) {
        log.warn("异步兑换触发限流，resource=exchangeAsync，userId={}, awardId={}",
                request.getUserId(), request.getAwardId());
        return R.fail(429, "当前排队人数过多，请稍后再试～");
    }

    /**
     * 异步兑换 — 业务降级 fallback
     */
    public R<Boolean> exchangeAsyncFallback(ExchangeRequest request, Throwable t) {
        if (t instanceof RuntimeException) {
            log.warn("异步兑换业务异常，userId={}, awardId={}, msg={}",
                    request.getUserId(), request.getAwardId(), t.getMessage());
            return R.fail(400, t.getMessage());
        }
        log.error("异步兑换系统异常，userId={}, awardId={}",
                request.getUserId(), request.getAwardId(), t);
        return R.fail(503, "兑换服务暂时不可用，请稍后再试～");
    }
}