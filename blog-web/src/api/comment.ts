import http from './http'
import type { CommentItem, PageResult } from '../types/api'

/**
 * 获取评论分页：按文章 id 加载评论列表。
 */
export function getCommentPage(articleId: number, pageNum = 1, pageSize = 10) {
  return http.post('/comment/page', { articleId, pageNum, pageSize }).then((res) => res.data.data as PageResult<CommentItem>)
}

/**
 * 发表评论：提交文章 id、父评论 id 和评论内容。
 */
export function createComment(payload: { articleId: number; parentId?: number; content: string }) {
  return http.post('/comment', payload).then((res) => res.data.data as number)
}
