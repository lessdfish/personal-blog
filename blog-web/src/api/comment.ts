import http from './http'
import type { CommentItem, PageResult } from '../types/api'

export function getCommentPage(articleId: number, pageNum = 1, pageSize = 10) {
  return http.post('/comment/page', { articleId, pageNum, pageSize }).then((res) => res.data.data as PageResult<CommentItem>)
}

export function createComment(payload: { articleId: number; parentId?: number; content: string }) {
  return http.post('/comment', payload).then((res) => res.data.data as number)
}
