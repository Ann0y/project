import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import 'element-plus/dist/index.css'
import './assets/main.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { setUnauthorizedHandler } from './utils/request'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ElementPlus, { locale: zhCn })

/**
 * Session 过期（任意接口返回 401）时的统一处理：
 * 清除前端登录态并跳登录页，避免页面停留在报错状态。
 */
setUnauthorizedHandler(() => {
  const auth = useAuthStore(pinia)
  if (!auth.isAuthenticated) return
  auth.user = null
  localStorage.removeItem('dsms.auth.user')
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
})

/**
 * 关键顺序：
 * 1. 先向后端确认 Session 是否有效（GET /api/auth/me）；
 * 2. 再注册路由（app.use(router) 会立刻触发首次导航），
 *    这样首次导航时守卫已能读到正确角色，避免误判为游客。
 */
const auth = useAuthStore(pinia)
auth.fetchCurrentUser().finally(() => {
  app.use(router)
  app.mount('#app')
})
