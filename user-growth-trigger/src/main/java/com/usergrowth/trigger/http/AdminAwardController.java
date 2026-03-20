package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.*;
import com.usergrowth.application.service.AdminAwardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class AdminAwardController {

    private final AdminAwardService adminAwardService;

    /** 创建奖品 POST /api/b/award/create */
    @PostMapping("/api/b/award/create")
    public R<Long> createAward(@Valid @ModelAttribute AwardCreateRequest request) {
        return R.ok(adminAwardService.createAward(request));
    }

    /** 更新奖品 PUT /api/b/award/update */
    @PutMapping("/api/b/award/update")
    public R<Boolean> updateAward(@Valid @ModelAttribute AwardUpdateRequest request) {
        return R.ok(adminAwardService.updateAward(request));
    }

    /** 下架奖品 DELETE /api/b/award/delete */
    @DeleteMapping("/api/b/award/delete")
    public R<Boolean> deleteAward(
            @RequestParam
            @NotNull(message = "奖品ID不能为空")
            @Min(value = 1, message = "奖品ID必须大于0")
            Long awardId) {
        return R.ok(adminAwardService.deleteAward(awardId));
    }

    /** 查询所有奖品 GET /api/b/award/list */
    @GetMapping("/api/b/award/list")
    public R<List<AwardVO>> listAllAwards() {
        return R.ok(adminAwardService.listAllAwards());
    }

    /** 更新库存 PUT /api/b/award/stock/update */
    @PutMapping("/api/b/award/stock/update")
    public R<Boolean> updateStock(@Valid @ModelAttribute StockUpdateRequest request) {
        return R.ok(adminAwardService.updateStock(request));
    }

    /** 查询实时库存 GET /api/b/award/stock/query */
    @GetMapping("/api/b/award/stock/query")
    public R<Integer> queryStock(
            @RequestParam
            @NotNull(message = "奖品ID不能为空")
            @Min(value = 1, message = "奖品ID必须大于0")
            Long awardId) {
        return R.ok(adminAwardService.queryStock(awardId));
    }

    /** 库存拆分配置 POST /api/b/award/stock/split */
    @PostMapping("/api/b/award/stock/split")
    public R<Boolean> splitStock(@Valid @ModelAttribute StockSplitRequest request) {
        return R.ok(adminAwardService.splitStock(request));
    }
}