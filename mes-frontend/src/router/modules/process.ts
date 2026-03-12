import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/process/instruction', component: () => import('@/views/process/InstructionList.vue') },
  { path: '/process/template', component: () => import('@/views/process/ProcessTemplateList.vue') },
  { path: '/process/info', component: () => import('@/views/process/ProcessInfoList.vue') },
  { path: '/process/work-instruction', component: () => import('@/views/process/WorkInstructionList.vue') },
  { path: '/process/spray-condition', component: () => import('@/views/process/SprayConditionList.vue') },
  { path: '/process/machining-program', component: () => import('@/views/process/MachiningProgramList.vue') },
  { path: '/process/bom', component: () => import('@/views/process/ManufacturingBomList.vue') },
]

export default routes
