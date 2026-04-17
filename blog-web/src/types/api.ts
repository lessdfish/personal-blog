export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface ApiError extends Error {
  code?: number
  status?: number
  responseMessage?: string
}

export interface PageVO<T> {
  total: number
  list: T[]
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export interface CurrentUser {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  sessionInfo?: SessionInfo
}

export interface SessionInfo {
  loginIp?: string
  location?: string
  device?: string
  browser?: string
  userAgent?: string
  loginTime?: string
}

export interface LoginUser {
  username: string
  nickname: string
  avatar?: string
}

export interface LoginResponse {
  user: LoginUser
}

export interface ArticleItem {
  id: number
  title: string
  summary: string
  boardName?: string
  tags?: string
  likeCount: number
  commentCount: number
  viewCount: number
  favoriteCount?: number
  heatScore?: number
  authorId?: number
  isTop?: number
  isEssence?: number
  createTime?: string
}

export interface ArticleDetail extends ArticleItem {
  content: string
  allowComment?: number
}

export interface CommentItem {
  id: number
  articleId: number
  parentId?: number | null
  userId: number
  userName?: string
  userAvatar?: string
  notifyUserId?: number
  notifyUserName?: string
  content: string
  createTime?: string
  children?: CommentItem[]
}

export interface NotifyItem {
  id: number
  type: number
  title: string
  articleId?: number
  commentId?: number
  isRead?: number
  createTime?: string
}

export interface NotifyDetail extends NotifyItem {
  content: string
  senderId?: number
}
