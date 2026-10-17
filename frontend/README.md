# 驾校学员培训与考试管理系统 · 前端

Vue 3 + Vite + Pinia + Vue Router + Element Plus + ECharts，对接 `backend` 的 Spring Boot 接口。

## 目录结构

```
src/
├── api/            后端接口封装（index.js 按模块分组）
├── assets/main.css 全局主题（黑灰白 + 边框式卡片 + 深色主题）
├── components/     TopBar(导航栏) / BaseChart(ECharts 容器) / StatCard / PageHeader / AuthShell
├── composables/    useTheme（深浅色切换）
├── config/roles.js 角色与导航配置（单一事实来源）
├── data/demo.js    仅保留枚举与科目常量（业务数据走接口）
├── router/         路由表 + 角色守卫
├── stores/         auth（Session 登录态）、data（业务数据缓存 + load/写操作）
├── utils/          request(axios 封装) / chart(ECharts 主题) / status(状态色标)
└── views/          student / coach / admin 三端页面
```

## 启动

后端先跑起来（默认 8080；若被占用用 `--server.port=8088`）：

```bash
cd ../backend && java -jar target/backend-1.0-SNAPSHOT.jar --server.port=8088
```

前端：

```bash
# 默认代理到 127.0.0.1:8080
npm run dev

# 后端不在 8080 时，用环境变量指定
VITE_API_TARGET=http://127.0.0.1:8088 npm run dev

# 生产构建
npm run build
```

## 演示账号（密码均为 `123456`）

| 账号 | 角色 | 登录后落地页 |
| --- | --- | --- |
| `student` | 学员 | 我的培训进度（首页） |
| `coach` | 教练 | 教练工作台 |
| `admin` | 管理员 | 管理端工作台 |

## 角色与导航

- 首页 `/` 恒定落在学员端「我的培训进度」，游客也可查看（游客视角提示登录）。
- 顶部导航栏**只显示当前角色所属的那一组菜单**：
  - 游客 → 学员端 4 项
  - 学员/教练/管理员 → 各自角色菜单
- 访问越权路由由 `router/index.js` 守卫拦截：
  - 未登录 → 跳登录页并携带 `redirect`
  - 已登录但角色不符 → 提示后回到本人首页

## 数据流

```
页面 onMounted → stores/data 的 loadXxx()
                 → api/index.js
                 → utils/request.js（axios，withCredentials 携带 JSESSIONID）
                 → 后端 /api/**
```

- 登录态由后端 Session 承载，前端只缓存用户信息用于渲染；`main.js` 启动时先 `GET /auth/me` 恢复会话再挂载路由。
- 任意接口返回 401 时，`request.js` 会触发统一处理器：清除本地登录态并跳登录页。
- 写操作统一返回 `{ ok, reason }`，页面据此 `ElMessage` 提示。

## 验证

```bash
# 界面/角色/图表/配色回归（需先 npm run build && npx vite preview）
npm run verify

# 端到端（需后端 + dev server 同时运行）
node scripts/verify-e2e.mjs
```

## 主题

- 主背景纯白，卡片用 1px 边框区分，无阴影/毛玻璃。
- 支持深浅色切换（顶部按钮，偏好存 `localStorage`）。
- ECharts 统一走 `utils/chart.js` 的灰阶色板，随主题切换自动重绘。
