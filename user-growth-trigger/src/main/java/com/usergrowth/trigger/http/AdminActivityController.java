package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import com.usergrowth.api.dto.ActivityCreateRequest;
import com.usergrowth.api.dto.ActivityUpdateRequest;
import com.usergrowth.api.dto.ActivityVO;
import com.usergrowth.application.service.AdminActivityService;
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
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    /** 创建活动 POST /api/b/activity/create */
    @PostMapping("/api/b/activity/create")
    public R<Long> createActivity(@Valid @ModelAttribute ActivityCreateRequest request) {
        return R.ok(adminActivityService.createActivity(request));
    }

    /** 更新活动 PUT /api/b/activity/update */
    @PutMapping("/api/b/activity/update")
    public R<Boolean> updateActivity(@Valid @ModelAttribute ActivityUpdateRequest request) {
        return R.ok(adminActivityService.updateActivity(request));
    }

    /** 删除活动 DELETE /api/b/activity/delete */
    @DeleteMapping("/api/b/activity/delete")
    public R<Boolean> deleteActivity(
            @RequestParam
            @NotNull(message = "活动ID不能为空")
            @Min(value = 1, message = "活动ID必须大于0")
            Long activityId) {
        return R.ok(adminActivityService.deleteActivity(activityId));
    }

    /** 活动列表 GET /api/b/activity/list */
    @GetMapping("/api/b/activity/list")
    public R<List<ActivityVO>> listActivities() {
        return R.ok(adminActivityService.listActivities());
    }
}