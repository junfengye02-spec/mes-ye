<template>
  <div class="order-start-check-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="检查人">
          <el-input v-model="query.checker" placeholder="检查人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="检查结果">
          <el-select v-model="query.checkResult" placeholder="检查结果" clearable style="width: 120px">
            <el-option label="合格" value="合格" />
            <el-option label="不合格" value="不合格" />
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
          <span class="table-title">订单开工检查列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="checkNo" label="检查单号" min-width="120" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="checkDate" label="检查日期" width="120" />
        <el-table-column prop="checker" label="检查人" width="100" />
        <el-table-column prop="checkResult" label="检查结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.checkResult === '合格' ? 'success' : 'danger'">
              {{ row.checkResult || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑订单开工检查' : '新增订单开工检查'"
      width="520px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="工单号" prop="workOrderNo">
          <el-input v-model="form.workOrderNo" placeholder="输入工单号" />
        </el-form-item>
        <el-form-item label="检查日期" prop="checkDate">
          <el-date-picker
            v-model="form.checkDate"
            type="date"
            placeholder="检查日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="检查人" prop="checker">
          <el-input v-model="form.checker" placeholder="检查人" />
        </el-form-item>
        <el-form-item label="检查结果" prop="checkResult">
          <el-radio-group v-model="form.checkResult">
            <el-radio value="合格">合格</el-radio>
            <el-radio value="不合格">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitForm">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { OrderStartCheckVO, OrderStartCheckDTO } from '@/types/quality'
import { orderStartCheckApi } from '@/api/quality/orderStartCheck'
import { workOrderApi } from '@/api/workorder/workOrder'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<OrderStartCheckVO[]>([])
const total = ref(0)
const query = reactive({
  workOrderNo: '',
  checker: '',
  checkResult: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<OrderStartCheckDTO & { workOrderNo?: string }>({
  workOrderId: undefined,
  workOrderNo: '',
  checkDate: '',
  checker: '',
  checkResult: '合格',
  remark: '',
})
const formRules: FormRules = {
  workOrderNo: [{ required: true, message: '请输入工单号', trigger: 'blur' }],
  checkDate: [{ required: true, message: '请选择检查日期', trigger: 'change' }],
}

async function fetchList() {
  loading.value = true
  try {
    const res = await orderStartCheckApi.page(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.workOrderNo = ''
  query.checker = ''
  query.checkResult = ''
  query.pageNum = 1
  fetchList()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: OrderStartCheckVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    workOrderId: row.workOrderId,
    workOrderNo: row.workOrderNo,
    checkDate: row.checkDate,
    checker: row.checker,
    checkResult: row.checkResult || '合格',
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    workOrderId: undefined,
    workOrderNo: '',
    checkDate: '',
    checker: '',
    checkResult: '合格',
    remark: '',
  })
}

async function resolveWorkOrderId(workOrderNo: string): Promise<number | undefined> {
  const res = await workOrderApi.page({ workOrderNo, pageNum: 1, pageSize: 1 })
  return res?.list?.[0]?.id
}

async function handleSubmitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const workOrderId = form.workOrderId ?? (form.workOrderNo ? await resolveWorkOrderId(form.workOrderNo) : undefined)
      const payload: OrderStartCheckDTO = {
        workOrderId,
        checkDate: form.checkDate,
        checker: form.checker,
        checkResult: form.checkResult,
        remark: form.remark,
      }
      if (isEdit.value && editId.value) {
        await orderStartCheckApi.update(editId.value, payload)
        ElMessage.success('更新成功')
      } else {
        if (!workOrderId) {
          ElMessage.warning('未找到对应工单，请检查工单号')
          return
        }
        await orderStartCheckApi.create(payload)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchList()
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.order-start-check-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-title {
  font-weight: 600;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
