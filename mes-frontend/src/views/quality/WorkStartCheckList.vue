<template>
  <div class="work-start-check-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="检查状态">
          <el-select v-model="query.checkStatus" placeholder="检查状态" clearable style="width: 120px">
            <el-option label="通过" value="PASSED" />
            <el-option label="不通过" value="FAILED" />
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
          <span class="table-title">开工检查列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="workNo" label="工作编号" min-width="120" />
        <el-table-column prop="checkItem" label="检查项" min-width="140" show-overflow-tooltip />
        <el-table-column prop="checkResult" label="检查结果" min-width="120" show-overflow-tooltip />
        <el-table-column prop="checkStatus" label="检查状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.checkStatus === 'PASSED' ? 'success' : 'danger'">
              {{ row.checkStatus === 'PASSED' ? '通过' : row.checkStatus === 'FAILED' ? '不通过' : row.checkStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checker" label="检查人" width="100" />
        <el-table-column prop="checkTime" label="检查时间" width="170" />
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
      :title="isEdit ? '编辑开工检查' : '新增开工检查'"
      width="560px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="工单ID" prop="workOrderId">
          <el-input-number v-model="form.workOrderId" :min="1" :controls="false" placeholder="工单ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作编号" prop="workNo">
          <el-input v-model="form.workNo" placeholder="工作编号" />
        </el-form-item>
        <el-form-item label="检查项" prop="checkItem">
          <el-input v-model="form.checkItem" placeholder="检查项" />
        </el-form-item>
        <el-form-item label="检查状态" prop="checkStatus">
          <el-select v-model="form.checkStatus" placeholder="检查状态" style="width: 100%">
            <el-option label="通过" value="PASSED" />
            <el-option label="不通过" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查结果" prop="checkResult">
          <el-input v-model="form.checkResult" type="textarea" :rows="2" placeholder="检查结果" />
        </el-form-item>
        <el-form-item label="检查备注" prop="checkRemark">
          <el-input v-model="form.checkRemark" type="textarea" :rows="2" placeholder="检查备注" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
import type { WorkStartCheckVO, WorkStartCheckDTO, WorkStartCheckQuery } from '@/types/quality'
import { workStartCheckApi } from '@/api/quality/workStartCheck'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<WorkStartCheckVO[]>([])
const total = ref(0)
const query = reactive<WorkStartCheckQuery>({
  workOrderNo: '',
  checkStatus: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<
  WorkStartCheckDTO & { workOrderTaskId?: number; workOrderNo?: string }
>({
  workOrderId: undefined,
  workOrderTaskId: undefined,
  workOrderNo: '',
  workNo: '',
  checkItem: '',
  checkResult: '',
  checkStatus: 'PASSED',
  checkRemark: '',
  remark: '',
})
const formRules: FormRules = {
  workOrderId: [{ required: true, message: '请输入工单ID', trigger: 'blur' }],
  checkItem: [{ required: true, message: '请输入检查项', trigger: 'blur' }],
  checkStatus: [{ required: true, message: '请选择检查状态', trigger: 'change' }],
}

async function fetchList() {
  loading.value = true
  try {
    const res = await workStartCheckApi.page(query)
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
  query.checkStatus = ''
  query.pageNum = 1
  fetchList()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: WorkStartCheckVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    workOrderId: row.workOrderId,
    workOrderTaskId: row.workOrderTaskId,
    workOrderNo: row.workOrderNo,
    workNo: row.workNo,
    checkItem: row.checkItem || '',
    checkResult: row.checkResult,
    checkStatus: row.checkStatus || 'PASSED',
    checkRemark: row.checkRemark,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    workOrderId: undefined,
    workOrderTaskId: undefined,
    workOrderNo: '',
    workNo: '',
    checkItem: '',
    checkResult: '',
    checkStatus: 'PASSED',
    checkRemark: '',
    remark: '',
  })
}

async function handleSubmitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload: WorkStartCheckDTO = {
      workNo: form.workNo,
      workOrderTaskId: form.workOrderTaskId,
      workOrderId: form.workOrderId,
      workOrderNo: form.workOrderNo || undefined,
      checkItem: form.checkItem,
      checkResult: form.checkResult,
      checkStatus: form.checkStatus,
      checkRemark: form.checkRemark,
      remark: form.remark,
    }
    if (isEdit.value && editId.value !== null) {
      await workStartCheckApi.update(editId.value!, payload)
      ElMessage.success('更新成功')
    } else {
      await workStartCheckApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.work-start-check-list {
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
