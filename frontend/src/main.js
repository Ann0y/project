import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import 'element-plus/dist/index.css'
import './assets/main.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useTheme } from './composables/useTheme'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus, { locale: zhCn })

// 初始化主题（读取本地偏好 / 系统偏好）
useTheme()

/**
 * 关键顺序：
 * 1. 先恢复登录态（同步读取本地存储），此时 Pinia 已就绪；
 * 2. 再注册路由（app.use(router) 会立刻触发首次导航），
 *    这样首次导航时守卫已能读到正确角色，避免误判为游客。
 */
useAuthStore(pinia).fetchCurrentUser()
app.use(router)
app.mount('#app')
