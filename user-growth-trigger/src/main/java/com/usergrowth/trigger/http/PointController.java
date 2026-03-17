package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.PageResult;
import com.usergrowth.api.dto.PointFlowVO;
import com.usergrowth.application.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * 查询积分余额
     * GET /api/point/balance
     */
    @GetMapping("/api/point/balance")
    public R<Integer> getBalance(@RequestParam Long userId) {
        return R.ok(pointService.getBalance(userId));
    }
    //接口三：查询积分余额
    //GET http://localhost:8080/api/point/balance?userId=1001

    /**
     * 查询积分流水
     * GET /api/point/history
     */
    @GetMapping("/api/point/history")
    public R<PageResult<PointFlowVO>> getPointHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(pointService.getPointHistory(userId, page, size));
    }
    //接口四：查询积分流水
    //GET http://localhost:8080/api/point/history?userId=1001&page=1&size=10
}
