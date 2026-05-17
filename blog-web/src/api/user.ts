import http from './http'
import type { CurrentUser, LoginResponse } from '../types/api'

/**
 * 调用登录接口：提交用户名和密码，换取后端返回的登录信息。
 */
export function login(payload: { username: string; password: string }) {
  return http.post('/user/login', payload).then((res) => res.data.data as LoginResponse)
}

/**
 * 调用注册接口：把用户填写的注册信息提交给后端。
 */
export function register(payload: { username: string; password: string; nickname: string; email: string; phone: string }) {
  return http.post('/user/register', payload).then((res) => res.data.data as string)
}

/**
 * 检查注册字段是否可用：确认用户名、昵称、邮箱或手机号是否已被占用。
 */
export function checkUserFieldAvailability(field: 'username' | 'nickname' | 'email' | 'phone', value: string) {
  return http.get('/user/check', { params: { field, value } }).then((res) => res.data.data as boolean)
}

/**
 * 获取当前登录用户信息：用于页面刷新后恢复登录状态。
 */
export function getCurrentUser() {
  return http.get('/user/me').then((res) => res.data.data as CurrentUser)
}

/**
 * 调用退出登录接口：让后端清理当前登录状态。
 */
export function logout() {
  return http.post('/user/logout').then((res) => res.data.data as string)
}

/**
 * 调用更新资料接口：保存用户修改后的个人资料。
 */
export function updateCurrentUser(payload: { nickname?: string; avatar?: string; email?: string; phone?: string }) {
  return http.put('/user/info', payload).then((res) => res.data.data as string)
}

/**
 * 上传头像文件：把本地选择的图片通过表单提交给后端。
 */
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/user/avatar/upload', formData).then((res) => res.data.data as string)
}

/**
 * 调用手机号重置密码接口：用于忘记密码场景。
 */
export function resetPasswordByPhone(payload: { username: string; phone: string; newPassword: string }) {
  return http.post('/user/password/reset', payload).then((res) => res.data.data as string)
}
