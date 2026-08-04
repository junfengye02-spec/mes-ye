export interface MenuItem {
  path: string
  /** 原始中文标题，作为 i18n 未命中时的 fallback 显示 */
  title: string
  /** i18n key，优先用于渲染（在 SidebarMenu/BreadcrumbNav 中通过 t(key) 翻译） */
  i18nKey?: string
  icon?: string
  children?: MenuItem[]
  /** 仅平台超管（tenant_id = 0）可见 */
  platformOnly?: boolean
}

export const menuList: MenuItem[] = [
  {
    path: '/basic',
    title: '基础数据',
    i18nKey: 'menu.basic._title',
    icon: 'Files',
    children: [
      { path: '/basic/material', title: '物料档案', i18nKey: 'menu.basic.material' },
      { path: '/basic/material-price', title: '物料价格', i18nKey: 'menu.basic.materialPrice' },
      { path: '/basic/work-center', title: '工作中心', i18nKey: 'menu.basic.workCenter' },
    ],
  },
  {
    path: '/team',
    title: '班组管理',
    i18nKey: 'menu.team._title',
    icon: 'User',
    children: [
      { path: '/team/production-team', title: '生产班组', i18nKey: 'menu.team.productionTeam' },
    ],
  },
  {
    path: '/process',
    title: '工艺管理',
    i18nKey: 'menu.process._title',
    icon: 'Setting',
    children: [
      { path: '/process/route', title: '工艺路线', i18nKey: 'menu.process.route' },
      { path: '/process/info', title: '工序库', i18nKey: 'menu.process.info' },
      { path: '/process/template', title: '工序模板', i18nKey: 'menu.process.template' },
      { path: '/process/work-instruction', title: '作业指导书', i18nKey: 'menu.process.workInstruction' },
      { path: '/process/instruction', title: '执行指示', i18nKey: 'menu.process.instruction' },
      { path: '/process/bom', title: '制造BOM', i18nKey: 'menu.process.bom' },
      { path: '/process/spray-condition', title: '喷涂参数', i18nKey: 'menu.process.sprayCondition' },
      { path: '/process/machining-program', title: '机加程序参数', i18nKey: 'menu.process.machiningProgram' },
    ],
  },
  {
    path: '/plan',
    title: '计划管理',
    i18nKey: 'menu.plan._title',
    icon: 'Calendar',
    children: [
      { path: '/plan/order', title: '订单计划', i18nKey: 'menu.plan.order' },
      { path: '/plan/production', title: '生产计划', i18nKey: 'menu.plan.production' },
    ],
  },
  {
    path: '/workorder',
    title: '生产工单',
    i18nKey: 'menu.workorder._title',
    icon: 'Document',
    children: [
      { path: '/workorder/list', title: '工单管理', i18nKey: 'menu.workorder.list' },
    ],
  },
  {
    path: '/dispatch',
    title: '生产派工',
    i18nKey: 'menu.dispatch._title',
    icon: 'Coordinate',
    children: [
      { path: '/dispatch/task', title: '派工管理', i18nKey: 'menu.dispatch.task' },
    ],
  },
  {
    path: '/abnormal',
    title: '异常管理',
    i18nKey: 'menu.abnormal._title',
    icon: 'WarningFilled',
    children: [
      { path: '/abnormal/contact', title: '异常联络单', i18nKey: 'menu.abnormal.contact' },
    ],
  },
  {
    path: '/quality',
    title: '成品质量',
    i18nKey: 'menu.quality._title',
    icon: 'CircleCheck',
    children: [
      { path: '/quality/recheck', title: '复检申请', i18nKey: 'menu.quality.recheck' },
      { path: '/quality/work-start-check', title: '开工检查实绩(工作)', i18nKey: 'menu.quality.workStartCheck' },
      { path: '/quality/order-start-check', title: '开工检查实绩(工单)', i18nKey: 'menu.quality.orderStartCheck' },
      { path: '/quality/shift-handover', title: '交班记录', i18nKey: 'menu.quality.shiftHandover' },
    ],
  },
  {
    path: '/query',
    title: '工作查询',
    i18nKey: 'menu.query._title',
    icon: 'Search',
    children: [
      { path: '/query/work-status', title: '六状态查看', i18nKey: 'menu.query.workStatus' },
      { path: '/query/production-work', title: '生产工作查询', i18nKey: 'menu.query.productionWork' },
      { path: '/query/inspection-work', title: '检验工作查询', i18nKey: 'menu.query.inspectionWork' },
      { path: '/query/work-start-check', title: '生产工作开工检查实绩', i18nKey: 'menu.query.workStartCheck' },
      { path: '/query/order-start-check', title: '生产工单开工检查实绩', i18nKey: 'menu.query.orderStartCheck' },
      { path: '/query/shift-handover', title: '交班记录', i18nKey: 'menu.query.shiftHandover' },
      { path: '/query/work-order', title: '生产工单', i18nKey: 'menu.query.workOrder' },
      { path: '/query/dispatch-work', title: '派工工作查询', i18nKey: 'menu.query.dispatchWork' },
    ],
  },
  {
    path: '/material-mgmt',
    title: '物料管理',
    i18nKey: 'menu.materialMgmt._title',
    icon: 'Box',
    children: [
      { path: '/material-mgmt/inventory', title: '存储地点库存', i18nKey: 'menu.materialMgmt.inventory' },
      { path: '/material-mgmt/requisition', title: '生产领料', i18nKey: 'menu.materialMgmt.requisition' },
      { path: '/material-mgmt/requisition-order', title: '按物料领料', i18nKey: 'menu.materialMgmt.requisitionOrder' },
      { path: '/material-mgmt/receipt-request', title: '完工入库申请', i18nKey: 'menu.materialMgmt.receiptRequest' },
      { path: '/material-mgmt/receipt', title: '完工入库', i18nKey: 'menu.materialMgmt.receipt' },
      { path: '/material-mgmt/return', title: '生产退料', i18nKey: 'menu.materialMgmt.return' },
      { path: '/material-mgmt/delivery-sign', title: '发货签收', i18nKey: 'menu.materialMgmt.deliverySign' },
    ],
  },
  {
    path: '/aps',
    title: 'APS 集成',
    i18nKey: 'menu.aps._title',
    icon: 'Connection',
    children: [
      { path: '/aps/sync-config', title: '同步配置', i18nKey: 'menu.aps.syncConfig' },
      { path: '/aps/sync-log', title: '同步日志', i18nKey: 'menu.aps.syncLog' },
      { path: '/aps/data-mapping', title: '数据映射管理', i18nKey: 'menu.aps.dataMapping' },
    ],
  },
  {
    path: '/system',
    title: '系统管理',
    i18nKey: 'menu.system._title',
    icon: 'Tools',
    children: [
      { path: '/system/user', title: '用户管理', i18nKey: 'menu.system.user' },
      { path: '/system/role', title: '角色管理', i18nKey: 'menu.system.role' },
      { path: '/system/menu', title: '菜单管理', i18nKey: 'menu.system.menu' },
    ],
  },
  {
    path: '/platform',
    title: '运营后台',
    i18nKey: 'menu.platform._title',
    icon: 'OfficeBuilding',
    platformOnly: true,
    children: [
      { path: '/platform/tenants', title: '租户管理', i18nKey: 'menu.platform.tenants', platformOnly: true },
    ],
  },
]
