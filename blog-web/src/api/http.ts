import axios, { AxiosHeaders } from 'axios'
import type { ApiError, Result } from '../types/api'

const CSRF_COOKIE_NAME = 'BLOG_CSRF_TOKEN'
const CSRF_HEADER_NAME = 'X-CSRF-Token'
const UNSAFE_METHODS = new Set(['post', 'put', 'patch', 'delete'])

const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const method = (config.method || 'get').toLowerCase()
  if (UNSAFE_METHODS.has(method)) {
    const csrfToken = readCookie(CSRF_COOKIE_NAME)
    if (csrfToken) {
      const headers = AxiosHeaders.from(config.headers)
      headers.set(CSRF_HEADER_NAME, csrfToken)
      config.headers = headers
    }
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as Result<unknown>
    if (typeof body?.code === 'number' && body.code !== 200) {
      const error = new Error(body.message || '请求失败') as ApiError
      error.code = body.code
      error.responseMessage = body.message
      return Promise.reject(error)
    }
    return response
  },
  (error) => {
    const apiError = new Error(
      error?.response?.data?.message
      || error?.message
      || '请求失败，请稍后再试',
    ) as ApiError
    apiError.code = error?.response?.data?.code
    apiError.status = error?.response?.status
    apiError.responseMessage = error?.response?.data?.message
    if (typeof window !== 'undefined' && (apiError.code === 2004 || apiError.code === 2005)) {
      window.dispatchEvent(new CustomEvent('auth:expired', { detail: apiError.responseMessage || '登录已失效' }))
    }
    return Promise.reject(apiError)
  },
)

/**
 * 读取浏览器 Cookie：按名称找到对应的 Cookie 值。
 */
function readCookie(name: string) {
  if (typeof document === 'undefined') {
    return ''
  }
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : ''
}

export default http
