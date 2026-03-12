import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface R<T = any> {
  code: number
  message: string
  data: T
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

function getToken(): string | null {
  return localStorage.getItem('token')
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
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retried) {
      const storedRefreshToken = localStorage.getItem('refreshToken')
      if (!storedRefreshToken) {
        handleLogout()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(service(originalRequest))
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
        pendingRequests.forEach(cb => cb(newToken))
        pendingRequests = []
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return service(originalRequest)
      } catch {
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
  if (currentPath !== '/login') {
    window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
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
    return service.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export default request
