import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/query/work-status', component: () => import('@/views/query/WorkStatusView.vue') },
  { path: '/query/production-work', component: () => import('@/views/query/ProductionWorkQuery.vue') },
  { path: '/query/inspection-work', component: () => import('@/views/query/InspectionWorkQuery.vue') },
  { path: '/query/work-start-check', component: () => import('@/views/query/WorkStartCheckQuery.vue') },
  { path: '/query/order-start-check', component: () => import('@/views/query/OrderStartCheckQuery.vue') },
  { path: '/query/shift-handover', component: () => import('@/views/query/ShiftHandoverQuery.vue') },
  { path: '/query/work-order', component: () => import('@/views/query/WorkOrderQuery.vue') },
  { path: '/query/dispatch-work', component: () => import('@/views/query/DispatchWorkQuery.vue') },
]

export default routes
