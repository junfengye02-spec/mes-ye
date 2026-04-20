export interface MenuItem {
  path: string
  title: string
  icon?: string
  children?: MenuItem[]
  /** 仅平台超管（tenant_id = 0）可见 */
  platformOnly?: boolean
}

export const menuList: MenuItem[] = [
  {
    path: '/basic',
    title: '基础数据',
    icon: 'Files',
    children: [
      { path: '/basic/material', title: '物料档案' },
      { path: '/basic/material-price', title: '物料价格' },
      { path: '/basic/work-center', title: '工作中心' },
    ],
  },
  {
    path: '/team',
    title: '班组管理',
    icon: 'User',
    children: [
      { path: '/team/production-team', title: '生产班组' },
    ],
  },
  {
    path: '/process',
    title: '工艺管理',
    icon: 'Setting',
    children: [
      { path: '/process/instruction', title: '指示书管理' },
      { path: '/process/template', title: '工序模板' },
      { path: '/process/info', title: '工序信息' },
      { path: '/process/work-instruction', title: '指导书管理' },
      { path: '/process/spray-condition', title: '喷涂条件表' },
      { path: '/process/machining-program', title: '机械加工程序表' },
      { path: '/process/bom', title: '制造BOM' },
    ],
  },
  {
    path: '/plan',
    title: '计划管理',
    icon: 'Calendar',
    children: [
      { path: '/plan/order', title: '订单计划' },
      { path: '/plan/production', title: '生产计划' },
    ],
  },
  {
    path: '/workorder',
    title: '生产工单',
    icon: 'Document',
    children: [
      { path: '/workorder/list', title: '工单管理' },
    ],
  },
  {
    path: '/dispatch',
    title: '生产派工',
    icon: 'Coordinate',
    children: [
      { path: '/dispatch/task', title: '派工管理' },
    ],
  },
  {
    path: '/abnormal',
    title: '异常管理',
    icon: 'WarningFilled',
    children: [
      { path: '/abnormal/contact', title: '异常联络单' },
    ],
  },
  {
    path: '/quality',
    title: '成品质量',
    icon: 'CircleCheck',
    children: [
      { path: '/quality/recheck', title: '复检申请' },
      { path: '/quality/work-start-check', title: '开工检查实绩(工作)' },
      { path: '/quality/order-start-check', title: '开工检查实绩(工单)' },
      { path: '/quality/shift-handover', title: '交班记录' },
    ],
  },
  {
    path: '/query',
    title: '工作查询',
    icon: 'Search',
    children: [
      { path: '/query/work-status', title: '六状态查看' },
      { path: '/query/production-work', title: '生产工作查询' },
      { path: '/query/inspection-work', title: '检验工作查询' },
      { path: '/query/work-start-check', title: '生产工作开工检查实绩' },
      { path: '/query/order-start-check', title: '生产工单开工检查实绩' },
      { path: '/query/shift-handover', title: '交班记录' },
      { path: '/query/work-order', title: '生产工单' },
      { path: '/query/dispatch-work', title: '派工工作查询' },
    ],
  },
  {
    path: '/material-mgmt',
    title: '物料管理',
    icon: 'Box',
    children: [
      { path: '/material-mgmt/inventory', title: '存储地点库存' },
      { path: '/material-mgmt/requisition', title: '生产领料' },
      { path: '/material-mgmt/requisition-order', title: '按物料领料' },
      { path: '/material-mgmt/receipt-request', title: '完工入库申请' },
      { path: '/material-mgmt/receipt', title: '完工入库' },
      { path: '/material-mgmt/return', title: '生产退料' },
      { path: '/material-mgmt/delivery-sign', title: '发货签收' },
    ],
  },
  {
    path: '/aps',
    title: 'APS 集成',
    icon: 'Connection',
    children: [
      { path: '/aps/sync-config', title: '同步配置' },
      { path: '/aps/sync-log', title: '同步日志' },
      { path: '/aps/data-mapping', title: '数据映射管理' },
    ],
  },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Tools',
    children: [
      { path: '/system/user', title: '用户管理' },
      { path: '/system/role', title: '角色管理' },
      { path: '/system/menu', title: '菜单管理' },
    ],
  },
  {
    path: '/platform',
    title: '运营后台',
    icon: 'OfficeBuilding',
    platformOnly: true,
    children: [
      { path: '/platform/tenants', title: '租户管理', platformOnly: true },
    ],
  },
]
