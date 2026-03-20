package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.usergrowth.api.dto.ActivityCreateRequest;
import com.usergrowth.api.dto.ActivityUpdateRequest;
import com.usergrowth.api.dto.ActivityVO;
import com.usergrowth.infrastructure.mapper.ActivityMapper;
import com.usergrowth.infrastructure.po.ActivityPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminActivityServiceImpl implements AdminActivityService {

    private final ActivityMapper activityMapper;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Long createActivity(ActivityCreateRequest request) {
        ActivityPO activity = new ActivityPO();
        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setStartTime(LocalDateTime.parse(request.getStartTime(), FORMATTER));
        activity.setEndTime(LocalDateTime.parse(request.getEndTime(), FORMATTER));
        activity.setStatus(1);
        activityMapper.insert(activity);
        log.info("创建活动成功，activityId={}，name={}", activity.getId(), activity.getName());
        return activity.getId();
    }

    @Override
    public boolean updateActivity(ActivityUpdateRequest request) {
        ActivityPO activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new RuntimeException("活动不存在，activityId=" + request.getActivityId());
        }
        if (request.getName() != null) activity.setName(request.getName());
        if (request.getDescription() != null) activity.setDescription(request.getDescription());
        if (request.getStartTime() != null) {
            activity.setStartTime(LocalDateTime.parse(request.getStartTime(), FORMATTER));
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(LocalDateTime.parse(request.getEndTime(), FORMATTER));
        }
        if (request.getStatus() != null) activity.setStatus(request.getStatus());
        activityMapper.updateById(activity);
        log.info("更新活动成功，activityId={}", request.getActivityId());
        return true;
    }

    @Override
    public boolean deleteActivity(Long activityId) {
        ActivityPO activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在，activityId=" + activityId);
        }
        activity.setStatus(0);
        activityMapper.updateById(activity);
        log.info("删除活动成功（逻辑删除），activityId={}", activityId);
        return true;
    }

    @Override
    public List<ActivityVO> listActivities() {
        List<ActivityPO> activities = activityMapper.selectList(
                new LambdaQueryWrapper<ActivityPO>().orderByDesc(ActivityPO::getId)
        );
        return activities.stream().map(po -> {
            ActivityVO vo = new ActivityVO();
            vo.setId(po.getId());
            vo.setName(po.getName());
            vo.setDescription(po.getDescription());
            vo.setStartTime(po.getStartTime().toString());
            vo.setEndTime(po.getEndTime().toString());
            vo.setStatus(po.getStatus() == 1 ? "ENABLED" : "DISABLED");
            return vo;
        }).collect(Collectors.toList());
    }
}