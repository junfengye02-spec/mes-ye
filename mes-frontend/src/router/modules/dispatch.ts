import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/dispatch/task', component: () => import('@/views/dispatch/DispatchTask.vue') },
]

export default routes
