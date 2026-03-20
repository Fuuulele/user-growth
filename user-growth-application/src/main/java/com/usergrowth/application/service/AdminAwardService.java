package com.usergrowth.application.service;

import com.usergrowth.api.dto.*;

import java.util.List;

public interface AdminAwardService {

    /** 创建奖品（含库存初始化） */
    Long createAward(AwardCreateRequest request);

    /** 更新奖品信息 */
    boolean updateAward(AwardUpdateRequest request);

    /** 下架奖品 */
    boolean deleteAward(Long awardId);

    /** 查询所有奖品（含下架） */
    List<AwardVO> listAllAwards();

    /** 更新库存（重新拆分） */
    boolean updateStock(StockUpdateRequest request);

    /** 查询实时库存 */
    Integer queryStock(Long awardId);

    /**
     * 库存拆分配置（设定M值）
     * 不删除旧库存，在现有基础上重新按M份分配
     */
    boolean splitStock(StockSplitRequest request);
}