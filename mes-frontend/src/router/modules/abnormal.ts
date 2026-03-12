import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/abnormal/contact', component: () => import('@/views/abnormal/AbnormalContactList.vue') },
]

export default routes
