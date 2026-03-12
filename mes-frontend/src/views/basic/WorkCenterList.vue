<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="工作中心编码">
        <el-input v-model="query.workCenterCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工作中心名称">
        <el-input v-model="query.workCenterName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工作中心类型">
        <el-input v-model="query.workCenterType" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工厂">
        <el-input v-model="query.factory" placeholder="请输入" clearable style="width: 180px" />
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
      <template #title>工作中心</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
        <el-button :disabled="!selectedRows.length" @click="handleBatchEdit">
          <el-icon><Edit /></el-icon> 批量编辑
        </el-button>
      </template>
      <el-table-column prop="workCenterCode" label="工作中心编码" min-width="120" />
      <el-table-column prop="workCenterName" label="工作中心名称" min-width="140" />
      <el-table-column prop="workCenterType" label="工作中心类型" min-width="100" />
      <el-table-column prop="factory" label="工厂" width="100" />
      <el-table-column prop="capacity" label="产能" width="90" />
      <el-table-column prop="capacityUnit" label="产能单位" width="90" />
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
            {{ row.enabled === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工作中心' : '新增工作中心'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="工作中心编码" prop="workCenterCode">
          <el-input v-model="form.workCenterCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="工作中心名称" prop="workCenterName">
          <el-input v-model="form.workCenterName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工作中心类型" prop="workCenterType">
          <el-input v-model="form.workCenterType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工厂" prop="factory">
          <el-input v-model="form.factory" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="计划组织" prop="planOrg">
          <el-input v-model="form.planOrg" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产能" prop="capacity">
          <el-input-number v-model="form.capacity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="产能单位" prop="capacityUnit">
          <el-input v-model="form.capacityUnit" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" rows="3" placeholder="请输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <BatchEdit v-model:visible="batchEditVisible" :rows="(selectedRows as any)" @save="handleBatchSave">
      <el-table-column prop="workCenterCode" label="工作中心编码" width="120" />
      <el-table-column prop="workCenterName" label="工作中心名称" width="140" />
      <el-table-column prop="workCenterType" label="工作中心类型" width="100" />
      <el-table-column prop="factory" label="工厂" width="100" />
      <el-table-column prop="capacity" label="产能" width="90" />
      <el-table-column prop="capacityUnit" label="产能单位" width="90" />
      <el-table-column prop="enabled" label="启用" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="Number(row.enabled ?? 0)"
            @update:model-value="(v: any) => { row.enabled = v ? 1 : 0 }"
            :active-value="1"
            :inactive-value="0"
          />
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
import { workCenterApi } from '@/api/basic/workCenter'
import type { WorkCenterVO, WorkCenterDTO, WorkCenterQuery } from '@/types/basic'

const query = reactive<WorkCenterQuery>({
  workCenterCode: undefined,
  workCenterName: undefined,
  workCenterType: undefined,
  factory: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<WorkCenterVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const batchEditVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)
const selectedRows = ref<WorkCenterVO[]>([])

const form = reactive<WorkCenterDTO>({
  workCenterCode: '',
  workCenterName: '',
  workCenterType: '',
  factory: '',
  planOrg: '',
  capacity: undefined,
  capacityUnit: '',
  enabled: 1,
  remark: '',
})

const formRules = {
  workCenterCode: [{ required: true, message: '请输入工作中心编码', trigger: 'blur' }],
  workCenterName: [{ required: true, message: '请输入工作中心名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await workCenterApi.page(query)
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

function handleSelectionChange(rows: WorkCenterVO[]) {
  selectedRows.value = rows
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: WorkCenterVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    workCenterCode: row.workCenterCode,
    workCenterName: row.workCenterName,
    workCenterType: row.workCenterType,
    factory: row.factory,
    planOrg: row.planOrg,
    capacity: row.capacity,
    capacityUnit: row.capacityUnit,
    enabled: row.enabled ?? 1,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    workCenterCode: '',
    workCenterName: '',
    workCenterType: '',
    factory: '',
    planOrg: '',
    capacity: undefined,
    capacityUnit: '',
    enabled: 1,
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await workCenterApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await workCenterApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: WorkCenterVO) {
  await ElMessageBox.confirm('确定要删除该工作中心吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await workCenterApi.delete(row.id)
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

async function handleBatchSave(rows: WorkCenterVO[]) {
  loading.value = true
  try {
    const dtos = rows.map(r => ({
      workCenterCode: r.workCenterCode,
      workCenterName: r.workCenterName,
      workCenterType: r.workCenterType,
      factory: r.factory,
      planOrg: r.planOrg,
      capacity: r.capacity,
      capacityUnit: r.capacityUnit,
      enabled: r.enabled,
      remark: r.remark,
    }))
    await workCenterApi.batchUpdate(dtos)
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
