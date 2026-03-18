package com.usergrowth.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usergrowth.api.dto.PageResult;
import com.usergrowth.api.dto.PointFlowVO;
import com.usergrowth.infrastructure.mapper.PointAccountMapper;
import com.usergrowth.infrastructure.mapper.PointFlowMapper;
import com.usergrowth.infrastructure.po.PointAccountPO;
import com.usergrowth.infrastructure.po.PointFlowPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointAccountMapper pointAccountMapper;
    private final PointFlowMapper pointFlowMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 前缀
    private static final String BALANCE_KEY = "point:balance:";
    // 缓存 30 分钟
    private static final long BALANCE_TTL = 30;

    @Override
    public Integer getBalance(Long userId) {
        String key = BALANCE_KEY + userId;

        // 1. 先读 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("积分余额缓存命中，userId={}", userId);
            return (Integer) cached;
        }

        // 2. 缓存未命中，查数据库
        PointAccountPO account = pointAccountMapper.selectByUserId(userId);
        int balance = account == null ? 0 : account.getBalance();

        // 3. 写入缓存
        redisTemplate.opsForValue().set(key, balance, BALANCE_TTL, TimeUnit.MINUTES);
        log.info("积分余额写入缓存，userId={}，balance={}", userId, balance);

        return balance;
    }

    @Override
    public PageResult<PointFlowVO> getPointHistory(Long userId, Integer page, Integer size) {
        IPage<PointFlowPO> pageResult = pointFlowMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PointFlowPO>()
                        .eq(PointFlowPO::getUserId, userId)
                        .orderByDesc(PointFlowPO::getCompletedAt)
        );

        List<PointFlowVO> list = pageResult.getRecords().stream().map(po -> {
            PointFlowVO vo = new PointFlowVO();
            vo.setTaskName(po.getTaskName());
            vo.setPointsEarned(po.getPointsEarned());
            vo.setCompletedAt(po.getCompletedAt().toString());
            vo.setTaskType(po.getTaskType());
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(list, pageResult.getTotal(), page, size);
    }
}