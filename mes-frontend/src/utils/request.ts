import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '@/utils/apiBase'

export interface R<T = any> {
  code: number
  message: string
  data: T
}

const service: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

type PendingEntry = {
  resolve: (token: string) => void
  reject: (err: unknown) => void
}

let isRefreshing = false
let pendingRequests: PendingEntry[] = []

function getToken(): string | null {
  return localStorage.getItem('token')
}

function flushPending(token: string) {
  pendingRequests.forEach((p) => p.resolve(token))
  pendingRequests = []
}

function rejectPending(err: unknown) {
  pendingRequests.forEach((p) => p.reject(err))
  pendingRequests = []
}

function isRefreshUrl(url?: string): boolean {
  return !!url && /\/auth\/refresh\b/.test(url)
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response: AxiosResponse<R>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data as any
  },
  async (error) => {
    const originalRequest = error.config || {}
    const status = error.response?.status

    // 刷新接口本身 401 → 直接登出，避免死锁：既不入队列也不重试
    if (status === 401 && isRefreshUrl(originalRequest.url)) {
      rejectPending(error)
      isRefreshing = false
      handleLogout()
      return Promise.reject(error)
    }

    if (status === 401 && !originalRequest._retried) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingRequests.push({
            resolve: (newToken: string) => {
              originalRequest.headers = originalRequest.headers || {}
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              originalRequest._retried = true
              resolve(service(originalRequest))
            },
            reject,
          })
        })
      }

      isRefreshing = true
      originalRequest._retried = true

      try {
        const { useAuthStore } = await import('@/stores/auth')
        const authStore = useAuthStore()
        await authStore.doRefreshToken()
        const newToken = authStore.accessToken
        flushPending(newToken)
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return service(originalRequest)
      } catch (e) {
        rejectPending(e)
        handleLogout()
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  const currentPath = window.location.pathname
  const isAppSide = currentPath.startsWith('/app')
  const loginPath = isAppSide ? '/app/login' : '/login'
  if (currentPath !== loginPath) {
    window.location.href = `${loginPath}?redirect=${encodeURIComponent(currentPath)}`
  }
}

const request = {
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, { params, ...config })
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },
  delete<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, { params, ...config })
  },
  upload<T = any>(url: string, file: File, directory?: string): Promise<T> {
    const formData = new FormData()
    formData.append('file', file)
    if (directory) formData.append('directory', directory)
    // 不手动设置 Content-Type；浏览器会自动附带正确的 multipart boundary
    return service.post(url, formData)
  },
}

export default request
