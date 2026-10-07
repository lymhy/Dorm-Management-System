# Design Documentation · 员工宿舍管理系统

> 最后校准：2026-09-23（与当前后端控制器/实体、前端视图、数据库表结构核对）

## 一、技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3.4.5、MyBatis-Plus 3.5.x、Sa-Token 1.39（无状态 JWT：sa-token-jwt + hutool-jwt）、SpringDoc OpenAPI 2.7.0、BCrypt |
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios + ECharts 5 + SheetJS(xlsx) |
| 数据库 | MySQL 8.0（utf8mb4） |
| 运行环境 | JDK 21（`D:\tools\jdk21\jdk-21.0.7+6`）、Node 18+、Maven 3.9.9 |
| 端口 | 后端 8080，前端 5173（Vite 代理 `/api` → 8080） |

## 二、模块清单（17 个后端模块 / 19 个前端页面）

| 模块 | 后端前缀 | 说明 | 角色 |
|---|---|---|---|
| Auth 认证 | `/api/auth` | 登录、当前用户信息、改密 | 全部 |
| Dashboard 仪表盘 | `/api/dashboard` | 聚合统计（管理员/员工两套卡片） | 全部 |
| Building 宿舍楼 | `/api/building` | CRUD + 关键字搜索 | 管理员 |
| Room 房间 | `/api/room` | CRUD + 楼栋/楼层/状态筛选 | 管理员 |
| Employee 员工 | `/api/employee` | CRUD + 关键字/部门/性别/状态多级筛选、`/me`、`/my-allocation`、`/depts` | 管理员 / 员工 |
| Allocation 入住 | `/api/allocation` | CRUD | 管理员 |
| Fee 水电费 | `/api/fee` | CRUD + 筛选 + 汇总 + 报表 + 批量标记/删除 + 一键催缴 + 自动算费 | 管理员 / 员工(只读) |
| FeePayment 缴费流水 | `/api/fee-payment` | 流水查询/补录/删除、按账单生成收据 | 管理员 |
| Pay 在线支付 | `/api/pay` | 员工在线缴费下单/确认/关闭/订单查询 | 员工 / 管理员(代付、订单管理) |
| Repair 报修 | `/api/repair` | CRUD + 状态流转触发通知（员工提交固定为待处理、报修人取登录身份） | 全部 |
| Visitor 访客 | `/api/visitor` | 访客登记 CRUD | 全部 |
| Announcement 公告 | `/api/announcement` | CRUD + 最新公告 | 管理员发布 / 全部浏览 |
| ChangeRoom 调宿 | `/api/change-room` | 员工申请 + 管理员审批 | 员工 / 管理员 |
| MoveOut 退宿 | `/api/move-out` | 员工申请 + 管理员审批 | 员工 / 管理员 |
| Notification 消息通知 | `/api/notification` | 未读数、已读/未读、全部已读 | 员工 |
| SystemConfig 系统参数 | `/api/system-config` | 水价/电价/押金/预警阈值 | 管理员 |
| User 用户管理 | `/api/user` | 账号 CRUD + 重置密码 + 启用禁用 + Excel 批量导入 | 管理员 |

## 三、分层架构

```
controller/   REST 入口，参数校验、Result 包装
service/ + service/impl/   业务逻辑（继承 MyBatis-Plus IService）
mapper/       MyBatis-Plus BaseMapper（少量 @Select 自定义 SQL）
entity/       表实体（@TableName，逻辑删除 deleted）
config/       WebConfig(CORS + Sa-Token 无状态 JWT StpLogic + 登录拦截器)、JacksonConfig(LocalDateTime 多格式兼容)、MybatisPlusConfig、MyMetaObjectHandler、DataInitializer
util/         AuthUtil、Result、NotificationHelper
pay/          PaymentGateway 抽象 + MockPaymentGateway 模拟通道（可插拔）
exception/    GlobalExceptionHandler
```

## 四、关键状态定义（与代码一致）

| 实体 | 字段 | 取值 |
|---|---|---|
| room | status | 0 空 / 1 部分入住 / 2 满 / 3 维修 |
| room | type | 1 单人间 / 2 双人间 / 4 四人间 / 6 六人间 |
| allocation | status | 1 在住 / 0 已退宿 |
| fee | paid | 0 未缴 / 1 已缴 |
| repair | status | 0 待处理 / 1 已接单(处理中) / 2 已完成 |
| change_room / move_out | status | 0 待审批 / 1 通过 / 2 拒绝 |
| employee / sys_user | status | 1 启用(在职) / 0 禁用(离职) |
| announcement | priority | 1 普通 / 2 重要 / 3 紧急 |
| pay_order | status | PENDING 待支付 / SUCCESS 成功 / CLOSED 关闭 |
| notification | isRead | 0 未读 / 1 已读 |

## 五、数据库（15 张表）

`database/` 下按模块拆分脚本，**全新环境需依次执行**：

| 脚本 | 建表 |
|---|---|
| `init.sql` | sys_user、building、room、employee、allocation、fee、repair、visitor、announcement、system_config、change_room、move_out（12 张）+ 初始数据 |
| `notification.sql` | notification + `utility_threshold` 参数 |
| `fee_payment.sql` | fee_payment（缴费流水） |
| `payment.sql` | pay_order（在线支付订单） |

所有业务表统一带审计字段：`creator / create_time / updater / update_time / deleted / tenant_id`；`deleted` 为逻辑删除标记（0 未删 / 1 已删），由 `application.yml` 的 `logic-delete-field` 全局接管。

## 六、核心业务流程

### 6.1 认证与身份映射
`登录 → Sa-Token 签发无状态 JWT(username) → 请求携带 Bearer → SaInterceptor 校验 → controller 用 token 中的 username 反查 SysUser → realName → Employee.name → employee.id`（员工数据隔离依据）

### 6.2 缴费闭环
```
管理员生成账单(fee) → 员工看到 /fee/my
  ├─ 线下缴纳：管理员 PUT /fee/{id}/paid 或批量 → 自动写 fee_payment
  └─ 在线支付：POST /pay/order(PENDING) → POST /pay/confirm(SUCCESS)
              → 账单 paid=1 + 写 fee_payment(payMethod=在线支付) + 发 fee_paid 通知
超阈值(total > utility_threshold) → 发 fee_warning
未缴账单 → 一键催缴 → 发 fee_urge
```

### 6.3 通知触发点
| 触发动作 | type | 接收人 |
|---|---|---|
| 报修状态改为 1 | repair_accept | 报修员工 |
| 报修状态改为 2 | repair_finish | 报修员工 |
| 账单金额超阈值 | fee_warning | 该房间在住员工 |
| 一键催缴 | fee_urge | 未缴账单房间在住员工 |
| 在线支付成功 | fee_paid | 该房间在住员工 |
| 调宿审批通过/驳回 | change_room_result | 申请员工 |

### 6.4 支付网关（可插拔）
`PaymentGateway` 接口：`channel()` / `createPayment(order)`；`PayOrderServiceImpl` 按 `pay_order.channel` 从 Spring 容器路由到实现。当前仅 `MockPaymentGateway`（channel=`mock`，返回模拟收银台地址）；接入微信/支付宝时新增实现类注册为 Bean 即可，订单状态仍由 `PayOrderService` 统一管理。

## 七、前端结构

- 路由 `src/router/index.js`：全部视图懒加载（`() => import(...)`）；`meta.roles` 控制角色可见性（admin / employee）。
- 菜单 `src/views/Layout.vue`：`adminGroups` / `employeeGroups` 两组，按登录角色渲染；顶部消息铃铛仅员工可见（30s 轮询未读数）。
- 通用组件：`ListPageHeader`（页头+统计芯片）、`EmptyState`（空态）、`FeeReceipt`（可打印收据）、`PayCashier`（在线缴费收银台弹窗）。
- API 层 `src/api/*.js`：统一走 `request.js`（axios 实例，baseURL `/api`，401 踢回登录页）。
- 导出/导入：Excel 全部由前端 SheetJS 完成（用户导入、费用/流水导出），后端零 POI 依赖。
