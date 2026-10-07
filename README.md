# 员工宿舍管理系统 (Employee Dormitory Management System)

Spring Boot 3.4 + Vue 3 + MySQL 8.0 前后端分离全栈项目。

> 最后校准：2026-09-23（与当前代码一致）

## 功能模块

**管理端（admin）**
- 仪表盘统计（楼栋/房间/空房/人数/报修/欠费/访客）
- 宿舍楼管理、房间管理（楼栋/楼层/状态筛选）
- 员工管理（关键字/部门/性别/状态多级筛选）
- 入住管理、调宿管理、退宿管理
- 水电费管理（筛选、批量标记、自动算费、一键催缴、导出 Excel）
- 缴费流水（补录、收据、导出）、费用报表（月度趋势 + 楼栋/楼层汇总）
- 在线支付订单管理（代付、订单查询）
- 报修管理、访客管理、公告管理
- 用户管理（账号 CRUD、重置密码、启用/禁用、Excel 批量导入）
- 系统参数（水价/电价/押金/预警阈值）

**员工端（employee）**
- 我的概览、我的信息、入住信息
- 在线缴费（账单在线支付 + 订单记录）
- 我的水电费（只读、可导出）、报修、访客登记
- 消息通知（报修接单/维修完成/水电超限/缴费催缴/缴费成功/换寝审批结果，已读未读）

## 目录结构

```
dorm/
├── backend/       # Spring Boot 后端（端口 8080）
├── frontend/      # Vue 3 + Vite 前端（端口 5173）
├── database/      # init.sql / notification.sql / fee_payment.sql / payment.sql
├── docs/          # api.md / design.md / 需求规格说明书.md / 启动运行指南.md
├── docker/        # Dockerfile-backend / Dockerfile-frontend / docker-compose.yml
├── 一键启动.md          # 一键启动说明（脚本用法 / 账号 / 排错）
├── start-all.ps1        # 一键启动（MySQL + 后端 + 前端）
├── start-backend.ps1    # 仅启动后端
├── start-frontend.ps1   # 仅启动前端
└── stop-all.ps1         # 停止后端与前端
```

## 运行步骤

环境要求：JDK 21、Maven 3.9+、Node 18+、MySQL 8.0

1. 启动 MySQL，建库 `dorm`，按顺序执行 `database/` 下 4 个脚本：
   ```
   init.sql  →  notification.sql  →  fee_payment.sql  →  payment.sql
   ```
2. 启动后端（或直接跑 `start-backend.ps1`）：
   ```
   cd backend
   mvn spring-boot:run
   ```
3. 启动前端（或直接跑 `start-frontend.ps1`）：
   ```
   cd frontend
   npm install
   npm run dev
   ```
4. 浏览器打开 http://localhost:5173

Windows 一键启动：右键 `start-all.ps1` → 使用 PowerShell 运行，或：
```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```
详细说明见 `docs/启动运行指南.md`。

## 默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | admin | admin123 |
| 员工 | zhangwei / lina / wangfang | employee123 |

## 接口文档

- Swagger UI：http://localhost:8080/doc.html
- 手工整理的完整接口清单：`docs/api.md`
