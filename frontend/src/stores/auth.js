import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { DEMO_ACCOUNTS, ROLES } from '@/config/roles'

/**
 * 登录态 Store
 * ---------------------------------------------------------------------------
 * 现阶段为「前端先行」：账号校验在前端完成（DEMO_ACCOUNTS），
 * 登录结果持久化到 localStorage，刷新页面不丢登录态。
 *
 * 后端 AuthController（/api/auth/**）已就绪，后续接入真实接口时，
 * 只需把 login / logout / fetchCurrentUser 三个函数的实现替换为
 * `@/api/auth` 中的请求即可（loginApi / logoutApi / getMeApi），
 * 其余页面与守卫代码无需改动。
 */

const STORAGE_KEY = 'dsms.auth.user'

function readStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    // 角色必须是合法角色，否则视为无效登录态
    return parsed && ROLES[parsed.role] ? parsed : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(readStored())
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(user.value))
  const role = computed(() => user.value?.role ?? 'guest')
  const roleName = computed(() => ROLES[role.value]?.name ?? '访客')
  const isAdmin = computed(() => role.value === 'admin')
  const displayName = computed(() => user.value?.name || user.value?.username || '未登录')
  const initial = computed(() => displayName.value.charAt(0))

  function persist(next) {
    user.value = next
    if (next) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  /**
   * 登录：匹配演示账号（后端接入后改为调用 loginApi）。
   * 校验失败抛出 Error，由页面 catch 后用 ElMessage 提示。
   */
  async function login({ username, password }) {
    loading.value = true
    try {
      await new Promise((resolve) => setTimeout(resolve, 320)) // 模拟请求耗时

      const account = DEMO_ACCOUNTS.find(
        (item) => item.username === username.trim() && item.password === password,
      )

      if (!account) {
        throw new Error('用户名或密码错误')
      }

      persist({
        username: account.username,
        name: account.name,
        role: account.role,
        title: account.title,
        loginAt: new Date().toISOString(),
      })
      return user.value
    } finally {
      loading.value = false
    }
  }

  /** 自助注册：统一注册为学员角色（需求 F01） */
  async function register(payload) {
    loading.value = true
    try {
      await new Promise((resolve) => setTimeout(resolve, 320))
      const exists = DEMO_ACCOUNTS.some((item) => item.username === payload.username)
      if (exists) {
        throw new Error('该用户名已被占用，请更换')
      }
      DEMO_ACCOUNTS.push({
        username: payload.username,
        password: payload.password,
        role: 'student',
        name: payload.name,
        title: `学员 · ${payload.licenseType || 'C1'} 待审核`,
      })
      return { username: payload.username }
    } finally {
      loading.value = false
    }
  }

  /** 修改密码（个人中心 F01）—— 前端先行阶段同步演示账号口令 */
  async function changePassword({ oldPassword, newPassword }) {
    const account = DEMO_ACCOUNTS.find((item) => item.username === user.value?.username)
    if (!account) throw new Error('演示账号不存在，无法修改密码')
    if (account.password !== oldPassword) throw new Error('原密码不正确')
    account.password = newPassword
    return true
  }

  function logout() {
    persist(null)
  }

  /** 刷新页面后恢复登录态（读本地存储） */
  function fetchCurrentUser() {
    user.value = readStored()
    return user.value
  }

  return {
    user,
    loading,
    isAuthenticated,
    role,
    roleName,
    isAdmin,
    displayName,
    initial,
    login,
    register,
    changePassword,
    logout,
    fetchCurrentUser,
  }
})
