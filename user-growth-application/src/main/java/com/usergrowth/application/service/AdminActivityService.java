package com.usergrowth.application.service;

import com.usergrowth.api.dto.ActivityCreateRequest;
import com.usergrowth.api.dto.ActivityUpdateRequest;
import com.usergrowth.api.dto.ActivityVO;

import java.util.List;

public interface AdminActivityService {

    /** 创建活动 */
    Long createActivity(ActivityCreateRequest request);

    /** 更新活动 */
    boolean updateActivity(ActivityUpdateRequest request);

    /** 删除活动（逻辑删除） */
    boolean deleteActivity(Long activityId);

    /** 查询活动列表 */
    List<ActivityVO> listActivities();
}