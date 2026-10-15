import axios from 'axios'

// 全局唯一的 axios 实例：统一后端地址、超时、以及 Session Cookie 的携带。
const request = axios.create({
  baseURL: '/api', // 开发环境由 Vite 代理到后端（见 vite.config.js）
  timeout: 10000,
  withCredentials: true, // 关键：携带 JSESSIONID，后端 Session 才能识别登录态
})

// 请求拦截器：如后续需要统一加请求头，可在此处理（当前 Session 方案无需 token）。
request.interceptors.request.use((config) => config)

// 响应拦截器：后端统一返回 { code, message, data }，这里统一拆包。
// - code === 200：直接返回 data；
// - 其它：抛出 Error(message)，由调用方 catch（配合 ElMessage 提示）。
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
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const message =
      error.response?.data?.message || error.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

export default request
