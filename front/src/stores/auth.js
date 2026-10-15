import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

// 说明：当前为纯前端 mock 实现，账号与登录态保存在 localStorage，
// 仅用于打通登录与权限流程。接入后端时，把 login / register 换成对
// RESTful 接口的调用（JWT 由后端返回并写入 localStorage），
// role 由后端在登录响应中给出即可，页面与路由守卫无需改动。
const ACCOUNTS_KEY = 'driver-school-accounts'
const SESSION_KEY = 'driver-school-session'

const ROLES = {
  admin: '管理员',
  student: '学员',
}

function delay(ms = 400) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// 兼容早期未写入 role 的本地数据：admin 账号视为管理员，其余视为学员
function normalizeAccount(account) {
  if (account.role) return account
  return { ...account, role: account.username === 'admin' ? 'admin' : 'student' }
}

function loadAccounts() {
  try {
    const raw = localStorage.getItem(ACCOUNTS_KEY)
    if (raw) return JSON.parse(raw).map(normalizeAccount)
  } catch {
    // 数据损坏时回退到内置演示账号
  }
  return [
    { username: 'admin', password: '123456', name: '张佛亮', phone: '13800000000', role: 'admin' },
    { username: 'student', password: '123456', name: '李小明', phone: '13900000000', role: 'student' },
  ]
}

function loadSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(loadSession())
  const isAuthenticated = computed(() => Boolean(user.value))
  const role = computed(() => user.value?.role ?? 'guest')
  const roleName = computed(() => ROLES[role.value] ?? '访客')
  const isAdmin = computed(() => role.value === 'admin')

  async function login({ username, password }) {
    await delay()
    const account = loadAccounts().find((item) => item.username === username)
    if (!account || account.password !== password) {
      throw new Error('用户名或密码错误')
    }
    user.value = {
      username: account.username,
      name: account.name,
      phone: account.phone,
      role: account.role,
    }
    localStorage.setItem(SESSION_KEY, JSON.stringify(user.value))
    return user.value
  }

  async function register({ username, password, name, phone }) {
    await delay()
    const accounts = loadAccounts()
    if (accounts.some((item) => item.username === username)) {
      throw new Error('该用户名已被注册')
    }
    // 自助注册的账号默认为学员角色
    accounts.push({ username, password, name, phone, role: 'student' })
    localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts))
    return { username, name, phone, role: 'student' }
  }

  function logout() {
    user.value = null
    localStorage.removeItem(SESSION_KEY)
  }

  return { user, isAuthenticated, role, roleName, isAdmin, login, register, logout }
})
