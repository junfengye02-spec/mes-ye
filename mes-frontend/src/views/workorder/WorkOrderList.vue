<template>
  <div class="work-order-list">
    <el-card shadow="never" class="search-card" role="search" :aria-label="t('common.a11y.searchForm')">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="query.productName" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('workOrderStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker
            v-model="query.planTimeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon aria-hidden="true"><Search /></el-icon> {{ t('buttons.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon aria-hidden="true"><Refresh /></el-icon> {{ t('buttons.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header" role="toolbar" :aria-label="t('workorder.listTitle')">
          <span>{{ t('workorder.listTitle') }}</span>
          <el-button v-auth="['workorder:workorder:create']" type="primary" @click="handleAdd">
            <el-icon aria-hidden="true"><Plus /></el-icon> {{ t('buttons.add') }}
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        role="region"
        :aria-label="t('workorder.listTitle')"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="workOrderType" label="工单类型" width="100" />
        <el-table-column prop="productCode" label="产品编码" min-width="100" />
        <el-table-column prop="productName" label="产品名称" min-width="120" />
        <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
        <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('workOrderStatus', row.status || '') as any">
              {{ getDictLabel('workOrderStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planStartTime" label="计划开始时间" width="170" />
        <el-table-column prop="planEndTime" label="计划结束时间" width="170" />
        <el-table-column label="操作" width="380" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-auth="['workorder:workorder:detail']" type="primary" link size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
            <el-button v-auth="['workorder:workorder:update']" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              v-auth="['workorder:workorder:release']"
              type="primary"
              link
              size="small"
              @click="handleRelease(row)"
            >
              下发
            </el-button>
            <el-button
              v-if="row.status === 'RELEASED'"
              v-auth="['workorder:workorder:start']"
              type="success"
              link
              size="small"
              @click="handleStart(row)"
            >
              开工
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              v-auth="['workorder:workorder:complete']"
              type="success"
              link
              size="small"
              @click="handleComplete(row)"
            >
              完工
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              v-auth="['workorder:workorder:forceComplete']"
              type="warning"
              link
              size="small"
              @click="handleForceComplete(row)"
            >
              强制完工
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <nav class="pagination-wrapper" :aria-label="t('common.a11y.pagination')">
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
      width="900px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工单号" prop="workOrderNo">
              <el-input v-model="form.workOrderNo" placeholder="请输入工单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单类型" prop="workOrderType">
              <el-input v-model="form.workOrderType" placeholder="请输入工单类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生产计划号" prop="productionPlanNo">
              <el-input v-model="form.productionPlanNo" placeholder="请输入生产计划号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单计划号" prop="orderPlanNo">
              <el-input v-model="form.orderPlanNo" placeholder="请输入订单计划号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单编号" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="请输入订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新制/维修类型" prop="newOrRepairType">
              <el-input v-model="form.newOrRepairType" placeholder="新制 / 维修" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="workType">
              <el-input v-model="form.workType" placeholder="主机 / 维修 / 检查" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="BOM编码" prop="bomCode">
              <el-input v-model="form.bomCode" placeholder="请输入BOM编码" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">产品信息</el-divider>
        <el-row :gutter="16">
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
            <el-form-item label="主产品" prop="mainProduct">
              <el-input v-model="form.mainProduct" placeholder="请输入主产品" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机型" prop="machineModel">
              <el-input v-model="form.machineModel" placeholder="请输入机型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品类别" prop="productCategory">
              <el-input v-model="form.productCategory" placeholder="请输入产品类别" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品类型" prop="productType">
              <el-input v-model="form.productType" placeholder="请输入产品类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划数量" prop="planQty">
              <el-input-number v-model="form.planQty" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="qtyUnit">
              <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">项目信息</el-divider>
        <el-row :gutter="16">
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
          <el-col :span="12">
            <el-form-item label="序列号" prop="serialNo">
              <el-input v-model="form.serialNo" placeholder="请输入序列号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="特殊库存标识" prop="specialStockFlag">
              <el-input v-model="form.specialStockFlag" placeholder="请输入特殊库存标识" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="交货地点" prop="deliveryLocation">
              <el-input v-model="form.deliveryLocation" placeholder="请输入交货地点" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">组织与工作中心</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工厂组织" prop="factoryOrg">
              <el-input v-model="form.factoryOrg" placeholder="请输入工厂组织" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划组织" prop="planOrg">
              <el-input v-model="form.planOrg" placeholder="请输入计划组织" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主制组织" prop="mainOrg">
              <el-input v-model="form.mainOrg" placeholder="请输入主制组织" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划工作中心ID" prop="planWorkCenterId">
              <el-input-number v-model="form.planWorkCenterId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指定工作中心ID" prop="specifiedWorkCenterId">
              <el-input-number v-model="form.specifiedWorkCenterId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">计划时间</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划开始时间" prop="planStartTime">
              <el-date-picker
                v-model="form.planStartTime"
                type="datetime"
                placeholder="选择开始时间"
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
                placeholder="选择结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 子表数据（本版先不开放前端录入，提交时传空数组，由后端维持与 DTO 结构一致） -->
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 8px"
          title="子表（工作清单 / 输入物料 / 输出物料 / 检验项目 / 约束关系 / 供应计划）提示"
          description="本页面新增/编辑仅维护工单主表；子表数据请在工单详情页或对应模块维护。类型定义已完整对齐后端 DTO，提交时默认以空数组传递。"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="forceCompleteVisible" title="强制完工" width="450px">
      <el-form ref="forceCompleteFormRef" :model="forceCompleteForm" :rules="forceCompleteRules" label-width="80px">
        <el-form-item label="原因" prop="reason">
          <el-input
            v-model="forceCompleteForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入强制完工原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forceCompleteVisible = false">取消</el-button>
        <el-button type="primary" :loading="forceCompleteLoading" @click="handleForceCompleteSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { WorkOrderVO, WorkOrderDTO, WorkOrderQuery } from '@/types/workorder'
import { workOrderApi } from '@/api/workorder/workOrder'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const list = ref<WorkOrderVO[]>([])
const total = ref(0)
const query = reactive<WorkOrderQuery & { planTimeRange?: [string, string] }>({
  workOrderNo: '',
  productCode: '',
  productName: '',
  status: '',
  planStartTime: '',
  planEndTime: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增工单')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
// 与后端 WorkOrderDTO 完整对齐（34 字段 + 6 子表）
const form = reactive<WorkOrderDTO>({
  workOrderNo: '',
  workOrderType: '',
  productionPlanNo: '',
  orderPlanNo: '',
  orderNo: '',
  productCode: '',
  productName: '',
  mainProduct: '',
  machineModel: '',
  productCategory: '',
  productType: '',
  bomCode: '',
  projectName: '',
  wbsElement: '',
  newOrRepairType: '',
  workType: '',
  planQty: 1,
  qtyUnit: '',
  factoryOrg: '',
  planOrg: '',
  mainOrg: '',
  planWorkCenterId: undefined,
  specifiedWorkCenterId: undefined,
  serialNo: '',
  specialStockFlag: '',
  deliveryLocation: '',
  remark: '',
  planStartTime: '',
  planEndTime: '',
  tasks: [],
  inputMaterials: [],
  outputMaterials: [],
  qualityItems: [],
  constraints: [],
  supplyPlans: [],
})

const rules: FormRules = {
  workOrderNo: [{ required: true, message: '请输入工单号', trigger: 'blur' }],
}

const forceCompleteVisible = ref(false)
const forceCompleteFormRef = ref<FormInstance>()
const forceCompleteLoading = ref(false)
const forceCompleteForm = reactive({ reason: '' })
const forceCompleteRules: FormRules = {
  reason: [{ required: true, message: '请输入强制完工原因', trigger: 'blur' }],
}
let forceCompleteRow: WorkOrderVO | null = null

async function loadList() {
  loading.value = true
  try {
    const { planTimeRange, ...params } = query
    const res = await workOrderApi.page({
      ...params,
      planStartTime: planTimeRange?.[0],
      planEndTime: planTimeRange?.[1],
    })
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
  query.workOrderNo = ''
  query.productCode = ''
  query.productName = ''
  query.status = ''
  query.planTimeRange = undefined
  query.pageNum = 1
  loadList()
}

function handleViewDetail(row: WorkOrderVO) {
  router.push(`/workorder/detail/${row.id}`)
}

function resetForm() {
  Object.assign(form, {
    workOrderNo: '',
    workOrderType: '',
    productionPlanNo: '',
    orderPlanNo: '',
    orderNo: '',
    productCode: '',
    productName: '',
    mainProduct: '',
    machineModel: '',
    productCategory: '',
    productType: '',
    bomCode: '',
    projectName: '',
    wbsElement: '',
    newOrRepairType: '',
    workType: '',
    planQty: 1,
    qtyUnit: '',
    factoryOrg: '',
    planOrg: '',
    mainOrg: '',
    planWorkCenterId: undefined,
    specifiedWorkCenterId: undefined,
    serialNo: '',
    specialStockFlag: '',
    deliveryLocation: '',
    remark: '',
    planStartTime: '',
    planEndTime: '',
    tasks: [],
    inputMaterials: [],
    outputMaterials: [],
    qualityItems: [],
    constraints: [],
    supplyPlans: [],
  })
}

function handleAdd() {
  dialogTitle.value = '新增工单'
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: WorkOrderVO) {
  dialogTitle.value = '编辑工单'
  editId.value = row.id
  resetForm()
  Object.assign(form, {
    workOrderNo: row.workOrderNo,
    workOrderType: row.workOrderType ?? '',
    productionPlanNo: row.productionPlanNo ?? '',
    orderPlanNo: row.orderPlanNo ?? '',
    orderNo: row.orderNo ?? '',
    productCode: row.productCode ?? '',
    productName: row.productName ?? '',
    mainProduct: row.mainProduct ?? '',
    machineModel: row.machineModel ?? '',
    productCategory: row.productCategory ?? '',
    productType: row.productType ?? '',
    bomCode: row.bomCode ?? '',
    projectName: row.projectName ?? '',
    wbsElement: row.wbsElement ?? '',
    newOrRepairType: row.newOrRepairType ?? '',
    workType: row.workType ?? '',
    planQty: row.planQty ?? 1,
    qtyUnit: row.qtyUnit ?? '',
    factoryOrg: row.factoryOrg ?? '',
    planOrg: row.planOrg ?? '',
    mainOrg: row.mainOrg ?? '',
    planWorkCenterId: row.planWorkCenterId,
    specifiedWorkCenterId: row.specifiedWorkCenterId,
    serialNo: row.serialNo ?? '',
    specialStockFlag: row.specialStockFlag ?? '',
    deliveryLocation: row.deliveryLocation ?? '',
    remark: row.remark ?? '',
    planStartTime: row.planStartTime ?? '',
    planEndTime: row.planEndTime ?? '',
    tasks: [],
    inputMaterials: [],
    outputMaterials: [],
    qualityItems: [],
    constraints: [],
    supplyPlans: [],
  })
  dialogVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editId.value) {
      await workOrderApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await workOrderApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleRelease(row: WorkOrderVO) {
  ElMessageBox.confirm('确定要下发该工单吗？', '提示', {
    type: 'info',
  }).then(async () => {
    await workOrderApi.release(row.id)
    ElMessage.success('下发成功')
    loadList()
  }).catch(() => {})
}

function handleStart(row: WorkOrderVO) {
  ElMessageBox.confirm('确定要开工该工单吗？', '提示', {
    type: 'info',
  }).then(async () => {
    await workOrderApi.start(row.id)
    ElMessage.success('开工成功')
    loadList()
  }).catch(() => {})
}

function handleComplete(row: WorkOrderVO) {
  ElMessageBox.confirm('确定要完工该工单吗？', '提示', {
    type: 'success',
  }).then(async () => {
    await workOrderApi.complete(row.id)
    ElMessage.success('完工成功')
    loadList()
  }).catch(() => {})
}

function handleForceComplete(row: WorkOrderVO) {
  forceCompleteRow = row
  forceCompleteForm.reason = ''
  forceCompleteVisible.value = true
}

async function handleForceCompleteSubmit() {
  await forceCompleteFormRef.value?.validate()
  if (!forceCompleteRow) return
  forceCompleteLoading.value = true
  try {
    await workOrderApi.forceComplete(forceCompleteRow.id, { reason: forceCompleteForm.reason })
    ElMessage.success('强制完工成功')
    forceCompleteVisible.value = false
    loadList()
  } finally {
    forceCompleteLoading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.work-order-list {
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
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
