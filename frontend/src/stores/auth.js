import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginApi, registerApi, logoutApi, getMeApi } from '@/api/auth'

// 登录态由后端 Session（Cookie 中的 JSESSIONID）承载，
// 具体请求由 @/utils/request（axios 实例）发出，见 @/api/auth。

const ROLES = {
  admin: '管理员',
  coach: '教练',
  student: '学员',
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  const isAuthenticated = computed(() => Boolean(user.value))
  const role = computed(() => user.value?.role ?? 'guest')
  const roleName = computed(() => ROLES[role.value] ?? '访客')
  const isAdmin = computed(() => role.value === 'admin')

  async function login({ username, password }) {
    user.value = await loginApi({ username, password })
    return user.value
  }

  async function register(payload) {
    return registerApi(payload)
  }

  function logout() {
    user.value = null // 前端先退出，界面立即响应
    return logoutApi().catch(() => {}) // 再通知后端销毁 Session
  }

  // 刷新页面后，用后端 Session 恢复登录态（未登录则置空）
  async function fetchCurrentUser() {
    try {
      user.value = await getMeApi()
    } catch {
      user.value = null
    }
    return user.value
  }

  return {
    user,
    isAuthenticated,
    role,
    roleName,
    isAdmin,
    login,
    register,
    logout,
    fetchCurrentUser,
  }
})
