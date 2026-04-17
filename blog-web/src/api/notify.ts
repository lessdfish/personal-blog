import http from './http'
import type { NotifyDetail, NotifyItem, PageResult } from '../types/api'

export function getNotifyPage(pageNum = 1, pageSize = 10) {
  return http.post('/notify/page', { pageNum, pageSize }).then((res) => res.data.data as PageResult<NotifyItem>)
}

export function getNotifyUnreadCount() {
  return http.get('/notify/unread/count').then((res) => res.data.data as number)
}

export function getNotifyDetail(id: number) {
  return http.get(`/notify/${id}`).then((res) => res.data.data as NotifyDetail)
}

export function markNotifyRead(id: number) {
  return http.put(`/notify/read/${id}`).then((res) => res.data.data)
}

export function markAllNotifyRead() {
  return http.put('/notify/read/all').then((res) => res.data.data)
}
