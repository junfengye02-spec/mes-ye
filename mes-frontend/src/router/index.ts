import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { MainLayout } from '@/layout'

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

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false },
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
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const PUBLIC_PATHS = new Set(['/login'])

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (PUBLIC_PATHS.has(to.path)) {
    token ? next('/') : next()
    return
  }

  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
