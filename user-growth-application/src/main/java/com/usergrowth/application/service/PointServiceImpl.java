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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointAccountMapper pointAccountMapper;
    private final PointFlowMapper pointFlowMapper;

    @Override
    public Integer getBalance(Long userId) {
        PointAccountPO account = pointAccountMapper.selectByUserId(userId);
        if (account == null) {
            return 0;
        }
        return account.getBalance();
    }

    @Override
    public PageResult<PointFlowVO> getPointHistory(Long userId, Integer page, Integer size) {
        // 分页查询
        IPage<PointFlowPO> pageResult = pointFlowMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PointFlowPO>()
                        .eq(PointFlowPO::getUserId, userId)
                        .orderByDesc(PointFlowPO::getCompletedAt)
        );

        // 转换 VO
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