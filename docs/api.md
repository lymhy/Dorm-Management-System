# API Documentation · 员工宿舍管理系统

> Base URL: `http://localhost:8080/api`
> 最后校准：2026-09-23（与 `backend/src/main/java/com/example/dorm/controller` 逐接口核对）
> Swagger UI: http://localhost:8080/doc.html

---

## 一、通用约定

### 1.1 统一响应体

所有接口返回统一 JSON 包装（`com.example.dorm.util.Result`）：

```json
{ "code": 200, "message": "success", "data": { } }
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 401 | 未登录 / 登录失败 / 权限不足（业务码，注意 HTTP 状态多为 200） |
| 403 | 账号被禁用（登录时） |
| 500 | 服务端异常 |

### 1.2 认证

- 登录成功后返回 Sa-Token 签发的**无状态 JWT**，后续请求需带请求头：
  `Authorization: Bearer <token>`
- 拦截器 `SaInterceptor` 拦截 `/api/**`，放行 `/api/auth/login`、`/doc.html`、`/v3/api-docs/**`、`/swagger-ui/**`。
- 未携带/校验失败 → 响应体 `{"code":401,"message":"未登录或登录已过期"}`（HTTP 状态 200）。

### 1.3 分页

分页接口参数为 `current`（页码，默认 1）、`size`（每页条数，默认 10），返回 MyBatis-Plus 的 `Page`：
`{ records: [], total, size, current, pages }`

> 注意：部分接口（`/fee/my`、`/repair/my`、`/visitor/my`）返回的 `data` 就是 `Page` 对象本身（`records`/`total` 在 `data` 下）。

### 1.4 业务角色约定

| 场景 | 说明 |
|---|---|
| 员工身份识别 | token 中的 `username` → `sys_user.real_name` → `employee.name` → `employee.id` |
| 管理员判断 | `sys_user.role == 'admin'` |
| 员工数据隔离 | `/my` 类接口按当前登录员工过滤；员工不可访问管理端接口（前端路由 + 菜单已限制，后端部分接口返回 401） |

### 1.5 时间字段格式

请求体中的时间字段（如 `reportTime`、`timeIn`、`payTime`）服务端统一按 `LocalDateTime` 解析，以下格式均可接收：

| 格式 | 示例 |
|---|---|
| ISO（响应默认格式） | `2026-10-07T16:54:14` |
| 空格分隔 | `2026-10-07 16:54:14` |
| 斜杠分隔 | `2026/10/7 16:54:14` |

> 由 `config/JacksonConfig` 的宽松反序列化器实现；空字符串按 `null` 处理。响应中的时间字段为 ISO-8601 字符串（`yyyy-MM-ddTHH:mm:ss`）。

---

## 二、接口清单

### 2.1 Auth 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录，返回 `{token, username, realName, role}` |
| GET | `/api/auth/info` | 当前登录用户信息（需 token） |
| PUT | `/api/auth/password` | 修改当前用户密码（body: `{oldPassword, newPassword}`） |

### 2.2 Dashboard 仪表盘 `/api/dashboard`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/dashboard/stats` | 聚合统计：buildingCount / roomCount / roomEmptyCount / employeeCount / residentCount / repairPendingCount / feeUnpaidCount / visitorTodayCount |

### 2.3 Building 宿舍楼 `/api/building`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/building/page` | 分页（keyword 模糊匹配名称/编号/地址/负责人） |
| GET | `/api/building/list` | 全量列表（下拉用） |
| GET | `/api/building/{id}` | 详情 |
| POST | `/api/building` | 新增 |
| PUT | `/api/building` | 修改 |
| DELETE | `/api/building/{id}` | 删除 |

### 2.4 Room 房间 `/api/room`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/room/page` | 分页（筛选：buildingId / floor / status / keyword） |
| GET | `/api/room/list` | 全量列表 |
| GET | `/api/room/{id}` | 详情 |
| POST | `/api/room` | 新增 |
| PUT | `/api/room` | 修改 |
| DELETE | `/api/room/{id}` | 删除 |

### 2.5 Employee 员工 `/api/employee`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/employee/page` | 分页（筛选：keyword / status / dept / gender） |
| GET | `/api/employee/depts` | 部门列表（去重，下拉用） |
| GET | `/api/employee/list` | 全量列表 |
| GET | `/api/employee/me` | 当前员工的档案（员工端） |
| GET | `/api/employee/my-allocation` | 当前员工的入住信息（含 room / building 嵌套） |
| GET | `/api/employee/{id}` | 详情 |
| POST | `/api/employee` | 新增 |
| PUT | `/api/employee` | 修改 |
| DELETE | `/api/employee/{id}` | 删除 |

### 2.6 Allocation 入住 `/api/allocation`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/allocation/page` | 分页 |
| GET | `/api/allocation/list` | 全量列表 |
| GET | `/api/allocation/{id}` | 详情 |
| POST | `/api/allocation` | 新增入住 |
| PUT | `/api/allocation` | 修改 |
| DELETE | `/api/allocation/{id}` | 删除 |

### 2.7 Fee 水电费 `/api/fee`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/fee/page` | 分页（筛选：roomId / month / paid / keyword 房间号）。员工调用时 `data` 为本人账单分页 |
| GET | `/api/fee/summary` | 汇总 `{count, paidCount, unpaidCount, totalAmount, paidAmount, unpaidAmount}` |
| GET | `/api/fee/report` | 报表：`byBuilding` / `byFloor` / `trend`（按月） |
| GET | `/api/fee/my` | 当前员工的账单分页 |
| GET | `/api/fee/list` | 全量列表 |
| GET | `/api/fee/{id}` | 详情 |
| POST | `/api/fee` | 新增账单（total 为空时按水费+电费自动补齐） |
| POST | `/api/fee/urge` | 一键催缴（body: `{ids:[], month}`），给未缴账单的在住员工发通知，返回 `{bills, notified}` |
| PUT | `/api/fee` | 修改账单 |
| PUT | `/api/fee/{id}/paid` | 标记单条已缴/未缴 |
| PUT | `/api/fee/batch-paid` | 批量标记（body: `{ids:[], paid:0/1}`） |
| DELETE | `/api/fee/batch` | 批量删除（body: `{ids:[]}`） |
| DELETE | `/api/fee/{id}` | 删除 |

> 所有「未缴 → 已缴」的路径（单条标记 / 批量标记 / 新增即已缴 / 修改为已缴 / 在线支付成功）都会自动写入 `fee_payment` 缴费流水（同一账单不重复写），并按 `system_config.utility_threshold`（默认 200）触发超限预警通知。
> 报价说明：`POST /api/fee`、`PUT /api/fee`、`PUT /api/fee/{id}/paid` 等写接口返回 `data` 为布尔值，非实体；新建后如需 ID 请用 `GET /api/fee/page` 按房间+月份回查。

### 2.8 FeePayment 缴费流水 `/api/fee-payment`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/fee-payment/page` | 分页（筛选：keyword 房间号 / roomId / month / payMethod），行内富化 roomNo/buildingName/floor/employeeName |
| GET | `/api/fee-payment/by-fee/{feeId}` | 按账单取缴费记录（生成收据用），返回单条缴费对象（含 roomNo/buildingName/employeeName） |
| GET | `/api/fee-payment/summary` | 汇总 `{count, amount}`（可传 month） |
| GET | `/api/fee-payment/list` | 全量列表 |
| GET | `/api/fee-payment/{id}` | 详情 |
| POST | `/api/fee-payment` | 补录缴费（payTime 默认当前时间，payMethod 默认「线下缴纳」） |
| DELETE | `/api/fee-payment/{id}` | 删除 |

### 2.9 Pay 在线支付 `/api/pay`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/pay/order` | 创建支付订单（body: `{feeId, channel}`，channel 默认 `mock`）。员工只能支付本人房间账单；管理员可代任意账单下单。返回 `{orderNo, amount, month, status, expireTime, payParams}` |
| POST | `/api/pay/confirm` | 确认支付（模拟渠道回调，body: `{orderNo, outTradeNo?}`）。幂等：重复回调直接返回既有成功订单 |
| POST | `/api/pay/close` | 关闭订单（body: `{orderNo, reason?}`） |
| GET | `/api/pay/order/{orderNo}` | 订单详情（含关联账单的 `feePaid` / `total`） |
| GET | `/api/pay/my` | 当前员工最近 50 条订单 |
| GET | `/api/pay/page` | 管理员：全部订单分页（筛选：status / channel） |

**订单状态机**：`PENDING` →（confirm）→ `SUCCESS`；`PENDING` →（close / 超时）→ `CLOSED`
**有效期**：30 分钟（下单时写入 `expire_time`，过期订单在下单/确认时自动关闭）
**账单核销**：`confirm` 成功后 —— 账单 `paid=1` + 写一条 `fee_payment`（payMethod = `在线支付`，remark 含订单号）+ 给房间在住员工发 `fee_paid` 通知

> 返回值约定：`POST /api/pay/order`、`POST /api/pay/confirm`、`GET /api/pay/order/{orderNo}` 返回订单对象；`POST /api/pay/close` 返回布尔值。员工访问 `/api/pay/page`（管理端）会被拒绝。同一账单在待支付期间重复下单会复用原订单，不会重复建单。

### 2.10 Repair 报修 `/api/repair`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/repair/page` | 分页 |
| GET | `/api/repair/my` | 当前员工的报修分页 |
| GET | `/api/repair/list` | 全量列表 |
| GET | `/api/repair/{id}` | 详情 |
| POST | `/api/repair` | 新增报修（员工提交时按登录身份回填 `employeeId`，强制 `status=0` 并清空 `handler`/`handleRemark`；`reportTime` 为空取服务端当前时间） |
| PUT | `/api/repair` | 更新（状态改为 1 发「已接单」通知，2 发「维修完成」通知） |
| DELETE | `/api/repair/{id}` | 删除 |

> 员工端新增报修只提交 `roomId`/`title`/`description`：报修人由服务端按 token 解析，状态固定为 0（待处理），处理人/处理说明由管理员分配时填写。管理员提交不受此约束。

### 2.11 Visitor 访客 `/api/visitor`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/visitor/page` | 分页 |
| GET | `/api/visitor/my` | 当前员工相关的访客分页 |
| GET | `/api/visitor/list` | 全量列表 |
| GET | `/api/visitor/{id}` | 详情 |
| POST | `/api/visitor` | 新增登记 |
| PUT | `/api/visitor` | 更新（如填写离开时间） |
| DELETE | `/api/visitor/{id}` | 删除 |

### 2.12 Announcement 公告 `/api/announcement`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/announcement/page` | 分页 |
| GET | `/api/announcement/latest` | 最新公告（取前几条） |
| GET | `/api/announcement/{id}` | 详情 |
| POST | `/api/announcement` | 新增 |
| PUT | `/api/announcement` | 修改 |
| DELETE | `/api/announcement/{id}` | 删除 |

### 2.13 ChangeRoom 调宿 `/api/change-room`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/change-room/page` | 分页（联表富化 employeeName / currentRoom / targetRoomType） |
| POST | `/api/change-room` | 员工提交调宿申请 |
| PUT | `/api/change-room/{id}/approve` | 管理员审批通过（发换寝结果通知） |
| PUT | `/api/change-room/{id}/reject` | 管理员驳回 |

### 2.14 MoveOut 退宿 `/api/move-out`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/move-out/page` | 分页（联表富化 employeeName / room / moveOutDate） |
| POST | `/api/move-out` | 员工提交退宿申请 |
| PUT | `/api/move-out/{id}/approve` | 管理员审批通过 |
| PUT | `/api/move-out/{id}/reject` | 管理员驳回 |

### 2.15 Notification 消息通知 `/api/notification`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/notification/my` | 当前员工的通知分页（筛选 isRead） |
| GET | `/api/notification/unread-count` | 未读数（管理员返回 0，不报错） |
| GET | `/api/notification/list` | 全量列表 |
| PUT | `/api/notification/read-all` | 全部标记已读 |
| PUT | `/api/notification/{id}/read` | 标记单条已读 |
| PUT | `/api/notification/{id}/unread` | 标记单条未读 |
| DELETE | `/api/notification/{id}` | 删除 |

**通知类型 type**：`repair_accept` 报修接单 / `repair_finish` 维修完成 / `fee_warning` 水电超限 / `fee_urge` 缴费催缴 / `fee_paid` 缴费成功 / `change_room_result` 换寝审批结果

### 2.16 SystemConfig 系统参数 `/api/system-config`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/system-config/list` | 配置列表（返回 key/value/name/unit 结构） |
| GET | `/api/system-config` | 同上（无路径后缀） |
| PUT | `/api/system-config` | 更新配置 |

**内置参数**：`water_price`(水价)、`elec_price`(电价)、`default_deposit`(默认押金)、`utility_threshold`(水电费预警阈值，默认 200)

### 2.17 User 用户管理 `/api/user`（管理员）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/user/page` | 分页（筛选：keyword 用户名/姓名/手机号、role、status；返回前置空 password） |
| GET | `/api/user/{id}` | 详情 |
| POST | `/api/user` | 新增账号（用户名唯一；空密码默认 `123456` 并 BCrypt 加密） |
| PUT | `/api/user` | 修改账号（空密码表示不修改） |
| DELETE | `/api/user/{id}` | 删除（内置 `admin` 不可删） |
| PUT | `/api/user/{id}/reset-password` | 重置密码（body `{password}`，缺省 `123456`） |
| PUT | `/api/user/{id}/status` | 启用/禁用（body `{status}`，内置 `admin` 不可禁用） |
| POST | `/api/user/import` | 批量导入（body 为账号对象数组，返回 `{total, success, fail, errors[]}`） |

---

## 三、典型调用链

```
登录        POST /api/auth/login              → token
管理员首页  GET  /api/dashboard/stats
员工首页    GET  /api/employee/my-allocation + GET /api/fee/my + GET /api/repair/my + GET /api/visitor/my
员工缴费    GET  /api/fee/my → POST /api/pay/order → POST /api/pay/confirm → GET /api/pay/my
账单核销    PUT  /api/fee/{id}/paid（或在线支付 confirm）→ 自动写 fee_payment + 发通知
```
