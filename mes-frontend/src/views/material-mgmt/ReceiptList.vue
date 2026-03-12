<template>
  <div class="receipt-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="入库单号">
          <el-input v-model="query.receiptNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="请输入" clearable style="width: 160px" />
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
              v-for="item in getDictList('receiptStatus')"
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
          <span>完工入库列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="receiptNo" label="入库单号" min-width="140" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="receiptQty" label="入库数量" width="100" align="right" />
        <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
        <el-table-column prop="storageLocation" label="存储地点" min-width="120" />
        <el-table-column prop="receiptType" label="入库类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('receiptType', row.receiptType || '') as any">
              {{ getDictLabel('receiptType', row.receiptType || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('receiptStatus', row.status || '') as any">
              {{ getDictLabel('receiptStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
      width="520px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工单" prop="workOrderId">
          <el-select
            v-model="form.workOrderId"
            placeholder="请选择工单"
            filterable
            remote
            :remote-method="searchWorkOrders"
            :loading="workOrderLoading"
            style="width: 100%"
          >
            <el-option
              v-for="wo in workOrderOptions"
              :key="wo.id"
              :label="wo.workOrderNo"
              :value="wo.id"
            />
          </el-select>
        </el-form-item>
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
        <el-form-item label="入库数量" prop="receiptQty">
          <el-input-number v-model="form.receiptQty" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="qtyUnit">
          <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="存储地点" prop="storageLocation">
          <el-input v-model="form.storageLocation" placeholder="请输入存储地点" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
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
import type { ReceiptVO, ReceiptDTO, ReceiptQuery } from '@/types/material-mgmt'
import type { WorkOrderVO } from '@/types/workorder'
import { receiptApi } from '@/api/material-mgmt/receipt'
import { workOrderApi } from '@/api/workorder/workOrder'

const loading = ref(false)
const list = ref<ReceiptVO[]>([])
const total = ref(0)
const query = reactive<ReceiptQuery>({
  receiptNo: '',
  workOrderNo: '',
  receiptType: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增入库')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const form = reactive<ReceiptDTO>({
  workOrderId: undefined,
  receiptType: '',
  receiptQty: undefined,
  qtyUnit: '',
  storageLocation: '',
  remark: '',
})

const workOrderOptions = ref<WorkOrderVO[]>([])
const workOrderLoading = ref(false)

const rules: FormRules = {
  workOrderId: [{ required: true, message: '请选择工单', trigger: 'change' }],
  receiptType: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
  receiptQty: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
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

async function loadList() {
  loading.value = true
  try {
    const res = await receiptApi.page(query)
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
  query.receiptNo = ''
  query.workOrderNo = ''
  query.receiptType = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增入库'
  editId.value = null
  Object.assign(form, {
    workOrderId: undefined,
    receiptType: '',
    receiptQty: undefined,
    qtyUnit: '',
    storageLocation: '',
    remark: '',
  })
  workOrderOptions.value = []
  dialogVisible.value = true
}

function handleEdit(row: ReceiptVO) {
  dialogTitle.value = '编辑入库'
  editId.value = row.id
  Object.assign(form, {
    workOrderId: row.workOrderId,
    receiptType: row.receiptType ?? '',
    receiptQty: row.receiptQty,
    qtyUnit: row.qtyUnit ?? '',
    storageLocation: row.storageLocation ?? '',
    remark: row.remark ?? '',
  })
  if (row.workOrderId) workOrderOptions.value = [{ id: row.workOrderId, workOrderNo: row.workOrderNo ?? '' } as WorkOrderVO]
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
      await receiptApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await receiptApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ReceiptVO) {
  ElMessageBox.confirm('确定要删除该入库单吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await receiptApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.receipt-list {
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
