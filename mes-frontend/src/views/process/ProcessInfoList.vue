<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="工序编码">
        <el-input v-model="query.processCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工序名称">
        <el-input v-model="query.processName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工序类型">
        <el-input v-model="query.processType" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
    </SearchForm>

    <DataTable
      :data="(list as any)"
      :loading="loading"
      :total="total"
      :page-num="Number(query.pageNum) || 1"
      :page-size="Number(query.pageSize) || 20"
      :show-selection="true"
      @page-change="handlePageChange"
      @selection-change="handleSelectionChange"
    >
      <template #title>工序信息</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
        <el-button :disabled="!selectedRows.length" @click="handleBatchEdit">
          <el-icon><Edit /></el-icon> 批量编辑
        </el-button>
      </template>
      <el-table-column prop="processCode" label="工序编码" min-width="120" />
      <el-table-column prop="processName" label="工序名称" min-width="140" />
      <el-table-column prop="processType" label="工序类型" min-width="100" />
      <el-table-column prop="workCenterName" label="工作中心" min-width="120" />
      <el-table-column prop="standardTime" label="标准工时" width="100" />
      <el-table-column prop="timeUnit" label="时间单位" width="90" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工序' : '新增工序'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="工序编码" prop="processCode">
          <el-input v-model="form.processCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="工序名称" prop="processName">
          <el-input v-model="form.processName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工序类型" prop="processType">
          <el-input v-model="form.processType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工作中心" prop="workCenterId">
          <el-input-number v-model="form.workCenterId" placeholder="请输入工作中心ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标准工时" prop="standardTime">
          <el-input-number v-model="form.standardTime" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时间单位" prop="timeUnit">
          <el-input v-model="form.timeUnit" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <BatchEdit v-model:visible="batchEditVisible" :rows="selectedRows" @save="handleBatchSave">
      <el-table-column prop="processCode" label="工序编码" width="120" />
      <el-table-column prop="processName" label="工序名称" width="140" />
      <el-table-column prop="processType" label="工序类型" width="100" />
      <el-table-column prop="workCenterId" label="工作中心ID" width="120">
        <template #default="{ row }">
          <el-input-number
            :model-value="row.workCenterId != null ? Number(row.workCenterId) : undefined"
            @update:model-value="(v: number | undefined) => { row.workCenterId = v }"
            size="small"
            :min="0"
          />
        </template>
      </el-table-column>
      <el-table-column prop="standardTime" label="标准工时" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.standardTime" size="small" :min="0" :precision="2" />
        </template>
      </el-table-column>
      <el-table-column prop="timeUnit" label="时间单位" width="90">
        <template #default="{ row }">
          <el-input v-model="row.timeUnit" size="small" />
        </template>
      </el-table-column>
    </BatchEdit>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - Element Plus type inference for form/table
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import BatchEdit from '@/components/BatchEdit/index.vue'
import { processInfoApi } from '@/api/process/processInfo'
import type { ProcessInfoVO, ProcessInfoDTO, ProcessInfoQuery } from '@/types/process'

const query = reactive<ProcessInfoQuery>({
  processCode: undefined,
  processName: undefined,
  processType: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<ProcessInfoVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const batchEditVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)
const selectedRows = ref<ProcessInfoVO[]>([])

const form = reactive<ProcessInfoDTO>({
  processCode: '',
  processName: '',
  processType: '',
  workCenterId: undefined,
  standardTime: undefined,
  timeUnit: '',
  description: '',
})

const formRules = {
  processCode: [{ required: true, message: '请输入工序编码', trigger: 'blur' }],
  processName: [{ required: true, message: '请输入工序名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await processInfoApi.page(query)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handlePageChange({ pageNum, pageSize }: { pageNum: number; pageSize: number }) {
  query.pageNum = pageNum
  query.pageSize = pageSize
  loadData()
}

function handleSelectionChange(rows: ProcessInfoVO[]) {
  selectedRows.value = rows
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: ProcessInfoVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    processCode: row.processCode,
    processName: row.processName,
    processType: row.processType,
    workCenterId: row.workCenterId,
    standardTime: row.standardTime,
    timeUnit: row.timeUnit,
    description: row.description,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    processCode: '',
    processName: '',
    processType: '',
    workCenterId: undefined,
    standardTime: undefined,
    timeUnit: '',
    description: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await processInfoApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await processInfoApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: ProcessInfoVO) {
  await ElMessageBox.confirm('确定要删除该工序吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await processInfoApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } finally {
    loading.value = false
  }
}

function handleBatchEdit() {
  if (!selectedRows.value.length) return
  batchEditVisible.value = true
}

async function handleBatchSave(rows: ProcessInfoVO[]) {
  loading.value = true
  try {
    const dtos = rows.map(r => ({
      processCode: r.processCode,
      processName: r.processName,
      processType: r.processType,
      workCenterId: r.workCenterId,
      standardTime: r.standardTime,
      timeUnit: r.timeUnit,
      description: r.description,
    }))
    await processInfoApi.batchUpdate(dtos)
    ElMessage.success('批量保存成功')
    batchEditVisible.value = false
    loadData()
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
