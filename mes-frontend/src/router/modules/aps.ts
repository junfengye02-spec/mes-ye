import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/aps/sync-config', component: () => import('@/views/aps/SyncConfigList.vue') },
  { path: '/aps/sync-log', component: () => import('@/views/aps/SyncLogList.vue') },
  { path: '/aps/data-mapping', component: () => import('@/views/aps/DataMappingList.vue') },
]

export default routes
