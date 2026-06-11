<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="工作中心编码">
        <el-input v-model="query.workCenterCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工作中心名称">
        <el-input v-model="query.workCenterName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工作中心分类">
        <el-input v-model="query.workCenterCategory" placeholder="请输入" clearable style="width: 180px" />
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
        <el-button v-auth="['basic:workCenter:create']" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
        <el-button v-auth="['basic:workCenter:update']" :disabled="!selectedRows.length" @click="handleBatchEdit">
          <el-icon><Edit /></el-icon> 批量编辑
        </el-button>
      </template>
      <el-table-column prop="workCenterCode" label="工作中心编码" min-width="120" />
      <el-table-column prop="workCenterName" label="工作中心名称" min-width="140" />
      <el-table-column prop="workCenterCategory" label="工作中心分类" min-width="120" />
      <el-table-column prop="businessUnit" label="业务单元" min-width="100" />
      <el-table-column prop="resourceType" label="资源种类" min-width="100" />
      <el-table-column prop="resourceSubtype" label="资源子类型" min-width="100" />
      <el-table-column prop="resourceCapacity" label="资源能力" width="100" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button v-auth="['basic:workCenter:detail']" link type="info" @click.stop="handleView(row)">查看</el-button>
          <el-button v-auth="['basic:workCenter:update']" link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button v-auth="['basic:workCenter:delete']" link type="danger" @click.stop="handleDelete(row)">删除</el-button>
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
        <el-form-item label="工作中心分类" prop="workCenterCategory">
          <el-input v-model="form.workCenterCategory" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="业务单元" prop="businessUnit">
          <el-input v-model="form.businessUnit" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工作日历" prop="workCalendar">
          <el-input v-model="form.workCalendar" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="资源排序" prop="resourceOrder">
          <el-input-number v-model="form.resourceOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用量" prop="usageQty">
          <el-input-number v-model="form.usageQty" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用量单位" prop="usageUnit">
          <el-input v-model="form.usageUnit" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="处理批量" prop="batchQty">
          <el-input-number v-model="form.batchQty" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="效率" prop="efficiency">
          <el-input-number v-model="form.efficiency" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="资源种类" prop="resourceType">
          <el-input v-model="form.resourceType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="资源子类型" prop="resourceSubtype">
          <el-input v-model="form.resourceSubtype" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="资源能力" prop="resourceCapacity">
          <el-input-number v-model="form.resourceCapacity" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工序不中断" prop="processNoInterrupt">
          <el-switch v-model="form.processNoInterrupt" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="工序不跨天" prop="processNoCrossDay">
          <el-switch v-model="form.processNoCrossDay" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="固定节拍点生产" prop="fixedTaktProduction">
          <el-switch v-model="form.fixedTaktProduction" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          v-auth="['basic:workCenter:create', 'basic:workCenter:update']"
          type="primary"
          :loading="saving"
          @click="handleSave"
        >确定</el-button>
      </template>
    </el-dialog>

    <BatchEdit v-model:visible="batchEditVisible" :rows="(selectedRows as any)" @save="handleBatchSave">
      <el-table-column prop="workCenterCode" label="工作中心编码" width="120" />
      <el-table-column prop="workCenterName" label="工作中心名称" width="140" />
      <el-table-column prop="workCenterCategory" label="工作中心分类" width="120" />
      <el-table-column prop="businessUnit" label="业务单元" width="100" />
      <el-table-column prop="resourceType" label="资源种类" width="100" />
      <el-table-column prop="resourceSubtype" label="资源子类型" width="110" />
      <el-table-column prop="resourceCapacity" label="资源能力" width="100" />
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
  workCenterCategory: undefined,
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
  workCenterCategory: '',
  businessUnit: '',
  workCalendar: '',
  resourceOrder: undefined,
  usageQty: undefined,
  usageUnit: '',
  batchQty: undefined,
  efficiency: undefined,
  resourceType: '',
  resourceSubtype: '',
  resourceCapacity: undefined,
  processNoInterrupt: 0,
  processNoCrossDay: 0,
  fixedTaktProduction: 0,
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
    workCenterCategory: row.workCenterCategory,
    businessUnit: row.businessUnit,
    workCalendar: row.workCalendar,
    resourceOrder: row.resourceOrder,
    usageQty: row.usageQty,
    usageUnit: row.usageUnit,
    batchQty: row.batchQty,
    efficiency: row.efficiency,
    resourceType: row.resourceType,
    resourceSubtype: row.resourceSubtype,
    resourceCapacity: row.resourceCapacity,
    processNoInterrupt: row.processNoInterrupt ?? 0,
    processNoCrossDay: row.processNoCrossDay ?? 0,
    fixedTaktProduction: row.fixedTaktProduction ?? 0,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    workCenterCode: '',
    workCenterName: '',
    workCenterCategory: '',
    businessUnit: '',
    workCalendar: '',
    resourceOrder: undefined,
    usageQty: undefined,
    usageUnit: '',
    batchQty: undefined,
    efficiency: undefined,
    resourceType: '',
    resourceSubtype: '',
    resourceCapacity: undefined,
    processNoInterrupt: 0,
    processNoCrossDay: 0,
    fixedTaktProduction: 0,
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

function handleView(row: WorkCenterVO) {
  const lines = [
    `编码：${row.workCenterCode ?? '-'}`,
    `名称：${row.workCenterName ?? '-'}`,
    `分类：${row.workCenterCategory ?? '-'}`,
    `业务单元：${row.businessUnit ?? '-'}`,
    `资源：${row.resourceType ?? '-'} / ${row.resourceSubtype ?? '-'}`,
    `资源能力：${row.resourceCapacity ?? '-'}`,
  ].join('\n')
  ElMessageBox.alert(lines, '工作中心详情', { confirmButtonText: '关闭' }).catch(() => {})
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
      workCenterCategory: r.workCenterCategory,
      businessUnit: r.businessUnit,
      workCalendar: r.workCalendar,
      resourceOrder: r.resourceOrder,
      usageQty: r.usageQty,
      usageUnit: r.usageUnit,
      batchQty: r.batchQty,
      efficiency: r.efficiency,
      resourceType: r.resourceType,
      resourceSubtype: r.resourceSubtype,
      resourceCapacity: r.resourceCapacity,
      processNoInterrupt: r.processNoInterrupt,
      processNoCrossDay: r.processNoCrossDay,
      fixedTaktProduction: r.fixedTaktProduction,
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
