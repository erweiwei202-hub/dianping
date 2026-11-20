# 智享生活前端

消费者 H5 与运营管理 Web 的 Vue 3 + TypeScript + Vite 前端。

## 本地运行

在本目录执行 `npm install`，随后执行 `npm run dev`。

Vite 将 `/api` 请求代理到 `http://localhost:8080`，可用 `VITE_API_PROXY_TARGET` 覆盖。当前首页与运营台以预览数据展示；登录、评价、点赞等页面在后端接口接入后启用真实数据。

## 构建

执行 `npm run build`。
