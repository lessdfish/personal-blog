import http from './http'
import type { ArticleDetail, ArticleItem, PageVO } from '../types/api'

/**
 * 获取文章分页列表：按查询条件加载首页文章数据。
 */
export function getArticlePage(params: Record<string, unknown>) {
  return http.get('/article/page/normal', { params }).then((res) => res.data.data as PageVO<ArticleItem>)
}

/**
 * 获取热榜分页数据：加载热度排序的文章列表。
 */
export function getHotArticlePage(params: { pageNum: number; pageSize: number }) {
  return http.get('/article/page/hot', { params }).then((res) => res.data.data as PageVO<ArticleItem>)
}

/**
 * 获取热门文章：通常用于首页侧边栏或推荐区域。
 */
export function getHotArticles(limit = 10) {
  return http.get('/article/hot', { params: { limit } }).then((res) => res.data.data as ArticleItem[])
}

/**
 * 获取文章详情：按文章 id 加载正文、统计数据和互动状态。
 */
export function getArticleDetail(id: number | string) {
  return http.get(`/article/detail/${id}`).then((res) => res.data.data as ArticleDetail)
}

/**
 * 发布文章：把标题、正文、版块等内容提交给后端。
 */
export function publishArticle(payload: {
  title: string
  summary?: string
  content: string
  boardId?: number
  tags?: string
}) {
  return http.post('/article/publish', payload).then((res) => res.data.data as string)
}

/**
 * 点赞文章：把当前用户对文章的状态改为已点赞。
 */
export function likeArticle(articleId: number) {
  return http.put(`/article/like/${articleId}`).then((res) => res.data.data as boolean)
}

/**
 * 取消点赞文章：把当前用户对文章的状态改为未点赞。
 */
export function unlikeArticle(articleId: number) {
  return http.delete(`/article/like/${articleId}`).then((res) => res.data.data as boolean)
}

/**
 * 收藏文章：把当前用户对文章的状态改为已收藏。
 */
export function favoriteArticle(articleId: number) {
  return http.put(`/article/favorite/${articleId}`).then((res) => res.data.data as boolean)
}

/**
 * 取消收藏文章：把当前用户对文章的状态改为未收藏。
 */
export function unfavoriteArticle(articleId: number) {
  return http.delete(`/article/favorite/${articleId}`).then((res) => res.data.data as boolean)
}

/**
 * 查询是否已点赞：刷新详情页按钮状态。
 */
export function hasLikedArticle(articleId: number) {
  return http.get(`/article/liked/${articleId}`).then((res) => res.data.data as boolean)
}

/**
 * 查询是否已收藏：刷新详情页收藏按钮状态。
 */
export function hasFavoritedArticle(articleId: number) {
  return http.get(`/article/favorited/${articleId}`).then((res) => res.data.data as boolean)
}
