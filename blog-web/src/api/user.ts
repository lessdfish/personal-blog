import http from './http'
import type { CurrentUser, LoginResponse } from '../types/api'

export function login(payload: { username: string; password: string }) {
  return http.post('/user/login', payload).then((res) => res.data.data as LoginResponse)
}

export function register(payload: { username: string; password: string; nickname: string; email: string; phone: string }) {
  return http.post('/user/register', payload).then((res) => res.data.data as string)
}

export function checkUserFieldAvailability(field: 'username' | 'nickname' | 'email' | 'phone', value: string) {
  return http.get('/user/check', { params: { field, value } }).then((res) => res.data.data as boolean)
}

export function getCurrentUser() {
  return http.get('/user/me').then((res) => res.data.data as CurrentUser)
}

export function logout() {
  return http.post('/user/logout').then((res) => res.data.data as string)
}

export function updateCurrentUser(payload: { nickname?: string; avatar?: string; email?: string; phone?: string }) {
  return http.put('/user/info', payload).then((res) => res.data.data as string)
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/user/avatar/upload', formData).then((res) => res.data.data as string)
}

export function resetPasswordByPhone(payload: { username: string; phone: string; newPassword: string }) {
  return http.post('/user/password/reset', payload).then((res) => res.data.data as string)
}
