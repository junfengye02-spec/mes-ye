<template>
  <div class="receipt-request-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="申请单号">
          <el-input v-model="query.requestNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="工单ID">
          <el-input-number
            v-model="query.workOrderId"
            :min="1"
            :controls="false"
            placeholder="工单ID"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="入库类型">
          <el-select v-model="query.receiptType" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('receiptType')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('receiptRequestStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <span>完工入库申请列表</span>
          <el-button v-auth="['material:receiptRequest:create']" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="requestNo" label="申请单号" min-width="140" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="receiptType" label="入库类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('receiptType', row.receiptType || '') as any">
              {{ getDictLabel('receiptType', row.receiptType || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="serialNo" label="序列号" min-width="120" />
        <el-table-column prop="qty" label="数量" width="100" align="right" />
        <el-table-column prop="qualifiedQty" label="合格数" width="100" align="right" />
        <el-table-column prop="unqualifiedQty" label="不合格数" width="100" align="right" />
        <el-table-column prop="pendingReceiptQty" label="待收料数" width="110" align="right" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('receiptRequestStatus', row.status || '') as any">
              {{ getDictLabel('receiptRequestStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-auth="['material:receiptRequest:update']" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-auth="['material:receiptRequest:delete']" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
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
      width="760px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申请单号" prop="requestNo">
              <el-input v-model="form.requestNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库类型" prop="receiptType">
              <el-select v-model="form.receiptType" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in getDictList('receiptType')"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单" prop="workOrderId">
              <el-select
                v-model="form.workOrderId"
                placeholder="请选择工单"
                filterable
                remote
                clearable
                :remote-method="searchWorkOrders"
                :loading="workOrderLoading"
                style="width: 100%"
                @change="onWorkOrderChange"
              >
                <el-option
                  v-for="wo in workOrderOptions"
                  :key="wo.id"
                  :label="wo.workOrderNo"
                  :value="wo.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单号" prop="workOrderNo">
              <el-input v-model="form.workOrderNo" placeholder="自动填充" disabled />
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
          <el-col :span="12">
            <el-form-item label="物料编码" prop="materialCode">
              <el-input v-model="form.materialCode" placeholder="请输入物料编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="materialName">
              <el-input v-model="form.materialName" placeholder="请输入物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号" prop="serialNo">
              <el-input v-model="form.serialNo" placeholder="请输入序列号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量" prop="qty">
              <el-input-number v-model="form.qty" :min="0.0001" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合格数量" prop="qualifiedQty">
              <el-input-number v-model="form.qualifiedQty" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="不合格数量" prop="unqualifiedQty">
              <el-input-number v-model="form.unqualifiedQty" :min="0" :precision="4" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划入库时间" prop="planReceiptTime">
              <el-date-picker
                v-model="form.planReceiptTime"
                type="datetime"
                placeholder="选择计划入库时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { ReceiptRequestVO, ReceiptRequestDTO, ReceiptRequestQuery } from '@/types/material-mgmt'
import type { WorkOrderVO } from '@/types/workorder'
import { receiptRequestApi } from '@/api/material-mgmt/receipt'
import { workOrderApi } from '@/api/workorder/workOrder'

const loading = ref(false)
const list = ref<ReceiptRequestVO[]>([])
const total = ref(0)
const query = reactive<ReceiptRequestQuery>({
  requestNo: '',
  workOrderId: undefined,
  receiptType: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增入库申请')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)

// 对齐后端 ReceiptRequestDTO
const form = reactive<ReceiptRequestDTO>({
  requestNo: '',
  receiptType: '',
  workOrderId: undefined,
  workOrderNo: '',
  projectName: '',
  wbsElement: '',
  materialId: undefined,
  materialCode: '',
  materialName: '',
  serialNo: '',
  qty: undefined,
  qualifiedQty: undefined,
  unqualifiedQty: undefined,
  unit: '',
  description: '',
  planReceiptTime: '',
})

const workOrderOptions = ref<WorkOrderVO[]>([])
const workOrderLoading = ref(false)

const rules: FormRules = {
  receiptType: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
  workOrderId: [{ required: true, message: '请选择工单', trigger: 'change' }],
  qty: [{ required: true, message: '请输入数量', trigger: 'blur' }],
}

async function searchWorkOrders(keyword: string) {
  if (!keyword) return
  workOrderLoading.value = true
  try {
    const res = await workOrderApi.page({ workOrderNo: keyword, pageNum: 1, pageSize: 20 })
    workOrderOptions.value = res?.list ?? []
  } finally {
    workOrderLoading.value = false
  }
}

// 选择工单时同步回填 workOrderNo
function onWorkOrderChange(id: number) {
  const wo = workOrderOptions.value.find((w) => w.id === id)
  form.workOrderNo = wo?.workOrderNo ?? ''
}

async function loadList() {
  loading.value = true
  try {
    const res = await receiptRequestApi.page(query)
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
  query.requestNo = ''
  query.workOrderId = undefined
  query.receiptType = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function resetForm() {
  Object.assign(form, {
    requestNo: '',
    receiptType: '',
    workOrderId: undefined,
    workOrderNo: '',
    projectName: '',
    wbsElement: '',
    materialId: undefined,
    materialCode: '',
    materialName: '',
    serialNo: '',
    qty: undefined,
    qualifiedQty: undefined,
    unqualifiedQty: undefined,
    unit: '',
    description: '',
    planReceiptTime: '',
  })
}

function handleAdd() {
  dialogTitle.value = '新增入库申请'
  editId.value = null
  resetForm()
  workOrderOptions.value = []
  dialogVisible.value = true
}

function handleEdit(row: ReceiptRequestVO) {
  dialogTitle.value = '编辑入库申请'
  editId.value = row.id
  Object.assign(form, {
    requestNo: row.requestNo ?? '',
    receiptType: row.receiptType ?? '',
    workOrderId: row.workOrderId,
    workOrderNo: row.workOrderNo ?? '',
    projectName: row.projectName ?? '',
    wbsElement: '',
    materialId: undefined,
    materialCode: row.materialCode ?? '',
    materialName: row.materialName ?? '',
    serialNo: row.serialNo ?? '',
    qty: row.qty,
    qualifiedQty: row.qualifiedQty,
    unqualifiedQty: row.unqualifiedQty,
    unit: '',
    description: '',
    planReceiptTime: '',
  })
  if (row.workOrderId) {
    workOrderOptions.value = [{ id: row.workOrderId, workOrderNo: row.workOrderNo ?? '' } as WorkOrderVO]
  }
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
      await receiptRequestApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await receiptRequestApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ReceiptRequestVO) {
  ElMessageBox.confirm('确定要删除该入库申请吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await receiptRequestApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.receipt-request-list {
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
