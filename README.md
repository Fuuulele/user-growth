# 用户增长积分系统

> 基于 Spring Boot 3 的用户激励与积分兑换平台，支持任务完成上报、积分发放、高并发秒杀兑换等核心业务。

---

## 项目背景

本项目为 XXX 求职小程序的**用户增长（留存）模块**，通过构建任务激励与积分兑换体系提升用户日活与长期粘性。用户可通过完成签到、刷题、分享等任务获取积分，再使用积分兑换会员、周边礼品、耳机、手机等奖品。积分兑换场景支持数十万用户同时参与的高并发秒杀模式。

**预期业务指标：**
- DAU 增长 30%~40%
- 7 日留存率从 35% 提升至 50%
- 用户获取成本（CAC）降低 15%~25%

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 基础框架 | Spring Boot 3.2.5 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.x + Caffeine 3.x |
| 消息队列 | RocketMQ 5.x（预留） |
| 分布式ID | 雪花算法 |
| 构建工具 | Maven 3.9 |
| JDK | Java 21 |

---

## 工程结构

```
user-growth-parent
├── user-growth-api              # 接口定义层：DTO、枚举、统一响应 R<T>
├── user-growth-domain           # 领域层：领域模型、业务规则
├── user-growth-infrastructure   # 基础设施层：Mapper、Redis、配置
├── user-growth-application      # 应用服务层：业务编排、事务控制
└── user-growth-trigger          # 触发层：Controller、启动类
```

---

## 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 7.x
- Maven 3.9+

### 1. 克隆项目

```bash
git clone https://gitee.com/your-repo/user-growth-parent.git
cd user-growth-parent
```

### 2. 初始化数据库

连接 MySQL，执行 `sql/init.sql`（包含建库、建表、初始化任务数据）：

```bash
mysql -u root -p < sql/init.sql
```

或在 Navicat 中手动执行以下 SQL 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS user_growth
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 3. 修改配置

编辑 `user-growth-trigger/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_growth?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 编译启动

```bash
mvn clean install -DskipTests
cd user-growth-trigger
mvn spring-boot:run
```

启动成功后访问：`http://localhost:8080`

---

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `task` | 任务配置表（任务名、积分奖励、类型） |
| `task_record` | 任务完成记录（幂等唯一索引） |
| `point_account` | 积分账户表（乐观锁版本号） |
| `point_flow` | 积分流水表 |
| `award` | 奖品配置表（是否允许超卖） |
| `award_inventory_split` | 库存拆分表（降低热点更新竞争） |
| `exchange_record` | 兑换记录表（状态机：待处理→成功/失败） |
| `user_award` | 用户奖品表 |

---

## API 接口

### 任务模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/task/list?userId=` | 获取任务列表（含完成状态） |
| POST | `/api/task/complete?userId=&taskId=` | 完成任务上报，发放积分 |

**获取任务列表示例：**

```json
GET /api/task/list?userId=1001

{
  "code": 200,
  "message": "success",
  "data": [
    {
      "taskID": 1,
      "taskName": "每日签到",
      "pointsReward": 10,
      "taskType": "DAILY",
      "taskStatus": "COMPLETED"
    },
    {
      "taskID": 2,
      "taskName": "完成一道刷题",
      "pointsReward": 20,
      "taskType": "DAILY",
      "taskStatus": "AVAILABLE"
    }
  ]
}
```

### 积分模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/point/balance?userId=` | 查询积分余额 |
| GET | `/api/point/history?userId=&page=&size=` | 分页查询积分流水 |

**查询积分余额示例：**

```json
GET /api/point/balance?userId=1001

{
  "code": 200,
  "message": "success",
  "data": 160
}
```

**查询积分流水示例：**

```json
GET /api/point/history?userId=1001&page=1&size=10

{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "taskName": "每日签到",
        "pointsEarned": 10,
        "completedAt": "2026-03-18T10:30:00",
        "taskType": "DAILY"
      }
    ],
    "total": 5,
    "page": 1,
    "size": 10,
    "totalPages": 1
  }
}
```

### 奖品模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/award/list` | 获取奖品列表（实时库存） |
| POST | `/api/award/exchange?userId=&awardId=` | 积分兑换奖品 |

**获取奖品列表示例：**

```json
GET /api/award/list

{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "rewardName": "7天会员",
      "rewardDesc": "体验全站VIP功能",
      "cover": "",
      "pointsCost": 50,
      "stock": 99,
      "totalStock": 100
    }
  ]
}
```

---

## 核心设计

### 幂等设计

任务完成上报通过 **唯一索引 + INSERT IGNORE** 实现幂等：

- 幂等 Key：`MD5(userId + "_" + taskId + "_" + bizDate)`
- 每日任务含日期，每天可完成一次
- 一次性任务不含日期，全局只能完成一次
- `INSERT IGNORE` 受影响行数为 0 时直接返回成功，保证接口幂等

### 积分发放（乐观锁）

```
SELECT version FROM point_account WHERE user_id = ?
UPDATE point_account SET balance = balance + ?, version = version + 1
WHERE user_id = ? AND version = ?
```

CAS 更新失败时最多重试 3 次，防止并发冲突。

### 兑换库存扣减（双路径）

| 场景 | 路径 | 原理 |
|------|------|------|
| 允许超卖（虚拟商品）| Redis DECR | 内存操作无锁，TPS 极高，接受极少量超卖风险 |
| 不允许超卖（实物商品）| 库存拆分 + 乐观锁 | 总库存 N 拆为 M 份，随机路由降低锁竞争，吞吐提升约 M 倍 |

### 缓存策略

| 数据 | 缓存层 | TTL | 更新策略 |
|------|--------|-----|----------|
| 积分余额 | Redis | 30 分钟 | 积分变动后 DEL 缓存 |
| 任务列表 | Caffeine（本地） | 5 分钟 | TTL 自动失效 |

### 分布式 ID（雪花算法）

兑换记录主键使用自研雪花算法生成，结构：

```
1位符号位 | 41位时间戳 | 5位数据中心ID | 5位机器ID | 12位序列号
```

保证趋势递增、全局唯一，对 MySQL B+ 树索引友好。

---

## 错误码说明

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务错误（积分不足、库存不足、参数错误等） |
| 500 | 系统内部错误 |

---

## 项目进度

- [x] 工程搭建（Maven 多模块）
- [x] 数据库设计与初始化
- [x] 任务模块（列表查询、完成上报、幂等）
- [x] 积分模块（余额查询、流水分页）
- [x] 奖品模块（列表查询、积分兑换）
- [x] 全局异常处理
- [x] 一次性任务跨天判断
- [x] Redis 积分余额缓存
- [x] Caffeine 任务列表本地缓存
- [x] 允许超卖场景（Redis DECR）
- [x] 参数校验（@Valid）
- [x] MQ 事务消息 + 异步兑换
- [ ] Sentinel 限流熔断
- [ ] B 端运营管理接口
- [ ] 单元测试

---

## 开发环境

| 工具 | 版本 |
|------|------|
| IDEA | 2024.1 |
| JDK | 21 |
| Maven | 3.9.12 |
| MySQL | 8.0 |
| Redis | 7.x（Windows 本地） |
| Navicat | Premium Lite 17 |
| Apifox | 最新版 |

---

## License

MIT