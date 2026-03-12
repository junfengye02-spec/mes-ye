import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/quality/recheck', component: () => import('@/views/quality/RecheckRequestList.vue') },
  { path: '/quality/work-start-check', component: () => import('@/views/quality/WorkStartCheckList.vue') },
  { path: '/quality/order-start-check', component: () => import('@/views/quality/OrderStartCheckList.vue') },
  { path: '/quality/shift-handover', component: () => import('@/views/quality/ShiftHandoverList.vue') },
]

export default routes
