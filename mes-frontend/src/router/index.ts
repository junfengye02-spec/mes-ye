import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { MainLayout } from '@/layout'
import UserPortalLayout from '@/layout/UserPortalLayout.vue'
import { useAuthStore } from '@/stores/auth'

import basicRoutes from './modules/basic'
import teamRoutes from './modules/team'
import processRoutes from './modules/process'
import planRoutes from './modules/plan'
import workorderRoutes from './modules/workorder'
import dispatchRoutes from './modules/dispatch'
import abnormalRoutes from './modules/abnormal'
import qualityRoutes from './modules/quality'
import queryRoutes from './modules/query'
import materialMgmtRoutes from './modules/material-mgmt'
import apsRoutes from './modules/aps'
import systemRoutes from './modules/system'
import platformRoutes from './modules/platform'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/app/login',
    name: 'AppLogin',
    component: () => import('@/views/login/AppLogin.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/app',
    component: UserPortalLayout,
    meta: { requiresAuth: true, portal: 'USER' },
    children: [
      { path: '', redirect: '/app/workorder/list' },
      {
        path: 'workorder/list',
        component: () => import('@/views/workorder/WorkOrderList.vue'),
      },
      {
        path: 'workorder/detail/:id',
        component: () => import('@/views/workorder/WorkOrderDetail.vue'),
      },
      {
        path: 'dispatch/task',
        component: () => import('@/views/dispatch/DispatchTask.vue'),
      },
      {
        path: 'query/work-status',
        component: () => import('@/views/query/WorkStatusView.vue'),
      },
    ],
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/basic/material',
    children: [
      ...basicRoutes,
      ...teamRoutes,
      ...processRoutes,
      ...planRoutes,
      ...workorderRoutes,
      ...dispatchRoutes,
      ...abnormalRoutes,
      ...qualityRoutes,
      ...queryRoutes,
      ...materialMgmtRoutes,
      ...apsRoutes,
      ...systemRoutes,
      ...platformRoutes,
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const PUBLIC_PATHS = new Set(['/login', '/app/login'])

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')

  if (PUBLIC_PATHS.has(to.path)) {
    if (token) {
      const authStore = useAuthStore()
      if (!authStore.userInfo) {
        await authStore.fetchUserInfo().catch(() => {})
      }
      if (to.path === '/login') {
        next(authStore.userInfo?.accountType === 'STAFF' ? '/app/workorder/list' : '/')
        return
      }
      if (to.path === '/app/login') {
        next('/app/workorder/list')
        return
      }
    }
    next()
    return
  }

  if (!token) {
    if (to.path.startsWith('/app')) {
      next({ path: '/app/login', query: { redirect: to.fullPath } })
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return
  }

  const authStore = useAuthStore()
  if (!authStore.userInfo) {
    const loginPath = to.path.startsWith('/app') ? '/app/login' : '/login'
    try {
      await authStore.fetchUserInfo()
    } catch {
      authStore.logout()
      next({ path: loginPath, query: { redirect: to.fullPath } })
      return
    }
    if (!authStore.userInfo) {
      next({ path: loginPath, query: { redirect: to.fullPath } })
      return
    }
  }

  const accountType = authStore.userInfo?.accountType || 'ADMIN'
  if (accountType === 'STAFF') {
    if (!to.path.startsWith('/app')) {
      next('/app/workorder/list')
      return
    }
  }

  if (to.path.startsWith('/system') && accountType === 'STAFF') {
    next('/app/workorder/list')
    return
  }

  // 平台级路由：仅允许 tenantId = 0 的平台超管访问
  if (to.matched.some((r) => r.meta?.platformOnly) || to.path.startsWith('/platform')) {
    const tenantId = authStore.userInfo?.tenantId
    if (tenantId !== 0) {
      next('/')
      return
    }
  }

  next()
})

export default router
