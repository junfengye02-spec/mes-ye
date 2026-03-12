import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/material-mgmt/inventory', component: () => import('@/views/material-mgmt/InventoryList.vue') },
  { path: '/material-mgmt/requisition', component: () => import('@/views/material-mgmt/RequisitionList.vue') },
  { path: '/material-mgmt/requisition-order', component: () => import('@/views/material-mgmt/RequisitionOrderList.vue') },
  { path: '/material-mgmt/receipt-request', component: () => import('@/views/material-mgmt/ReceiptRequestList.vue') },
  { path: '/material-mgmt/receipt', component: () => import('@/views/material-mgmt/ReceiptList.vue') },
  { path: '/material-mgmt/return', component: () => import('@/views/material-mgmt/ReturnList.vue') },
  { path: '/material-mgmt/delivery-sign', component: () => import('@/views/material-mgmt/DeliverySignList.vue') },
]

export default routes
