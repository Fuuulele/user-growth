-- ============================================================
-- 用户增长积分系统 — 数据库初始化脚本
-- 生成依据：所有 PO 类字段 + Mapper 查询条件 + 业务约束
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_growth
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE user_growth;

-- ------------------------------------------------------------
-- 1. activity（活动表）
--    AdminActivityServiceImpl 管理，目前与任务/奖品无外键关联
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `activity` (
                                          `id`          BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '活动ID',
                                          `name`        VARCHAR(64)  NOT NULL                  COMMENT '活动名称',
    `description` VARCHAR(255) NOT NULL DEFAULT ''       COMMENT '活动描述',
    `start_time`  DATETIME     NOT NULL                  COMMENT '活动开始时间',
    `end_time`    DATETIME     NOT NULL                  COMMENT '活动结束时间',
    `status`      TINYINT      NOT NULL DEFAULT 1        COMMENT '状态 1=启用 0=禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';


-- ------------------------------------------------------------
-- 2. task（任务配置表）
--    TaskPO：id 为 Integer（AUTO_INCREMENT），任务类型枚举固定
--    查询条件：status=1 过滤启用任务，id ASC 排序
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `task` (
                                      `id`            INT          NOT NULL AUTO_INCREMENT   COMMENT '任务ID',
                                      `task_name`     VARCHAR(64)  NOT NULL                  COMMENT '任务名称',
    `points_reward` INT          NOT NULL                  COMMENT '积分奖励',
    `task_type`     VARCHAR(16)  NOT NULL                  COMMENT '任务类型 DAILY=每日 ONE_TIME=一次性 ACTIVITY=活动',
    `status`        TINYINT      NOT NULL DEFAULT 1        COMMENT '状态 1=启用 0=禁用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务配置表';


-- ------------------------------------------------------------
-- 3. task_record（任务完成记录表）
--    核心：INSERT IGNORE 幂等，依赖 uk_idempotent_key 唯一索引
--    幂等 key = MD5(userId_taskId_bizDate)，ONE_TIME 任务 bizDate 固定为 "once"
--    查询：WHERE user_id=? AND biz_date=? 查当日完成；
--          WHERE user_id=? 查历史全部（用于 ONE_TIME 判断）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `task_record` (
                                             `id`             BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '记录ID',
                                             `user_id`        BIGINT       NOT NULL                  COMMENT '用户ID',
                                             `task_id`        INT          NOT NULL                  COMMENT '任务ID',
                                             `target_id`      VARCHAR(64)  NOT NULL DEFAULT ''       COMMENT '业务目标ID（扩展用，如刷题ID）',
    `biz_date`       DATE         NOT NULL                  COMMENT '业务日期（DAILY任务按天隔离）',
    `points_earned`  INT          NOT NULL                  COMMENT '获得积分',
    `idempotent_key` VARCHAR(64)  NOT NULL                  COMMENT '幂等Key MD5(userId_taskId_bizDate)',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),   -- INSERT IGNORE 幂等核心
    INDEX `idx_user_date` (`user_id`, `biz_date`),       -- 查当日完成任务
    INDEX `idx_user_id`   (`user_id`)                    -- 查历史全部完成任务
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务完成记录表';


-- ------------------------------------------------------------
-- 4. point_account（积分账户表）
--    @Version 乐观锁：version 字段由 MyBatis-Plus 自动管理
--    查询：WHERE user_id=? selectByUserId
--    更新：UPDATE ... SET balance=balance+? version=version+1 WHERE user_id=? AND version=?
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `point_account` (
                                               `id`         BIGINT   NOT NULL AUTO_INCREMENT   COMMENT '账户ID',
                                               `user_id`    BIGINT   NOT NULL                  COMMENT '用户ID',
                                               `balance`    INT      NOT NULL DEFAULT 0         COMMENT '积分余额',
                                               `version`    INT      NOT NULL DEFAULT 0         COMMENT '乐观锁版本号',
                                               `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
                                               PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)   -- 每个用户只有一个积分账户
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分账户表';


-- ------------------------------------------------------------
-- 5. point_flow（积分流水表）
--    只写不更新，记录每次积分变动明细
--    查询：WHERE user_id=? ORDER BY completed_at DESC 分页
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `point_flow` (
                                            `id`           BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '流水ID',
                                            `user_id`      BIGINT       NOT NULL                  COMMENT '用户ID',
                                            `task_name`    VARCHAR(64)  NOT NULL                  COMMENT '来源任务名称',
    `task_type`    VARCHAR(16)  NOT NULL                  COMMENT '任务类型',
    `points_earned` INT         NOT NULL                  COMMENT '获得积分（负数=扣减）',
    `completed_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `completed_at`)   -- 分页查询用
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水表';


-- ------------------------------------------------------------
-- 6. award（奖品配置表）
--    allow_oversell=1 → 启动时同步库存到 Redis；=0 → 拆分到 award_inventory_split
--    查询：WHERE status=1 展示上架奖品；WHERE allow_oversell=1 AND status=1 初始化 Redis 库存
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `award` (
                                       `id`            BIGINT        NOT NULL AUTO_INCREMENT   COMMENT '奖品ID',
                                       `reward_name`   VARCHAR(64)   NOT NULL                  COMMENT '奖品名称',
    `reward_desc`   VARCHAR(255)  NOT NULL DEFAULT ''       COMMENT '奖品描述',
    `cover`         VARCHAR(512)  NOT NULL DEFAULT ''       COMMENT '封面图URL',
    `points_cost`   INT           NOT NULL                  COMMENT '兑换所需积分',
    `total_stock`   INT           NOT NULL                  COMMENT '总库存',
    `allow_oversell` TINYINT      NOT NULL DEFAULT 0        COMMENT '是否允许超卖 1=是 0=否',
    `status`        TINYINT       NOT NULL DEFAULT 1        COMMENT '状态 1=上架 0=下架',
    `expire_time`   DATETIME      NULL                      COMMENT '活动截止时间（NULL=永不过期）',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`, `allow_oversell`)   -- 启动初始化 Redis 库存时用
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品配置表';


-- ------------------------------------------------------------
-- 7. award_inventory_split（库存拆分表）
--    对应技术方案"将库存N拆分成M份"，降低热点行锁竞争
--    @Version 乐观锁：UPDATE ... SET sub_stock=sub_stock-1 version=version+1 WHERE id=? AND version=?
--    查询：WHERE award_id=? AND sub_stock>0 随机选一条执行扣减
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `award_inventory_split` (
                                                       `id`        BIGINT  NOT NULL AUTO_INCREMENT   COMMENT '分片ID',
                                                       `award_id`  BIGINT  NOT NULL                  COMMENT '奖品ID',
                                                       `sub_stock` INT     NOT NULL DEFAULT 0         COMMENT '子库存数量',
                                                       `version`   INT     NOT NULL DEFAULT 0         COMMENT '乐观锁版本号',
                                                       PRIMARY KEY (`id`),
    INDEX `idx_award_stock` (`award_id`, `sub_stock`)   -- 查可用子库存
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品库存拆分表';


-- ------------------------------------------------------------
-- 8. exchange_record（兑换记录表）
--    主键 id = 雪花算法生成（非自增），注意 @TableId 无 IdType.AUTO
--    status: 0=待处理 1=成功 2=失败
--    MQ 事务消息：本地事务插入此表（status=0），消费者处理后更新 status
--    幂等：idempotent_id = String.valueOf(exchangeId)，唯一索引防重复消费
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exchange_record` (
                                                 `id`            BIGINT       NOT NULL              COMMENT '兑换ID（雪花算法，非自增）',
                                                 `user_id`       BIGINT       NOT NULL              COMMENT '用户ID',
                                                 `award_id`      BIGINT       NOT NULL              COMMENT '奖品ID',
                                                 `idempotent_id` VARCHAR(64)  NOT NULL              COMMENT '幂等ID（= exchangeId 字符串）',
    `status`        TINYINT      NOT NULL DEFAULT 0    COMMENT '状态 0=待处理 1=成功 2=失败',
    `points_cost`   INT          NOT NULL              COMMENT '消耗积分',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotent_id` (`idempotent_id`),   -- 防重复插入
    INDEX `idx_user_id`  (`user_id`),
    INDEX `idx_status`   (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换记录表';


-- ------------------------------------------------------------
-- 9. user_award（用户奖品表）
--    兑换成功后由消费者写入，记录用户持有的奖品
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_award` (
                                            `id`          BIGINT   NOT NULL AUTO_INCREMENT   COMMENT '记录ID',
                                            `user_id`     BIGINT   NOT NULL                  COMMENT '用户ID',
                                            `award_id`    BIGINT   NOT NULL                  COMMENT '奖品ID',
                                            `exchange_id` BIGINT   NOT NULL                  COMMENT '兑换记录ID',
                                            `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发放时间',
                                            PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exchange_id` (`exchange_id`),   -- 同一兑换记录只能发一次奖品
    INDEX `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户奖品表';


-- ============================================================
-- 初始化基础数据
-- ============================================================

-- 初始化任务（对应 README 接口示例中的数据）
INSERT INTO `task` (`task_name`, `points_reward`, `task_type`, `status`) VALUES
                                                                             ('每日签到',       10,  'DAILY',    1),
                                                                             ('完成一道刷题',   20,  'DAILY',    1),
                                                                             ('分享好友',       15,  'DAILY',    1),
                                                                             ('分享朋友圈',     15,  'DAILY',    1),
                                                                             ('完善个人简历',  100,  'ONE_TIME', 1),
                                                                             ('绑定手机号',     50,  'ONE_TIME', 1),
                                                                             ('完成新手引导',   30,  'ONE_TIME', 1);

-- 初始化示例奖品（allow_oversell=1 虚拟商品走 Redis，=0 实物走库存拆分）
INSERT INTO `award` (`reward_name`, `reward_desc`, `points_cost`, `total_stock`, `allow_oversell`, `status`) VALUES
                                                                                                                 ('7天会员',   '体验全站VIP功能',  50,  10000, 1, 1),
                                                                                                                 ('30天会员',  '尊享全站VIP特权', 150,   5000, 1, 1),
                                                                                                                 ('品牌耳机',  '头部品牌联名款',  800,    100, 0, 1),
                                                                                                                 ('演唱会门票','热门艺人演唱会',  500,     50, 0, 1);

-- 为不允许超卖的奖品初始化库存拆分（每个奖品拆 4 份）
-- 品牌耳机（假设 id=3，总库存 100，拆 4 份每份 25）
INSERT INTO `award_inventory_split` (`award_id`, `sub_stock`, `version`) VALUES
                                                                             (3, 25, 0), (3, 25, 0), (3, 25, 0), (3, 25, 0);

-- 演唱会门票（假设 id=4，总库存 50，拆 4 份，前 2 份 13，后 2 份 12）
INSERT INTO `award_inventory_split` (`award_id`, `sub_stock`, `version`) VALUES
                                                                             (4, 13, 0), (4, 13, 0), (4, 12, 0), (4, 12, 0);