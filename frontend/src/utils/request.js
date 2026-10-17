import axios from 'axios'

// 全局唯一的 axios 实例：统一后端地址、超时、以及 Session Cookie 的携带。
const request = axios.create({
  baseURL: '/api', // 开发环境由 Vite 代理到后端（见 vite.config.js）
  timeout: 15000,
  withCredentials: true, // 关键：携带 JSESSIONID，后端 Session 才能识别登录态
})

request.interceptors.request.use((config) => config)

/**
 * 响应拦截器：后端统一返回 { code, message, data }，这里统一拆包。
 * - code === 200：返回 data
 * - 其它：抛出 Error(message)，由调用方 catch 后用 ElMessage 提示
 *
 * 401 特殊处理：会话过期时清除本地登录态并跳登录页（避免停留在空白页）。
 */
let onUnauthorized = null

/** 由 main.js 注入，避免 request 与 router/store 循环依赖 */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 兼容非统一结构（如文件流）直接返回
    if (!body || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === 200) {
      return body.data
    }
    if (body.code === 401 && onUnauthorized) onUnauthorized()
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message =
      error.response?.data?.message ||
      (status === 404 ? '接口不存在，请确认后端已启动' : error.message) ||
      '网络异常，请稍后重试'

    if (status === 401 && onUnauthorized) onUnauthorized()
    return Promise.reject(new Error(message))
  },
)

export default request
