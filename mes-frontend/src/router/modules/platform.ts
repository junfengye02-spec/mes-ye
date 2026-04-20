import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/platform/tenants',
    component: () => import('@/views/platform/TenantList.vue'),
    meta: {
      title: '租户管理',
      platformOnly: true,
      roles: ['PLATFORM_ADMIN'],
    },
  },
]

export default routes
