<template>
  <div class="requisition-list">
    <el-card shadow="never" class="search-card" role="search" aria-label="生产领料查询条件">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="领料单号">
          <el-input v-model="query.requisitionNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('requisitionStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon aria-hidden="true"><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon aria-hidden="true"><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header" role="toolbar" aria-label="生产领料操作工具栏">
          <span>生产领料列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon aria-hidden="true"><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        role="region"
        aria-label="生产领料列表"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="requisitionNo" label="领料单号" min-width="140" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="130" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="150" />
        <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
        <el-table-column prop="actualQty" label="实际数量" width="100" align="right" />
        <el-table-column prop="qualifiedQty" label="合格数量" width="100" align="right" />
        <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
        <el-table-column prop="mainOrg" label="主制组织" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('requisitionStatus', row.status || '') as any">
              {{ getDictLabel('requisitionStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看明细</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <nav class="pagination-wrapper" aria-label="生产领料分页">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </nav>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="1100px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="领料单号" prop="requisitionNo">
              <el-input v-model="form.requisitionNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单" prop="workOrderId">
              <el-select
                v-model="form.workOrderId"
                placeholder="请选择工单"
                filterable
                remote
                :remote-method="searchWorkOrders"
                :loading="workOrderLoading"
                style="width: 100%"
                @change="onWorkOrderChange"
              >
                <el-option
                  v-for="wo in workOrderOptions"
                  :key="wo.id"
                  :label="`${wo.workOrderNo} - ${wo.productCode || ''}`"
                  :value="wo.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单号" prop="workOrderNo">
              <el-input v-model="form.workOrderNo" placeholder="选择工单后自动带出" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品编码" prop="productCode">
              <el-input v-model="form.productCode" placeholder="请输入产品编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="请输入产品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主制组织" prop="mainOrg">
              <el-input v-model="form.mainOrg" placeholder="请输入主制组织" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划数量" prop="planQty">
              <el-input-number v-model="form.planQty" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="实际数量" prop="actualQty">
              <el-input-number v-model="form.actualQty" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格数量" prop="qualifiedQty">
              <el-input-number v-model="form.qualifiedQty" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量单位" prop="qtyUnit">
              <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售订单行" prop="salesOrderLine">
              <el-input v-model="form.salesOrderLine" placeholder="请输入销售订单行" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划开始时间" prop="planStartTime">
              <el-date-picker
                v-model="form.planStartTime"
                type="datetime"
                placeholder="选择计划开始时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束时间" prop="planEndTime">
              <el-date-picker
                v-model="form.planEndTime"
                type="datetime"
                placeholder="选择计划结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际开始时间" prop="actualStartTime">
              <el-date-picker
                v-model="form.actualStartTime"
                type="datetime"
                placeholder="选择实际开始时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际结束时间" prop="actualEndTime">
              <el-date-picker
                v-model="form.actualEndTime"
                type="datetime"
                placeholder="选择实际结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入项目" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="WBS元素" prop="wbsElement">
              <el-input v-model="form.wbsElement" placeholder="请输入WBS元素" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">领料明细</el-divider>

        <el-table :data="form.items" border stripe size="small" empty-text="请点击下方按钮添加领料明细">
          <el-table-column label="物料" min-width="220">
            <template #default="{ row }">
              <el-select
                v-model="row.materialId"
                placeholder="请选择物料"
                filterable
                remote
                :remote-method="searchMaterials"
                :loading="materialLoading"
                style="width: 100%"
                @change="(value) => onItemMaterialChange(row, value as number | undefined)"
              >
                <el-option
                  v-for="material in materialOptions"
                  :key="material.id"
                  :label="`${material.materialCode} - ${material.materialName}`"
                  :value="material.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="需求数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.demandQty" :min="0.0001" :precision="4" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="本次领料" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.issueQty" :min="0.0001" :precision="4" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="90">
            <template #default="{ row }">
              <el-input v-model="row.unit" placeholder="单位" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="发货地点" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.issueLocation" placeholder="发货地点" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="需求时间" width="180">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.demandTime"
                type="datetime"
                placeholder="需求时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.description" placeholder="说明" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link size="small" @click="removeItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="item-action-row">
          <el-button type="primary" link @click="addItem">
            <el-icon aria-hidden="true"><Plus /></el-icon> 添加明细
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="领料明细" width="1100px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="领料单号">{{ viewData.requisitionNo }}</el-descriptions-item>
        <el-descriptions-item label="工单号">{{ viewData.workOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="产品编码">{{ viewData.productCode }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ viewData.productName }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ viewData.planQty }}</el-descriptions-item>
        <el-descriptions-item label="实际数量">{{ viewData.actualQty }}</el-descriptions-item>
        <el-descriptions-item label="合格数量">{{ viewData.qualifiedQty }}</el-descriptions-item>
        <el-descriptions-item label="数量单位">{{ viewData.qtyUnit }}</el-descriptions-item>
        <el-descriptions-item label="主制组织">{{ viewData.mainOrg }}</el-descriptions-item>
        <el-descriptions-item label="销售订单行">{{ viewData.salesOrderLine }}</el-descriptions-item>
        <el-descriptions-item label="计划开始时间">{{ viewData.planStartTime }}</el-descriptions-item>
        <el-descriptions-item label="计划结束时间">{{ viewData.planEndTime }}</el-descriptions-item>
        <el-descriptions-item label="实际开始时间">{{ viewData.actualStartTime }}</el-descriptions-item>
        <el-descriptions-item label="实际结束时间">{{ viewData.actualEndTime }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ viewData.projectName }}</el-descriptions-item>
        <el-descriptions-item label="WBS元素">{{ viewData.wbsElement }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getDictLabel('requisitionStatus', viewData.status || '') }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createdTime }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">明细（{{ viewData.items?.length || 0 }} 条）</el-divider>
      <el-table :data="viewData.items || []" border stripe size="small">
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="demandQty" label="需求数量" width="110" align="right" />
        <el-table-column prop="pendingQty" label="待领数量" width="110" align="right" />
        <el-table-column prop="issueQty" label="本次领料" width="110" align="right" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="issueLocation" label="发货地点" min-width="120" />
        <el-table-column prop="demandTime" label="需求时间" width="170" />
        <el-table-column prop="description" label="说明" min-width="150" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { materialApi } from '@/api/basic/material'
import { requisitionApi } from '@/api/material-mgmt/requisition'
import { workOrderApi } from '@/api/workorder/workOrder'
import type { MaterialVO } from '@/types/basic'
import type {
  RequisitionDTO,
  RequisitionItemDTO,
  RequisitionQuery,
  RequisitionVO,
} from '@/types/material-mgmt'
import type { WorkOrderInputMaterialVO, WorkOrderVO } from '@/types/workorder'
import { getDictLabel, getDictList, getDictType } from '@/utils/dict'

type RequisitionForm = RequisitionDTO & {
  items: RequisitionItemDTO[]
}

function createEmptyItem(): RequisitionItemDTO {
  return {
    materialId: undefined,
    materialCode: '',
    materialName: '',
    demandQty: undefined,
    issueQty: undefined,
    unit: '',
    issueLocation: '',
    demandTime: '',
    description: '',
  }
}

function createEmptyForm(): RequisitionForm {
  return {
    requisitionNo: '',
    workOrderId: undefined,
    workOrderNo: '',
    productCode: '',
    productName: '',
    planQty: undefined,
    actualQty: undefined,
    qualifiedQty: undefined,
    qtyUnit: '',
    mainOrg: '',
    planStartTime: '',
    planEndTime: '',
    actualStartTime: '',
    actualEndTime: '',
    salesOrderLine: '',
    projectName: '',
    wbsElement: '',
    items: [],
  }
}

function createEmptyViewData(): RequisitionVO {
  return {
    id: 0,
    requisitionNo: '',
    workOrderId: undefined,
    workOrderNo: '',
    productCode: '',
    productName: '',
    planQty: undefined,
    actualQty: undefined,
    qualifiedQty: undefined,
    qtyUnit: '',
    mainOrg: '',
    planStartTime: '',
    planEndTime: '',
    actualStartTime: '',
    actualEndTime: '',
    salesOrderLine: '',
    projectName: '',
    wbsElement: '',
    status: '',
    items: [],
    createdBy: '',
    createdTime: '',
  }
}

const loading = ref(false)
const list = ref<RequisitionVO[]>([])
const total = ref(0)
const query = reactive<RequisitionQuery>({
  requisitionNo: '',
  workOrderNo: '',
  productCode: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增领料')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const form = reactive<RequisitionForm>(createEmptyForm())

const workOrderOptions = ref<WorkOrderVO[]>([])
const materialOptions = ref<MaterialVO[]>([])
const workOrderLoading = ref(false)
const materialLoading = ref(false)

const viewVisible = ref(false)
const viewData = reactive<RequisitionVO>(createEmptyViewData())

const rules: FormRules = {
  workOrderId: [{ required: true, message: '请选择工单', trigger: 'change' }],
}

function mergeWorkOrderOptions(records: WorkOrderVO[]) {
  const merged = new Map<number, WorkOrderVO>()
  for (const record of workOrderOptions.value) {
    merged.set(record.id, record)
  }
  for (const record of records) {
    merged.set(record.id, record)
  }
  workOrderOptions.value = Array.from(merged.values())
}

function mergeMaterialOptions(records: MaterialVO[]) {
  const merged = new Map<number, MaterialVO>()
  for (const record of materialOptions.value) {
    merged.set(record.id, record)
  }
  for (const record of records) {
    merged.set(record.id, record)
  }
  materialOptions.value = Array.from(merged.values())
}

function blankToUndefined(value?: string) {
  return value && value.trim() ? value : undefined
}

function calculateRemainingQty(requiredQty?: number, issuedQty?: number) {
  const required = requiredQty ?? 0
  const issued = issuedQty ?? 0
  return Math.max(required - issued, 0)
}

function applyWorkOrderHeader(workOrder: WorkOrderVO) {
  form.workOrderId = workOrder.id
  form.workOrderNo = workOrder.workOrderNo
  form.productCode = workOrder.productCode ?? ''
  form.productName = workOrder.productName ?? ''
  form.planQty = workOrder.planQty
  form.qtyUnit = workOrder.qtyUnit ?? ''
  form.mainOrg = workOrder.mainOrg ?? ''
  form.planStartTime = workOrder.planStartTime ?? ''
  form.planEndTime = workOrder.planEndTime ?? ''
  form.actualStartTime = workOrder.actualStartTime ?? form.actualStartTime ?? ''
  form.actualEndTime = workOrder.actualEndTime ?? form.actualEndTime ?? ''
  form.projectName = workOrder.projectName ?? ''
  form.wbsElement = workOrder.wbsElement ?? ''
}

function mapInputMaterialsToItems(inputMaterials: WorkOrderInputMaterialVO[], workOrder?: WorkOrderVO) {
  return inputMaterials
    .filter((item) => item.materialId)
    .map((item) => {
      const remainingQty = calculateRemainingQty(item.requiredQty, item.issuedQty)
      return {
        materialId: item.materialId,
        materialCode: item.materialCode ?? '',
        materialName: item.materialName ?? '',
        demandQty: remainingQty || item.requiredQty,
        issueQty: remainingQty || item.requiredQty,
        unit: item.qtyUnit ?? workOrder?.qtyUnit ?? '',
        issueLocation: '',
        demandTime: workOrder?.planStartTime ?? '',
        description: '',
      } satisfies RequisitionItemDTO
    })
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function resetViewData() {
  Object.assign(viewData, createEmptyViewData())
}

async function searchWorkOrders(keyword: string) {
  if (!keyword) return
  workOrderLoading.value = true
  try {
    const res = await workOrderApi.page({ workOrderNo: keyword, pageNum: 1, pageSize: 20 })
    mergeWorkOrderOptions(res?.list ?? [])
  } finally {
    workOrderLoading.value = false
  }
}

async function searchMaterials(keyword: string) {
  if (!keyword) return
  materialLoading.value = true
  try {
    const res = await materialApi.page({ materialCode: keyword, materialName: keyword, pageNum: 1, pageSize: 20 })
    mergeMaterialOptions(res?.list ?? [])
  } finally {
    materialLoading.value = false
  }
}

async function onWorkOrderChange(workOrderId?: number) {
  if (!workOrderId) {
    form.workOrderNo = ''
    return
  }

  const selected = workOrderOptions.value.find((item) => item.id === workOrderId)
  if (selected) {
    applyWorkOrderHeader(selected)
  }

  const detail = await workOrderApi.getDetail(workOrderId)
  mergeWorkOrderOptions([detail])
  applyWorkOrderHeader(detail)

  if (form.items.length === 0 && detail.inputMaterials?.length) {
    form.items = mapInputMaterialsToItems(detail.inputMaterials, detail)
    mergeMaterialOptions(
      detail.inputMaterials
        .filter((item) => item.materialId)
        .map((item) => ({
          id: item.materialId!,
          materialCode: item.materialCode ?? '',
          materialName: item.materialName ?? '',
          baseUnit: item.qtyUnit ?? '',
        }) as MaterialVO),
    )
  }
}

function onItemMaterialChange(row: RequisitionItemDTO, materialId?: number) {
  if (!materialId) {
    row.materialCode = ''
    row.materialName = ''
    return
  }

  const selected = materialOptions.value.find((item) => item.id === materialId)
  row.materialCode = selected?.materialCode ?? ''
  row.materialName = selected?.materialName ?? ''
  if (!row.unit) {
    row.unit = selected?.baseUnit ?? ''
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await requisitionApi.page(query)
    list.value = res?.list ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.requisitionNo = ''
  query.workOrderNo = ''
  query.productCode = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增领料'
  editId.value = null
  resetForm()
  workOrderOptions.value = []
  materialOptions.value = []
  dialogVisible.value = true
}

async function handleEdit(row: RequisitionVO) {
  dialogTitle.value = '编辑领料'
  editId.value = row.id
  const detail = await requisitionApi.getDetail(row.id)
  resetForm()
  Object.assign(form, {
    requisitionNo: detail.requisitionNo ?? '',
    workOrderId: detail.workOrderId,
    workOrderNo: detail.workOrderNo ?? '',
    productCode: detail.productCode ?? '',
    productName: detail.productName ?? '',
    planQty: detail.planQty,
    actualQty: detail.actualQty,
    qualifiedQty: detail.qualifiedQty,
    qtyUnit: detail.qtyUnit ?? '',
    mainOrg: detail.mainOrg ?? '',
    planStartTime: detail.planStartTime ?? '',
    planEndTime: detail.planEndTime ?? '',
    actualStartTime: detail.actualStartTime ?? '',
    actualEndTime: detail.actualEndTime ?? '',
    salesOrderLine: detail.salesOrderLine ?? '',
    projectName: detail.projectName ?? '',
    wbsElement: detail.wbsElement ?? '',
    items: (detail.items ?? []).map((item) => ({
      materialId: item.materialId,
      materialCode: item.materialCode ?? '',
      materialName: item.materialName ?? '',
      demandQty: item.demandQty,
      issueQty: item.issueQty,
      unit: item.unit ?? '',
      issueLocation: item.issueLocation ?? '',
      demandTime: item.demandTime ?? '',
      description: item.description ?? '',
    })),
  })

  if (detail.workOrderId) {
    mergeWorkOrderOptions([{ id: detail.workOrderId, workOrderNo: detail.workOrderNo ?? '' } as WorkOrderVO])
  }
  mergeMaterialOptions(
    (detail.items ?? [])
      .filter((item) => item.materialId)
      .map((item) => ({
        id: item.materialId!,
        materialCode: item.materialCode ?? '',
        materialName: item.materialName ?? '',
        baseUnit: item.unit ?? '',
      }) as MaterialVO),
  )
  dialogVisible.value = true
}

async function handleView(row: RequisitionVO) {
  const detail = await requisitionApi.getDetail(row.id)
  resetViewData()
  Object.assign(viewData, detail)
  viewVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

function addItem() {
  form.items.push(createEmptyItem())
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

function validateItems() {
  if (form.items.length === 0) {
    ElMessage.warning('请至少添加一条领料明细')
    return false
  }

  for (let index = 0; index < form.items.length; index += 1) {
    const item = form.items[index]
    if (!item.materialId) {
      ElMessage.warning(`第 ${index + 1} 条明细请选择物料`)
      return false
    }
    if (!item.demandQty || item.demandQty <= 0) {
      ElMessage.warning(`第 ${index + 1} 条明细请输入需求数量`)
      return false
    }
    if (item.issueQty !== undefined && item.issueQty <= 0) {
      ElMessage.warning(`第 ${index + 1} 条明细请输入有效的本次领料数量`)
      return false
    }
  }

  return true
}

function buildPayload(): RequisitionDTO {
  return {
    requisitionNo: blankToUndefined(form.requisitionNo),
    workOrderId: form.workOrderId,
    workOrderNo: blankToUndefined(form.workOrderNo),
    productCode: blankToUndefined(form.productCode),
    productName: blankToUndefined(form.productName),
    planQty: form.planQty,
    actualQty: form.actualQty,
    qualifiedQty: form.qualifiedQty,
    qtyUnit: blankToUndefined(form.qtyUnit),
    mainOrg: blankToUndefined(form.mainOrg),
    planStartTime: blankToUndefined(form.planStartTime),
    planEndTime: blankToUndefined(form.planEndTime),
    actualStartTime: blankToUndefined(form.actualStartTime),
    actualEndTime: blankToUndefined(form.actualEndTime),
    salesOrderLine: blankToUndefined(form.salesOrderLine),
    projectName: blankToUndefined(form.projectName),
    wbsElement: blankToUndefined(form.wbsElement),
    items: form.items.map((item) => ({
      materialId: item.materialId,
      materialCode: blankToUndefined(item.materialCode),
      materialName: blankToUndefined(item.materialName),
      demandQty: item.demandQty,
      issueQty: item.issueQty,
      unit: blankToUndefined(item.unit),
      issueLocation: blankToUndefined(item.issueLocation),
      demandTime: blankToUndefined(item.demandTime),
      description: blankToUndefined(item.description),
    })),
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (!validateItems()) return

  submitLoading.value = true
  try {
    const payload = buildPayload()
    if (editId.value) {
      await requisitionApi.update(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await requisitionApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: RequisitionVO) {
  ElMessageBox.confirm('确定要删除该领料单吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await requisitionApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.requisition-list {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-action-row {
  margin-top: 8px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
