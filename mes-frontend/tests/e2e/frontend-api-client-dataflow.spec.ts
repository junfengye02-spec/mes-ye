import type { Page } from '@playwright/test'
import { execSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { expect, test } from './fixtures'

type ApiCall = {
  method: string
  path: string
  query: Record<string, string>
  payload: unknown
}

type ApiCase = {
  name: string
  modulePath: string
  exportName: string
  methodName: string
  args: unknown[]
  expectMethod: string
  expectPath: string
  expectPayload?: unknown
  expectQuery?: Record<string, string>
}

type CrudResource = {
  name: string
  modulePath: string
  exportName: string
  base: string
  dto: Record<string, unknown>
  query?: Record<string, unknown>
  id?: number
  pageMethod?: string
  pagePath?: string
  detailMethod?: string
  createMethod?: string
  updateMethod?: string
  deleteMethod?: string
  hasPage?: boolean
  hasDetail?: boolean
  hasCreate?: boolean
  hasUpdate?: boolean
  hasDelete?: boolean
  createPath?: string
  updatePath?: string
  deletePath?: string
}

const ok = (data: unknown) => ({ code: 200, message: 'OK', data })
const pageQuery = { pageNum: 1, pageSize: 20 }
const id = 100

function crudCases(resource: CrudResource): ApiCase[] {
  const itemId = resource.id ?? id
  const query = resource.query ?? pageQuery
  const cases: ApiCase[] = []

  if (resource.hasPage !== false) {
    cases.push({
      name: `${resource.name} page`,
      modulePath: resource.modulePath,
      exportName: resource.exportName,
      methodName: resource.pageMethod ?? 'page',
      args: [query],
      expectMethod: 'GET',
      expectPath: resource.pagePath ?? `${resource.base}/page`,
    })
  }

  if (resource.hasDetail !== false) {
    cases.push({
      name: `${resource.name} detail`,
      modulePath: resource.modulePath,
      exportName: resource.exportName,
      methodName: resource.detailMethod ?? 'getDetail',
      args: [itemId],
      expectMethod: 'GET',
      expectPath: `${resource.base}/${itemId}`,
    })
  }

  if (resource.hasCreate !== false) {
    cases.push({
      name: `${resource.name} create`,
      modulePath: resource.modulePath,
      exportName: resource.exportName,
      methodName: resource.createMethod ?? 'create',
      args: [resource.dto],
      expectMethod: 'POST',
      expectPath: resource.createPath ?? resource.base,
      expectPayload: resource.dto,
    })
  }

  if (resource.hasUpdate !== false) {
    cases.push({
      name: `${resource.name} update`,
      modulePath: resource.modulePath,
      exportName: resource.exportName,
      methodName: resource.updateMethod ?? 'update',
      args: [itemId, resource.dto],
      expectMethod: 'PUT',
      expectPath: resource.updatePath ?? `${resource.base}/${itemId}`,
      expectPayload: resource.dto,
    })
  }

  if (resource.hasDelete !== false) {
    cases.push({
      name: `${resource.name} delete`,
      modulePath: resource.modulePath,
      exportName: resource.exportName,
      methodName: resource.deleteMethod ?? 'delete',
      args: [itemId],
      expectMethod: 'DELETE',
      expectPath: resource.deletePath ?? `${resource.base}/${itemId}`,
    })
  }

  return cases
}

const crudResources: CrudResource[] = [
  {
    name: 'material',
    modulePath: '/src/api/basic/material.ts',
    exportName: 'materialApi',
    base: '/basic/material',
    dto: { materialCode: 'MAT-E2E', materialName: 'E2E物料', materialType: 'RAW', baseUnit: 'PCS' },
  },
  {
    name: 'material price',
    modulePath: '/src/api/basic/materialPrice.ts',
    exportName: 'materialPriceApi',
    base: '/basic/material-price',
    dto: { materialId: 1, materialCode: 'MAT-E2E', materialName: 'E2E物料', unitPrice: 12.5, unit: 'PCS' },
  },
  {
    name: 'work center',
    modulePath: '/src/api/basic/workCenter.ts',
    exportName: 'workCenterApi',
    base: '/basic/work-center',
    dto: { workCenterCode: 'WC-E2E', workCenterName: 'E2E工作中心', workCenterCategory: 'MACHINING' },
  },
  {
    name: 'production team',
    modulePath: '/src/api/team/productionTeam.ts',
    exportName: 'productionTeamApi',
    base: '/team/production-team',
    dto: { teamCode: 'TEAM-E2E', teamName: 'E2E班组', orgId: 1, orgCode: 'ORG', orgName: '组织' },
  },
  {
    name: 'instruction',
    modulePath: '/src/api/process/instruction.ts',
    exportName: 'instructionApi',
    base: '/process/instruction',
    dto: { instructionNo: 'INS-E2E', projectNo: 'PRJ', productCategory: 'CAT', productType: 'TYPE', workOrderNo: 'WO', qty: 1 },
  },
  {
    name: 'work instruction',
    modulePath: '/src/api/process/workInstruction.ts',
    exportName: 'workInstructionApi',
    base: '/process/work-instruction',
    dto: { instructionCode: 'WI-E2E', instructionName: 'E2E指导书', processId: 1, version: 'A', content: '内容' },
  },
  {
    name: 'machining program',
    modulePath: '/src/api/process/machiningProgram.ts',
    exportName: 'machiningProgramApi',
    base: '/process/machining-program',
    dto: { gCode: 'G01 X1', programTable: 'N10 G01', productName: 'E2E产品' },
  },
  {
    name: 'spray condition',
    modulePath: '/src/api/process/sprayCondition.ts',
    exportName: 'sprayConditionApi',
    base: '/process/spray-condition',
    dto: { conditionNo: 'SC-E2E', powderFeedRate: 1, sprayDistance: 2, sprayGunModel: 'GUN', equipment: 'EQ' },
  },
  {
    name: 'process info',
    modulePath: '/src/api/process/processInfo.ts',
    exportName: 'processInfoApi',
    base: '/process/process-info',
    dto: { processNo: 'P10', processName: 'E2E工序', processCode: 'PROC', processType: 'MACHINING', handleTime: 1 },
  },
  {
    name: 'process template',
    modulePath: '/src/api/process/processTemplate.ts',
    exportName: 'processTemplateApi',
    base: '/process/process-template',
    dto: { processNo: 'TPL-P10', processName: '模板工序', processType: 'MACHINING' },
    hasPage: false,
  },
  {
    name: 'manufacturing bom',
    modulePath: '/src/api/process/manufacturingBom.ts',
    exportName: 'manufacturingBomApi',
    base: '/process/manufacturing-bom',
    dto: { bomCode: 'BOM-E2E', bomName: 'E2E BOM', version: 'A', productCode: 'PROD', productName: '产品' },
  },
  {
    name: 'order plan',
    modulePath: '/src/api/plan/orderPlan.ts',
    exportName: 'orderPlanApi',
    base: '/plan/order-plan',
    dto: { orderNo: 'ORDER-E2E', productCode: 'PROD', productName: '产品', planQty: 1, qtyUnit: 'PCS' },
  },
  {
    name: 'production plan',
    modulePath: '/src/api/plan/productionPlan.ts',
    exportName: 'productionPlanApi',
    base: '/plan/production-plan',
    dto: { orderPlanId: 1, orderNo: 'ORDER-E2E', productCode: 'PROD', productName: '产品', planQty: 1, qtyUnit: 'PCS' },
  },
  {
    name: 'work order',
    modulePath: '/src/api/workorder/workOrder.ts',
    exportName: 'workOrderApi',
    base: '/workorder/work-order',
    dto: { workOrderNo: 'WO-E2E', workOrderType: 'NORMAL', productCode: 'PROD', productName: '产品', planQty: 1, qtyUnit: 'PCS' },
  },
  {
    name: 'requisition',
    modulePath: '/src/api/material-mgmt/requisition.ts',
    exportName: 'requisitionApi',
    base: '/material/requisition',
    dto: { requisitionNo: 'REQ-E2E', workOrderId: 1, workOrderNo: 'WO-E2E', productCode: 'PROD', productName: '产品', items: [] },
  },
  {
    name: 'requisition order',
    modulePath: '/src/api/material-mgmt/requisitionOrder.ts',
    exportName: 'requisitionOrderApi',
    base: '/material/requisition-order',
    dto: { materialId: 1, materialCode: 'MAT', materialName: '物料', totalQty: 1, qtyUnit: 'PCS' },
  },
  {
    name: 'receipt request',
    modulePath: '/src/api/material-mgmt/receipt.ts',
    exportName: 'receiptRequestApi',
    base: '/material/receipt/request',
    pagePath: '/material/receipt/request/page',
    dto: { requestNo: 'RR-E2E', receiptType: 'FINISHED', workOrderId: 1, workOrderNo: 'WO-E2E', items: [] },
  },
  {
    name: 'receipt',
    modulePath: '/src/api/material-mgmt/receipt.ts',
    exportName: 'receiptApi',
    base: '/material/receipt',
    dto: { receiptNo: 'REC-E2E', receiptType: 'FINISHED', warehouse: 'WH', movementType: '101', items: [] },
  },
  {
    name: 'material return',
    modulePath: '/src/api/material-mgmt/materialReturn.ts',
    exportName: 'materialReturnApi',
    base: '/material/return',
    dto: { workOrderId: 1, workOrderNo: 'WO-E2E', orderNo: 'ORDER', productCode: 'PROD', productName: '产品' },
  },
  {
    name: 'delivery sign',
    modulePath: '/src/api/material-mgmt/deliverySign.ts',
    exportName: 'deliverySignApi',
    base: '/material/delivery-sign',
    dto: { lineNo: '10', workOrderId: 1, workOrderNo: 'WO-E2E', materialId: 1, materialCode: 'MAT', materialName: '物料', planDeliveryQty: 1, pendingSignQty: 1, unit: 'PCS' },
    hasDetail: false,
    hasUpdate: false,
    hasDelete: false,
  },
  {
    name: 'recheck request',
    modulePath: '/src/api/quality/recheckRequest.ts',
    exportName: 'recheckRequestApi',
    base: '/quality/recheck',
    dto: { projectCode: 'PRJ', materialCode: 'MAT', materialName: '物料', productionOrderNo: 'WO-E2E', recheckReason: '原因', recheckProposer: '张三' },
  },
  {
    name: 'work start check',
    modulePath: '/src/api/quality/workStartCheck.ts',
    exportName: 'workStartCheckApi',
    base: '/quality/work-start-check',
    dto: { workOrderId: 1, workNo: 'WORK-E2E', checkItem: '检查项', checkStatus: 'PASSED', checkResult: '正常' },
    hasDelete: false,
  },
  {
    name: 'order start check',
    modulePath: '/src/api/quality/orderStartCheck.ts',
    exportName: 'orderStartCheckApi',
    base: '/quality/order-start-check',
    dto: { workOrderId: 1, workNo: 'ORDER-WORK-E2E', checkItem: '检查项', checkStatus: 'PASSED', checkResult: '正常' },
    hasDelete: false,
  },
  {
    name: 'shift handover',
    modulePath: '/src/api/quality/shiftHandover.ts',
    exportName: 'shiftHandoverApi',
    base: '/quality/shift-handover',
    dto: { projectName: '项目', productSerialNo: 'SN-E2E', handoverDate: '2026-05-31', handoverPerson: '交接人', takeoverPerson: '接班人', handoverContent: '内容' },
    hasDelete: false,
  },
  {
    name: 'abnormal contact',
    modulePath: '/src/api/abnormal/abnormalContact.ts',
    exportName: 'abnormalContactApi',
    base: '/abnormal/contact',
    dto: { contactNo: 'ABN-E2E', subject: '异常', occurStage: '生产', eventCategory: '质量', productName: '产品', qty: 1 },
  },
  {
    name: 'APS config',
    modulePath: '/src/api/aps/apsSync.ts',
    exportName: 'apsSyncConfigApi',
    base: '/aps/config',
    pagePath: '/aps/config/page',
    dto: { configKey: 'sync.enabled', configValue: 'true', configDesc: '启用', enabled: 1 },
  },
  {
    name: 'APS mapping',
    modulePath: '/src/api/aps/apsSync.ts',
    exportName: 'apsDataMappingApi',
    base: '/aps/mapping',
    pagePath: '/aps/mapping/page',
    dto: { mappingType: 'WORK_CENTER', mesCode: 'MES-WC', mesName: 'MES资源', apsCode: 'APS-WC', apsName: 'APS资源', enabled: 1 },
  },
  {
    name: 'system user',
    modulePath: '/src/api/system/user.ts',
    exportName: 'sysUserApi',
    base: '/system/user',
    dto: { username: 'e2e_user', password: 'Change123', realName: 'E2E用户', accountType: 'ADMIN', enabled: true },
  },
  {
    name: 'system role',
    modulePath: '/src/api/system/role.ts',
    exportName: 'sysRoleApi',
    base: '/system/role',
    dto: { roleCode: 'E2E_ROLE', roleName: 'E2E角色', enabled: true },
  },
  {
    name: 'system menu',
    modulePath: '/src/api/system/menu.ts',
    exportName: 'sysMenuApi',
    base: '/system/menu',
    pageMethod: 'getTree',
    pagePath: '/system/menu/tree',
    dto: { parentId: 0, menuName: 'E2E菜单', menuType: 'MENU', sortOrder: 1, visible: true },
  },
]

const extraCases: ApiCase[] = [
  {
    name: 'work center batch update',
    modulePath: '/src/api/basic/workCenter.ts',
    exportName: 'workCenterApi',
    methodName: 'batchUpdate',
    args: [[{ workCenterCode: 'WC-BATCH', workCenterName: '批量中心' }]],
    expectMethod: 'PUT',
    expectPath: '/basic/work-center/batch',
    expectPayload: [{ workCenterCode: 'WC-BATCH', workCenterName: '批量中心' }],
  },
  {
    name: 'production team toggle',
    modulePath: '/src/api/team/productionTeam.ts',
    exportName: 'productionTeamApi',
    methodName: 'toggleEnabled',
    args: [id],
    expectMethod: 'PUT',
    expectPath: `/team/production-team/${id}/toggle-enabled`,
  },
  {
    name: 'process info batch update',
    modulePath: '/src/api/process/processInfo.ts',
    exportName: 'processInfoApi',
    methodName: 'batchUpdate',
    args: [[{ processNo: 'P20', processName: '批量工序' }]],
    expectMethod: 'PUT',
    expectPath: '/process/process-info/batch',
    expectPayload: [{ processNo: 'P20', processName: '批量工序' }],
  },
  {
    name: 'process template tree',
    modulePath: '/src/api/process/processTemplate.ts',
    exportName: 'processTemplateApi',
    methodName: 'tree',
    args: [],
    expectMethod: 'GET',
    expectPath: '/process/process-template/tree',
  },
  ...['upgrade', 'publish', 'disable'].map((methodName) => ({
    name: `manufacturing bom ${methodName}`,
    modulePath: '/src/api/process/manufacturingBom.ts',
    exportName: 'manufacturingBomApi',
    methodName,
    args: [id],
    expectMethod: 'POST',
    expectPath: `/process/manufacturing-bom/${id}/${methodName}`,
  })),
  {
    name: 'manufacturing bom items tree',
    modulePath: '/src/api/process/manufacturingBom.ts',
    exportName: 'manufacturingBomApi',
    methodName: 'getItemsTree',
    args: [id],
    expectMethod: 'GET',
    expectPath: `/process/manufacturing-bom/${id}/items/tree`,
  },
  {
    name: 'instruction upgrade',
    modulePath: '/src/api/process/instruction.ts',
    exportName: 'instructionApi',
    methodName: 'upgrade',
    args: [id],
    expectMethod: 'POST',
    expectPath: `/process/instruction/${id}/upgrade`,
  },
  ...['release', 'complete'].map((methodName) => ({
    name: `order plan ${methodName}`,
    modulePath: '/src/api/plan/orderPlan.ts',
    exportName: 'orderPlanApi',
    methodName,
    args: [id],
    expectMethod: 'POST',
    expectPath: `/plan/order-plan/${id}/${methodName}`,
  })),
  {
    name: 'order plan terminate',
    modulePath: '/src/api/plan/orderPlan.ts',
    exportName: 'orderPlanApi',
    methodName: 'terminate',
    args: [id, '终止原因'],
    expectMethod: 'POST',
    expectPath: `/plan/order-plan/${id}/terminate`,
    expectQuery: { reason: '终止原因' },
  },
  {
    name: 'production plan release',
    modulePath: '/src/api/plan/productionPlan.ts',
    exportName: 'productionPlanApi',
    methodName: 'release',
    args: [id],
    expectMethod: 'POST',
    expectPath: `/plan/production-plan/${id}/release`,
  },
  ...['release', 'start', 'complete'].map((methodName) => ({
    name: `work order ${methodName}`,
    modulePath: '/src/api/workorder/workOrder.ts',
    exportName: 'workOrderApi',
    methodName,
    args: [id],
    expectMethod: 'POST',
    expectPath: `/workorder/work-order/${id}/${methodName}`,
  })),
  {
    name: 'work order force complete',
    modulePath: '/src/api/workorder/workOrder.ts',
    exportName: 'workOrderApi',
    methodName: 'forceComplete',
    args: [id, { reason: '强制原因' }],
    expectMethod: 'POST',
    expectPath: `/workorder/work-order/${id}/force-complete`,
    expectPayload: { reason: '强制原因' },
  },
  {
    name: 'dispatch task page',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'page',
    args: [pageQuery],
    expectMethod: 'GET',
    expectPath: '/dispatch/task/page',
  },
  {
    name: 'dispatch task create',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'create',
    args: [{ workOrderId: 1, workOrderTaskId: 2, orderNo: 'ORDER', processNo: 'P10', workName: '工序', planQty: 1, qtyUnit: 'PCS' }],
    expectMethod: 'POST',
    expectPath: '/dispatch/task/create',
    expectPayload: { workOrderId: 1, workOrderTaskId: 2, orderNo: 'ORDER', processNo: 'P10', workName: '工序', planQty: 1, qtyUnit: 'PCS' },
  },
  {
    name: 'dispatch task update',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'update',
    args: [{ id, workName: '更新工序' }],
    expectMethod: 'PUT',
    expectPath: '/dispatch/task/update',
    expectPayload: { id, workName: '更新工序' },
  },
  {
    name: 'dispatch task assign',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'assign',
    args: [{ taskId: id, assignType: 'PERSON', assigneeIds: [1] }],
    expectMethod: 'POST',
    expectPath: '/dispatch/task/assign',
    expectPayload: { taskId: id, assignType: 'PERSON', assigneeIds: [1] },
  },
  {
    name: 'dispatch task unassign',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'unassign',
    args: [id, '撤销原因'],
    expectMethod: 'POST',
    expectPath: `/dispatch/task/unassign/${id}`,
    expectQuery: { reason: '撤销原因' },
  },
  {
    name: 'dispatch task cancel',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'cancel',
    args: [id, '取消原因'],
    expectMethod: 'POST',
    expectPath: `/dispatch/task/cancel/${id}`,
    expectQuery: { cancelReason: '取消原因' },
  },
  ...['start', 'complete'].map((methodName) => ({
    name: `dispatch task ${methodName}`,
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName,
    args: methodName === 'complete' ? [id, { actualEndTime: '2026-05-31 10:00:00', qualityResult: 'PASS' }] : [id],
    expectMethod: 'POST',
    expectPath: `/dispatch/task/${methodName}/${id}`,
    ...(methodName === 'complete' ? { expectPayload: { actualEndTime: '2026-05-31 10:00:00', qualityResult: 'PASS' } } : {}),
  })),
  {
    name: 'dispatch assignments',
    modulePath: '/src/api/dispatch/dispatchTask.ts',
    exportName: 'dispatchTaskApi',
    methodName: 'getAssignments',
    args: [id],
    expectMethod: 'GET',
    expectPath: `/dispatch/assignment/list/${id}`,
  },
  {
    name: 'delivery sign confirm',
    modulePath: '/src/api/material-mgmt/deliverySign.ts',
    exportName: 'deliverySignApi',
    methodName: 'confirm',
    args: [id],
    expectMethod: 'POST',
    expectPath: `/material/delivery-sign/${id}/confirm`,
  },
  ...['submit', 'review', 'approve', 'complete'].map((methodName) => ({
    name: `recheck ${methodName}`,
    modulePath: '/src/api/quality/recheckRequest.ts',
    exportName: 'recheckRequestApi',
    methodName,
    args:
      methodName === 'review'
        ? [id, { reviewer: '审核员', reviewDate: '2026-05-31', isReasonable: 1 }]
        : methodName === 'approve'
          ? [id, { approved: true }]
          : [id],
    expectMethod: 'POST',
    expectPath: `/quality/recheck/${id}/${methodName}`,
    ...(methodName === 'review' ? { expectPayload: { reviewer: '审核员', reviewDate: '2026-05-31', isReasonable: 1 } } : {}),
    ...(methodName === 'approve' ? { expectPayload: { approved: true } } : {}),
  })),
  {
    name: 'shift handover receive',
    modulePath: '/src/api/quality/shiftHandover.ts',
    exportName: 'shiftHandoverApi',
    methodName: 'receive',
    args: [id],
    expectMethod: 'POST',
    expectPath: `/quality/shift-handover/${id}/receive`,
  },
  ...['submit', 'process', 'close'].map((methodName) => ({
    name: `abnormal ${methodName}`,
    modulePath: '/src/api/abnormal/abnormalContact.ts',
    exportName: 'abnormalContactApi',
    methodName,
    args: [id],
    expectMethod: 'POST',
    expectPath: `/abnormal/contact/${id}/${methodName}`,
  })),
  {
    name: 'abnormal attachments',
    modulePath: '/src/api/abnormal/abnormalContact.ts',
    exportName: 'abnormalContactApi',
    methodName: 'getAttachments',
    args: [id],
    expectMethod: 'GET',
    expectPath: `/abnormal/contact/${id}/attachments`,
  },
  {
    name: 'abnormal add attachment',
    modulePath: '/src/api/abnormal/abnormalContact.ts',
    exportName: 'abnormalContactApi',
    methodName: 'addAttachment',
    args: [id, { fileName: 'a.pdf' }],
    expectMethod: 'POST',
    expectPath: `/abnormal/contact/${id}/attachments`,
    expectPayload: { fileName: 'a.pdf' },
  },
  {
    name: 'abnormal delete attachment',
    modulePath: '/src/api/abnormal/abnormalContact.ts',
    exportName: 'abnormalContactApi',
    methodName: 'deleteAttachment',
    args: [id],
    expectMethod: 'DELETE',
    expectPath: `/abnormal/contact/attachments/${id}`,
  },
  {
    name: 'abnormal sign attachment',
    modulePath: '/src/api/abnormal/abnormalContact.ts',
    exportName: 'abnormalContactApi',
    methodName: 'signAttachment',
    args: [id],
    expectMethod: 'POST',
    expectPath: `/abnormal/contact/attachments/${id}/sign`,
  },
  {
    name: 'inventory page',
    modulePath: '/src/api/material-mgmt/inventory.ts',
    exportName: 'inventoryApi',
    methodName: 'page',
    args: [pageQuery],
    expectMethod: 'GET',
    expectPath: '/material/inventory/page',
  },
  {
    name: 'APS log page',
    modulePath: '/src/api/aps/apsSync.ts',
    exportName: 'apsSyncLogApi',
    methodName: 'page',
    args: [pageQuery],
    expectMethod: 'GET',
    expectPath: '/aps/log/page',
  },
  {
    name: 'APS log detail',
    modulePath: '/src/api/aps/apsSync.ts',
    exportName: 'apsSyncLogApi',
    methodName: 'getDetail',
    args: [id],
    expectMethod: 'GET',
    expectPath: `/aps/log/${id}`,
  },
  {
    name: 'APS status',
    modulePath: '/src/api/aps/apsSync.ts',
    exportName: 'apsSyncApi',
    methodName: 'getStatus',
    args: [],
    expectMethod: 'GET',
    expectPath: '/aps/sync/status',
  },
  ...[
    ['triggerDownstream', '/aps/sync/downstream'],
    ['triggerUpstream', '/aps/sync/upstream'],
    ['triggerCompensate', '/aps/sync/compensate'],
  ].map(([methodName, expectPath]) => ({
    name: `APS ${methodName}`,
    modulePath: '/src/api/aps/apsSync.ts',
    exportName: 'apsSyncApi',
    methodName,
    args: [],
    expectMethod: 'POST',
    expectPath,
  })),
  {
    name: 'system role list',
    modulePath: '/src/api/system/role.ts',
    exportName: 'sysRoleApi',
    methodName: 'list',
    args: [],
    expectMethod: 'GET',
    expectPath: '/system/role/list',
  },
  {
    name: 'system role assign menus',
    modulePath: '/src/api/system/role.ts',
    exportName: 'sysRoleApi',
    methodName: 'assignMenus',
    args: [id, [1, 2]],
    expectMethod: 'PUT',
    expectPath: `/system/role/${id}/menus`,
    expectPayload: [1, 2],
  },
  {
    name: 'system role menu ids',
    modulePath: '/src/api/system/role.ts',
    exportName: 'sysRoleApi',
    methodName: 'getRoleMenuIds',
    args: [id],
    expectMethod: 'GET',
    expectPath: `/system/role/${id}/menus`,
  },
  {
    name: 'system user reset password',
    modulePath: '/src/api/system/user.ts',
    exportName: 'sysUserApi',
    methodName: 'resetPassword',
    args: [id],
    expectMethod: 'PUT',
    expectPath: `/system/user/${id}/reset-password`,
  },
  ...[
    ['list', 'GET', '/platform/tenants', [pageQuery]],
    ['getDetail', 'GET', `/platform/tenants/${id}`, [id]],
    ['register', 'POST', '/platform/tenants/register', [{ tenantCode: 'tenant-e2e', tenantName: 'E2E租户' }]],
    ['suspend', 'POST', `/platform/tenants/${id}/suspend`, [id]],
    ['resume', 'POST', `/platform/tenants/${id}/resume`, [id]],
    ['archive', 'POST', `/platform/tenants/${id}/archive`, [id]],
    ['reprovision', 'POST', `/platform/tenants/${id}/reprovision`, [id]],
  ].map(([methodName, expectMethod, expectPath, args]) => ({
    name: `platform tenant ${methodName}`,
    modulePath: '/src/api/platform/tenant.ts',
    exportName: 'platformTenantApi',
    methodName: methodName as string,
    args: args as unknown[],
    expectMethod: expectMethod as string,
    expectPath: expectPath as string,
    ...((methodName === 'register') ? { expectPayload: { tenantCode: 'tenant-e2e', tenantName: 'E2E租户' } } : {}),
  })),
  ...[
    ['workStatusPage', '/query/work-status-view/page', [pageQuery]],
    ['productionWorkPage', '/query/production-work/page', [pageQuery]],
    ['productionWorkDetail', `/query/production-work/${id}`, [id]],
    ['inspectionWorkPage', '/query/inspection-work/page', [pageQuery]],
    ['inspectionWorkDetail', `/query/inspection-work/${id}`, [id]],
    ['workStartCheckPage', '/quality/work-start-check/page', [pageQuery]],
    ['workStartCheckDetail', `/quality/work-start-check/${id}`, [id]],
    ['orderStartCheckPage', '/quality/order-start-check/page', [pageQuery]],
    ['orderStartCheckDetail', `/quality/order-start-check/${id}`, [id]],
    ['shiftHandoverPage', '/quality/shift-handover/page', [pageQuery]],
    ['shiftHandoverDetail', `/quality/shift-handover/${id}`, [id]],
    ['workOrderPage', '/workorder/work-order/page', [pageQuery]],
    ['workOrderDetail', `/workorder/work-order/${id}`, [id]],
    ['dispatchWorkPage', '/dispatch/task/page', [pageQuery]],
    ['dispatchWorkDetail', `/dispatch/task/${id}`, [id]],
  ].map(([methodName, expectPath, args]) => ({
    name: `query ${methodName}`,
    modulePath: '/src/api/query/workQuery.ts',
    exportName: 'workQueryApi',
    methodName: methodName as string,
    args: args as unknown[],
    expectMethod: 'GET',
    expectPath: expectPath as string,
  })),
  ...[
    ['login', 'POST', '/auth/login', [{ username: 'admin', password: 'admin123', loginClient: 'ADMIN' }]],
    ['getCaptcha', 'GET', '/auth/captcha', []],
    ['refresh', 'POST', '/auth/refresh', []],
    ['logout', 'POST', '/auth/logout', []],
    ['getUserInfo', 'GET', '/auth/user-info', []],
  ].map(([methodName, expectMethod, expectPath, args]) => ({
    name: `auth ${methodName}`,
    modulePath: '/src/api/system/auth.ts',
    exportName: 'authApi',
    methodName: methodName as string,
    args: args as unknown[],
    expectMethod: expectMethod as string,
    expectPath: expectPath as string,
    ...((methodName === 'login') ? { expectPayload: { username: 'admin', password: 'admin123', loginClient: 'ADMIN' } } : {}),
  })),
]

const API_CASES = [...crudResources.flatMap(crudCases), ...extraCases]

function collectViewApiCalls() {
  const files = execSync('rg --files src/views -g "*.vue"', { encoding: 'utf8' })
    .trim()
    .split('\n')
    .filter(Boolean)
  const calls = new Set<string>()
  const callPattern = /\b([A-Za-z_]\w*(?:Api|api))\.([A-Za-z_]\w*)\s*\(/g

  for (const file of files) {
    const source = readFileSync(file, 'utf8')
    for (const match of source.matchAll(callPattern)) {
      calls.add(`${match[1]}.${match[2]}`)
    }
  }

  return [...calls].sort()
}

function responseDataFor(path: string) {
  if (path === '/auth/login' || path === '/auth/refresh') {
    return {
      accessToken: 'api-client-token',
      userInfo: { id: 1, username: 'admin', realName: '管理员', tenantId: 0, accountType: 'ADMIN', roles: ['ADMIN'], permissions: ['*:*:*'] },
    }
  }
  if (path === '/auth/user-info') {
    return { id: 1, username: 'admin', realName: '管理员', tenantId: 0, accountType: 'ADMIN', roles: ['ADMIN'], permissions: ['*:*:*'] }
  }
  if (path.endsWith('/page') || path === '/platform/tenants') return { list: [{ id }], total: 1 }
  if (path.endsWith('/tree') || path.endsWith('/list') || path.includes('/assignment/list/') || path.includes('/attachments')) return [{ id }]
  return { id }
}

async function mockApi(page: Page) {
  const calls: ApiCall[] = []
  await page.route('**/*', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (!url.pathname.startsWith('/api/')) {
      await route.fallback()
      return
    }

    let payload: unknown
    try {
      payload = request.postDataJSON()
    } catch {
      payload = undefined
    }

    const path = url.pathname.replace(/^\/api/, '')
    calls.push({
      method: request.method(),
      path,
      query: Object.fromEntries(url.searchParams.entries()),
      payload,
    })

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok(responseDataFor(path))),
    })
  })
  return calls
}

async function invokeApi(page: Page, apiCase: ApiCase) {
  await page.evaluate(async ({ modulePath, exportName, methodName, args }) => {
    const mod = await import(modulePath)
    const api = mod[exportName]
    await api[methodName](...args)
  }, apiCase)
}

test.describe('前端 API 客户端数据流 / API client dataflow', () => {
  test('每个视图引用的 API 方法都有数据流用例覆盖', () => {
    const covered = new Set(API_CASES.map((apiCase) => `${apiCase.exportName}.${apiCase.methodName}`))
    const missing = collectViewApiCalls().filter((call) => !covered.has(call))
    expect(missing, `未覆盖的视图 API 调用:\n${missing.join('\n')}`).toEqual([])
  })

  test('所有前端 API wrapper 都发送正确方法、路径和载荷', async ({ page }) => {
    const calls = await mockApi(page)
    await page.goto('/login')
    calls.length = 0

    for (const apiCase of API_CASES) {
      await test.step(apiCase.name, async () => {
        calls.length = 0
        await invokeApi(page, apiCase)

        const call = calls.find((item) => item.method === apiCase.expectMethod && item.path === apiCase.expectPath)
        expect(call, `${apiCase.expectMethod} ${apiCase.expectPath}`).toBeTruthy()
        if (!call) return

        if (apiCase.expectPayload !== undefined) {
          expect(call.payload).toMatchObject(apiCase.expectPayload as Record<string, unknown>)
        }
        if (apiCase.expectQuery) {
          expect(call.query).toMatchObject(apiCase.expectQuery)
        }
      })
    }
  })
})
