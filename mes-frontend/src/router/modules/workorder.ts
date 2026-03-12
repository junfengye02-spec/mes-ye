import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/workorder/list', component: () => import('@/views/workorder/WorkOrderList.vue') },
  { path: '/workorder/detail/:id', component: () => import('@/views/workorder/WorkOrderDetail.vue') },
]

export default routes
