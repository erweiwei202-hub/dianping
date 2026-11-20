# 智享生活平台

基于 Java、Spring Boot、MySQL、Redis、RocketMQ 与 Vue 的生活服务平台。首期包含商户浏览、用户评价与点赞、优惠券秒杀、订单与余额支付扩展。

## 项目结构

- `backend/`：Spring Boot REST API 与异步消费者
- `frontend/`：消费者 H5 与运营管理 Web
- `docs/`：系统设计、API 与开发文档

## 技术基线

- Java 21
- Spring Boot 3.5.x
- Maven
- RocketMQ：消息吞吐验收目标至少 2,000 条/秒
- Vue 3 + TypeScript + Vite

本仓库处于初始开发阶段。密钥、生产连接串和支付凭证不得提交到仓库。充值功能首期关闭，不允许模拟或直接写入真实余额。

## 启动

服务依赖 MySQL、Redis 和 RocketMQ。首次启动前，在仓库根目录复制环境变量模板并启动本地依赖：

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Docker Compose 仅用于本地开发环境。确认 MySQL、Redis 和 RocketMQ 容器就绪后，再运行：

```powershell
mvn -pl backend spring-boot:run
```

前端开发服务器：

```powershell
cd frontend
npm install
npm run dev
```

本地服务默认使用 `.env.example` 中的数据库账号；真实密码、支付密钥和生产配置不得提交到仓库。当前 RocketMQ Compose 为单节点开发配置，不代表生产高可用或 2,000 QPS 压测验收结果。

健康检查：`GET /api/health`。

需要本地运营管理员时，在启动后端前设置 `APP_BOOTSTRAP_ADMIN_USERNAME` 和至少 10 位的 `APP_BOOTSTRAP_ADMIN_PASSWORD` 环境变量。首次启动会创建该管理员；密码不要写入仓库或提交到 `.env.example`。

详细架构及业务规则见 [`docs/智享生活平台-系统设计文档.md`](docs/智享生活平台-系统设计文档.md)。
