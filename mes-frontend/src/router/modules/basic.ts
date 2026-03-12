import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/basic/material', component: () => import('@/views/basic/MaterialList.vue') },
  { path: '/basic/material-price', component: () => import('@/views/basic/MaterialPriceList.vue') },
  { path: '/basic/work-center', component: () => import('@/views/basic/WorkCenterList.vue') },
]

export default routes
