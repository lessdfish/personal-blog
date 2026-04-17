import axios from 'axios'
import type { ApiError, Result } from '../types/api'

const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
  withCredentials: true,
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

export default http
