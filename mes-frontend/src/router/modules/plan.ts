import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/plan/order', component: () => import('@/views/plan/OrderPlanList.vue') },
  { path: '/plan/production', component: () => import('@/views/plan/ProductionPlanList.vue') },
]

export default routes
