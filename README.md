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

服务依赖 MySQL、Redis 和 RocketMQ。准备本地依赖服务后，在仓库根目录运行：

```powershell
mvn -pl backend spring-boot:run
```

健康检查：`GET /api/health`。

详细架构及业务规则见 [`docs/智享生活平台-系统设计文档.md`](docs/智享生活平台-系统设计文档.md)。
