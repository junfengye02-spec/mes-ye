import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/system/auth'
import type { UserInfo, LoginParams } from '@/api/system/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)
  let refreshPromise: Promise<void> | null = null

  const isLoggedIn = computed(() => !!accessToken.value)
  const username = computed(() => userInfo.value?.realName || userInfo.value?.username || '')

  async function login(params: LoginParams) {
    const res = await authApi.login(params)
    accessToken.value = res.accessToken
    refreshToken.value = res.refreshToken
    userInfo.value = res.userInfo
    localStorage.setItem('token', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
  }

  async function fetchUserInfo() {
    if (!accessToken.value) return
    try {
      userInfo.value = await authApi.getUserInfo()
    } catch {
      logout()
    }
  }

  async function doRefreshToken(): Promise<void> {
    if (refreshPromise) return refreshPromise
    refreshPromise = (async () => {
      try {
        if (!refreshToken.value) throw new Error('no refresh token')
        const res = await authApi.refresh(refreshToken.value)
        accessToken.value = res.accessToken
        refreshToken.value = res.refreshToken
        localStorage.setItem('token', res.accessToken)
        localStorage.setItem('refreshToken', res.refreshToken)
      } finally {
        refreshPromise = null
      }
    })()
    return refreshPromise
  }

  function logout() {
    authApi.logout().catch(() => {})
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    refreshPromise = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    try {
      import('@/stores/permission').then(({ usePermissionStore }) => {
        usePermissionStore().reset()
      })
    } catch { /* ignore if store not ready */ }
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    username,
    login,
    fetchUserInfo,
    doRefreshToken,
    logout,
  }
})
