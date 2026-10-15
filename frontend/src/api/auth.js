import request from '@/utils/request'

// 认证相关接口（对应后端 AuthController，/api 前缀已由 request 统一处理）
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
