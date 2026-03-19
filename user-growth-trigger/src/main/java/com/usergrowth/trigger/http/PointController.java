package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.PageResult;
import com.usergrowth.api.dto.PointFlowVO;
import com.usergrowth.api.dto.PointHistoryRequest;
import com.usergrowth.application.service.PointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * 查询积分余额
     * GET /api/point/balance?userId=
     */
    @GetMapping("/api/point/balance")
    public R<Integer> getBalance(
            @RequestParam
            @NotNull(message = "用户ID不能为空")
            @Min(value = 1, message = "用户ID必须大于0")
            Long userId) {
        return R.ok(pointService.getBalance(userId));
    }

    /**
     * 查询积分流水
     * GET /api/point/history
     */
    @GetMapping("/api/point/history")
    public R<PageResult<PointFlowVO>> getPointHistory(
            @Valid @ModelAttribute PointHistoryRequest request) {
        return R.ok(pointService.getPointHistory(
                request.getUserId(),
                request.getPage(),
                request.getSize()));
    }
}