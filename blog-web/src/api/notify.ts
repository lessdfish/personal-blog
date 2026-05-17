import http from './http'
import type { NotifyDetail, NotifyItem, PageResult } from '../types/api'

/**
 * 获取通知分页：加载当前用户的站内通知列表。
 */
export function getNotifyPage(pageNum = 1, pageSize = 10) {
  return http.post('/notify/page', { pageNum, pageSize }).then((res) => res.data.data as PageResult<NotifyItem>)
}

/**
 * 获取未读通知数：用于导航栏角标。
 */
export function getNotifyUnreadCount() {
  return http.get('/notify/unread/count').then((res) => res.data.data as number)
}

/**
 * 获取通知详情：查看单条通知的完整内容。
 */
export function getNotifyDetail(id: number) {
  return http.get(`/notify/${id}`).then((res) => res.data.data as NotifyDetail)
}

/**
 * 标记通知已读：用户点开通知后同步后端状态。
 */
export function markNotifyRead(id: number) {
  return http.put(`/notify/read/${id}`).then((res) => res.data.data)
}

/**
 * 全部标记已读：一键清空未读通知角标。
 */
export function markAllNotifyRead() {
  return http.put('/notify/read/all').then((res) => res.data.data)
}
