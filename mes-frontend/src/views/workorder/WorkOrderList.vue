<template>
  <div class="work-order-list">
    <el-card shadow="never" class="search-card">
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
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span>工单列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
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
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              type="primary"
              link
              size="small"
              @click="handleRelease(row)"
            >
              下发
            </el-button>
            <el-button
              v-if="row.status === 'RELEASED'"
              type="success"
              link
              size="small"
              @click="handleStart(row)"
            >
              开工
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              type="success"
              link
              size="small"
              @click="handleComplete(row)"
            >
              完工
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
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
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
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
            <el-form-item label="BOM编码" prop="bomCode">
              <el-input v-model="form.bomCode" placeholder="请输入BOM编码" />
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
            <el-form-item label="计划工作中心" prop="planWorkCenterId">
              <el-input-number v-model="form.planWorkCenterId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
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
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { WorkOrderVO, WorkOrderDTO, WorkOrderQuery } from '@/types/workorder'
import { workOrderApi } from '@/api/workorder/workOrder'

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
const form = reactive<WorkOrderDTO>({
  workOrderNo: '',
  workOrderType: '',
  productCode: '',
  productName: '',
  bomCode: '',
  planQty: 1,
  qtyUnit: '',
  factoryOrg: '',
  planOrg: '',
  planWorkCenterId: undefined,
  planStartTime: '',
  planEndTime: '',
  remark: '',
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

function handleAdd() {
  dialogTitle.value = '新增工单'
  editId.value = null
  Object.assign(form, {
    workOrderNo: '',
    workOrderType: '',
    productCode: '',
    productName: '',
    bomCode: '',
    planQty: 1,
    qtyUnit: '',
    factoryOrg: '',
    planOrg: '',
    planWorkCenterId: undefined,
    planStartTime: '',
    planEndTime: '',
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: WorkOrderVO) {
  dialogTitle.value = '编辑工单'
  editId.value = row.id
  Object.assign(form, {
    workOrderNo: row.workOrderNo,
    workOrderType: row.workOrderType,
    productCode: row.productCode,
    productName: row.productName,
    bomCode: row.bomCode,
    planQty: row.planQty ?? 1,
    qtyUnit: row.qtyUnit,
    factoryOrg: row.factoryOrg,
    planOrg: row.planOrg,
    planWorkCenterId: row.planWorkCenterId,
    planStartTime: row.planStartTime,
    planEndTime: row.planEndTime,
    remark: row.remark,
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
