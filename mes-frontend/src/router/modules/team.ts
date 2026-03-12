import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/team/production-team', component: () => import('@/views/team/ProductionTeamList.vue') },
]

export default routes
