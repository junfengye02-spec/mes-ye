import { expect, test } from './fixtures'

type JsonValue = Record<string, unknown>

type ContractCase = {
  name: string
  path: string
  pageEndpoint: string
  createEndpoint: string
  fields: Record<string, string>
  expected: JsonValue
  forbidden: string[]
}

const ok = (data: unknown) => ({
  code: 200,
  message: 'OK',
  data,
})

const CONTRACT_CASES: ContractCase[] = [
  {
    name: '工作中心',
    path: '/basic/work-center',
    pageEndpoint: '/api/basic/work-center/page',
    createEndpoint: '/api/basic/work-center',
    fields: {
      工作中心编码: 'WC_E2E_001',
      工作中心名称: 'E2E工作中心',
      工作中心分类: 'MACHINING',
      业务单元: 'BU01',
      工作日历: 'CAL_DAY',
      资源排序: '1',
      使用量: '2',
      使用量单位: 'H',
      处理批量: '3',
      效率: '0.95',
      资源种类: 'EQUIPMENT',
      资源子类型: 'CNC',
      资源能力: '8',
    },
    expected: {
      workCenterCode: 'WC_E2E_001',
      workCenterName: 'E2E工作中心',
      workCenterCategory: 'MACHINING',
      businessUnit: 'BU01',
      workCalendar: 'CAL_DAY',
      resourceOrder: 1,
      usageQty: 2,
      usageUnit: 'H',
      batchQty: 3,
      efficiency: 0.95,
      resourceType: 'EQUIPMENT',
      resourceSubtype: 'CNC',
      resourceCapacity: 8,
    },
    forbidden: ['workCenterType', 'factory', 'planOrg', 'capacity', 'capacityUnit', 'enabled'],
  },
  {
    name: '生产班组',
    path: '/team/production-team',
    pageEndpoint: '/api/team/production-team/page',
    createEndpoint: '/api/team/production-team',
    fields: {
      班组编码: 'TEAM_E2E_001',
      班组名称: 'E2E班组',
      生产组织ID: '11',
      生产组织编码: 'ORG_E2E',
      生产组织名称: 'E2E组织',
      说明: '班组说明',
    },
    expected: {
      teamCode: 'TEAM_E2E_001',
      teamName: 'E2E班组',
      orgId: 11,
      orgCode: 'ORG_E2E',
      orgName: 'E2E组织',
      description: '班组说明',
    },
    forbidden: ['teamType', 'leaderId', 'factory', 'workCenterId', 'remark'],
  },
  {
    name: '指示书',
    path: '/process/instruction',
    pageEndpoint: '/api/process/instruction/page',
    createEndpoint: '/api/process/instruction',
    fields: {
      指示书号: 'INS_E2E_001',
      项目编号: 'PRJ_E2E',
      WBS: 'WBS_E2E',
      产品类别: 'CAT_A',
      产品类型: 'TYPE_A',
      生产订单编号: 'WO_E2E',
      数量: '5',
      备注: '指示书备注',
    },
    expected: {
      instructionNo: 'INS_E2E_001',
      projectNo: 'PRJ_E2E',
      wbs: 'WBS_E2E',
      productCategory: 'CAT_A',
      productType: 'TYPE_A',
      workOrderNo: 'WO_E2E',
      qty: 5,
      remark: '指示书备注',
    },
    forbidden: ['instructionCode', 'instructionName', 'productCode', 'productName'],
  },
  {
    name: '喷涂条件',
    path: '/process/spray-condition',
    pageEndpoint: '/api/process/spray-condition/page',
    createEndpoint: '/api/process/spray-condition',
    fields: {
      条件号: 'SC_E2E_001',
      部长审批人: '部长',
      工段审批人: '工段',
      系长审批人: '系长',
      '送粉量(g/min)': '10.5',
      '喷涂距离(mm)': '120',
      喷枪型号: 'GUN-A',
      设备: 'EQ-A',
      对应粉末: 'POWDER-A',
    },
    expected: {
      conditionNo: 'SC_E2E_001',
      ministerApprover: '部长',
      sectionApprover: '工段',
      leaderApprover: '系长',
      powderFeedRate: 10.5,
      sprayDistance: 120,
      sprayGunModel: 'GUN-A',
      equipment: 'EQ-A',
      powderType: 'POWDER-A',
    },
    forbidden: ['conditionCode', 'conditionName', 'temperature', 'humidity', 'paintType', 'thickness'],
  },
  {
    name: '加工程序',
    path: '/process/machining-program',
    pageEndpoint: '/api/process/machining-program/page',
    createEndpoint: '/api/process/machining-program',
    fields: {
      'G-code': 'G01 X1',
      程序表: 'N10 G01 X1',
      产品名称: 'E2E产品',
    },
    expected: {
      gCode: 'G01 X1',
      programTable: 'N10 G01 X1',
      productName: 'E2E产品',
    },
    forbidden: ['programCode', 'programName', 'machineType', 'programContent', 'version'],
  },
  {
    name: '工序信息',
    path: '/process/info',
    pageEndpoint: '/api/process/process-info/page',
    createEndpoint: '/api/process/process-info',
    fields: {
      工序号: 'PROCNO_E2E',
      工序名: 'E2E工序',
      工艺编码: 'P_E2E',
      产品: 'E2E产品',
      产品类别: 'CAT_A',
      工序类型: 'MACHINING',
      工作中心ID: '1',
      处理时间: '2.5',
      说明: '工序说明',
    },
    expected: {
      processNo: 'PROCNO_E2E',
      processName: 'E2E工序',
      processCode: 'P_E2E',
      product: 'E2E产品',
      productCategory: 'CAT_A',
      processType: 'MACHINING',
      workCenterId: 1,
      handleTime: 2.5,
      remark: '工序说明',
    },
    forbidden: ['standardTime', 'timeUnit', 'description'],
  },
  {
    name: '配送签收',
    path: '/material-mgmt/delivery-sign',
    pageEndpoint: '/api/material/delivery-sign/page',
    createEndpoint: '/api/material/delivery-sign',
    fields: {
      行号: '10',
      工单ID: '100',
      工单号: 'WO_E2E',
      物料ID: '200',
      物料编码: 'MAT_E2E',
      物料名称: 'E2E物料',
      计划发货数量: '7',
      待签收数量: '6',
      单位: 'PCS',
      发货仓库: 'WH-A',
      发货存储地点: 'LOC-A',
    },
    expected: {
      lineNo: '10',
      workOrderId: 100,
      workOrderNo: 'WO_E2E',
      materialId: 200,
      materialCode: 'MAT_E2E',
      materialName: 'E2E物料',
      planDeliveryQty: 7,
      pendingSignQty: 6,
      unit: 'PCS',
      deliveryWarehouse: 'WH-A',
      deliveryLocation: 'LOC-A',
    },
    forbidden: ['orderNo', 'productCode', 'productName', 'deliveryQty', 'qtyUnit', 'deliveryDate'],
  },
  {
    name: '退料单',
    path: '/material-mgmt/return',
    pageEndpoint: '/api/material/return/page',
    createEndpoint: '/api/material/return',
    fields: {
      工单ID: '100',
      工单号: 'WO_E2E',
      订单号: 'ORDER_E2E',
      产品编码: 'PROD_E2E',
      产品名称: 'E2E产品',
      项目: 'E2E项目',
      WBS元素: 'WBS_E2E',
      新制维修类型: 'NEW',
      业务类型: 'MANUFACTURE',
      流程编码: 'FLOW_E2E',
      计划数量: '9',
      完工数量: '8',
    },
    expected: {
      workOrderId: 100,
      workOrderNo: 'WO_E2E',
      orderNo: 'ORDER_E2E',
      productCode: 'PROD_E2E',
      productName: 'E2E产品',
      projectName: 'E2E项目',
      wbsElement: 'WBS_E2E',
      newOrRepairType: 'NEW',
      businessType: 'MANUFACTURE',
      flowCode: 'FLOW_E2E',
      planQty: 9,
      completedQty: 8,
    },
    forbidden: ['materialId', 'materialCode', 'materialName', 'returnQty', 'qtyUnit', 'returnReason'],
  },
]

async function mockAuthenticatedShell(page: import('@playwright/test').Page, currentCase: ContractCase) {
  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'e2e-contract-token')
  })

  await page.route('**/api/auth/user-info', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({
        id: 1,
        username: 'admin',
        realName: '管理员',
        tenantId: 1,
        accountType: 'ADMIN',
        roles: ['ADMIN'],
        permissions: ['*:*:*'],
      })),
    }),
  )
  await page.route('**/api/system/menu/user-tree', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok([])) }),
  )
  await page.route(`**${currentCase.pageEndpoint}**`, (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({ list: [], total: 0 })),
    }),
  )
}

async function fillForm(page: import('@playwright/test').Page, fields: Record<string, string>) {
  const dialog = page.locator('.el-dialog').filter({ hasText: /新增|新建|编辑/ })
  await expect(dialog).toBeVisible()

  for (const [label, value] of Object.entries(fields)) {
    const item = dialog
      .locator('.el-form-item')
      .filter({ has: page.locator('.el-form-item__label', { hasText: new RegExp(`^${escapeRegExp(label)}$`) }) })
    await expect(item, `表单项 ${label}`).toBeVisible()
    const input = item.locator('input, textarea').first()
    await expect(input, `表单项 ${label} 输入框`).toBeVisible()
    await input.fill(value)
  }
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function submitAndCapture(page: import('@playwright/test').Page, endpoint: string) {
  let payload: JsonValue | null = null
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() === 'POST') {
      payload = JSON.parse(route.request().postData() || '{}')
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok(1)) })
      return
    }
    await route.fallback()
  })

  const submit = page.locator('.el-dialog').getByRole('button', { name: '确定' })
  await expect(submit).toBeVisible()
  await submit.click()
  await expect.poll(() => payload, { message: `${endpoint} POST payload` }).not.toBeNull()
  return payload!
}

test.describe('前端表单写入 / DTO contract dataflow', () => {
  for (const contractCase of CONTRACT_CASES) {
    test(`${contractCase.name} 新增表单提交后端契约字段`, async ({ page }) => {
      await mockAuthenticatedShell(page, contractCase)

      await page.goto(contractCase.path)
      await expect(page.locator('main')).toBeVisible()
      await page.getByRole('button', { name: /新增/ }).click()
      await fillForm(page, contractCase.fields)

      const payload = await submitAndCapture(page, contractCase.createEndpoint)

      expect(payload).toMatchObject(contractCase.expected)
      for (const key of contractCase.forbidden) {
        expect(payload, `${contractCase.name} payload 不应包含旧字段 ${key}`).not.toHaveProperty(key)
      }
    })
  }
})
