import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/system/user', component: () => import('@/views/system/UserList.vue') },
  { path: '/system/role', component: () => import('@/views/system/RoleList.vue') },
  { path: '/system/menu', component: () => import('@/views/system/MenuList.vue') },
]

export default routes
