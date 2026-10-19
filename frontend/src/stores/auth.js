import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginApi, registerApi, logoutApi, getMeApi, changePasswordApi } from '@/api'
import { ROLES } from '@/config/roles'

/**
 * 登录态 Store ： 对接后端 Session（JSESSIONID）
 * ---------------------------------------------------------------------------
 * 登录态由后端 Session 承载，本 store 只缓存当前用户信息用于渲染。
 * 刷新页面时通过 GET /api/auth/me 恢复（main.js 中 await 后再挂载路由）。
 */

const STORAGE_KEY = 'dsms.auth.user'

function readStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
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
  const isStudent = computed(() => role.value === 'student')
  const isCoach = computed(() => role.value === 'coach')
  const isAdmin = computed(() => role.value === 'admin')
  const displayName = computed(() => user.value?.name || user.value?.username || '未登录')
  const initial = computed(() => displayName.value.charAt(0))

  /** 学员档案 id / 教练档案 id，接口调用时会用到 */
  const studentId = computed(() => user.value?.studentId ?? null)
  const coachId = computed(() => user.value?.coachId ?? null)

  function persist(next) {
    user.value = next
    if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    else localStorage.removeItem(STORAGE_KEY)
  }

  /** 登录：成功后后端写入 Session，前端缓存用户信息 */
  async function login({ username, password }) {
    loading.value = true
    try {
      const u = await loginApi({ username: username.trim(), password })
      persist(u)
      return u
    } finally {
      loading.value = false
    }
  }

  /** 自助注册（学员） */
  async function register(payload) {
    loading.value = true
    try {
      return await registerApi(payload)
    } finally {
      loading.value = false
    }
  }

  /** 修改本人密码 */
  async function changePassword({ oldPassword, newPassword }) {
    await changePasswordApi({ oldPassword, newPassword })
    return true
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常，前端一律清除本地态
    }
    persist(null)
  }

  /**
   * 恢复登录态：向后端确认 Session 是否仍有效。
   * 返回 null 表示未登录（或会话已过期）。
   */
  async function fetchCurrentUser() {
    try {
      const u = await getMeApi()
      persist(u)
      return u
    } catch {
      persist(null)
      return null
    }
  }

  return {
    user,
    loading,
    isAuthenticated,
    role,
    roleName,
    isStudent,
    isCoach,
    isAdmin,
    displayName,
    initial,
    studentId,
    coachId,
    login,
    register,
    changePassword,
    logout,
    fetchCurrentUser,
  }
})
