import request from '@/utils/request'

/**
 * 认证相关接口（对应后端 AuthController，/api 前缀已由 request 统一处理）
 * ---------------------------------------------------------------------------
 * ⚠️ 前端先行阶段：登录校验在 @/stores/auth 内本地完成，本文件暂未被调用。
 *    后端就绪后，把 stores/auth.js 中 login / register / logout /
 *    fetchCurrentUser 的实现替换为下面这几个请求即可，页面代码无需改动。
 */
export function loginApi(data) {
  return request.post('/auth/login', data)
}

export function registerApi(data) {
  return request.post('/auth/register', data)
}

export function logoutApi() {
  return request.post('/auth/logout')
}

export function getMeApi() {
  return request.get('/auth/me')
}
