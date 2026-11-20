# 智享生活前端

Vue 3 + TypeScript + Vite，包含消费者 H5 与运营管理 Web 入口。

## 本地启动

```powershell
npm ci
npm run dev
```

Vite 将 `/api` 代理到 `http://localhost:8080`。可通过 `VITE_API_PROXY_TARGET` 覆盖目标地址。

## 已接入页面

- 消费者：服务端商户查询、评价列表、注册/登录、发布/编辑/删除评价、评价点赞/取消点赞。
- 活动与支付区尚未接入 API，页面会标为预览内容，不提交秒杀或支付请求。
- `/admin` 目前是运营台界面预览，活动写入与审核操作尚未接入 API。

认证采用服务端 Session Cookie；写请求先获取 `/api/auth/csrf`，携带 `X-XSRF-TOKEN`。所有会话请求都使用 `credentials: include`。
