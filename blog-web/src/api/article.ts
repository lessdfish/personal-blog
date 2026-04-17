import http from './http'
import type { ArticleDetail, ArticleItem, PageVO } from '../types/api'

export function getArticlePage(params: Record<string, unknown>) {
  return http.get('/article/page/normal', { params }).then((res) => res.data.data as PageVO<ArticleItem>)
}

export function getHotArticlePage(params: { pageNum: number; pageSize: number }) {
  return http.get('/article/page/hot', { params }).then((res) => res.data.data as PageVO<ArticleItem>)
}

export function getHotArticles(limit = 10) {
  return http.get('/article/hot', { params: { limit } }).then((res) => res.data.data as ArticleItem[])
}

export function getArticleDetail(id: number | string) {
  return http.get(`/article/detail/${id}`).then((res) => res.data.data as ArticleDetail)
}

export function publishArticle(payload: {
  title: string
  summary?: string
  content: string
  boardId?: number
  tags?: string
}) {
  return http.post('/article/publish', payload).then((res) => res.data.data as string)
}

export function likeArticle(articleId: number) {
  return http.put(`/article/like/${articleId}`).then((res) => res.data.data as boolean)
}

export function unlikeArticle(articleId: number) {
  return http.delete(`/article/like/${articleId}`).then((res) => res.data.data as boolean)
}

export function favoriteArticle(articleId: number) {
  return http.put(`/article/favorite/${articleId}`).then((res) => res.data.data as boolean)
}

export function unfavoriteArticle(articleId: number) {
  return http.delete(`/article/favorite/${articleId}`).then((res) => res.data.data as boolean)
}

export function hasLikedArticle(articleId: number) {
  return http.get(`/article/liked/${articleId}`).then((res) => res.data.data as boolean)
}

export function hasFavoritedArticle(articleId: number) {
  return http.get(`/article/favorited/${articleId}`).then((res) => res.data.data as boolean)
}
