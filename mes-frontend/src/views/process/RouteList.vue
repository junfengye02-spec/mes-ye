<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="路线编码">
        <el-input v-model="query.routeCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="路线名称">
        <el-input v-model="query.routeName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="产品编码">
        <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="产品类别">
        <el-input v-model="query.productCategory" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="请选择" clearable style="width: 150px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </SearchForm>

    <DataTable
      :data="(list as any)"
      :loading="loading"
      :total="total"
      :page-num="Number(query.pageNum) || 1"
      :page-size="Number(query.pageSize) || 20"
      @page-change="handlePageChange"
    >
      <template #title>工艺路线</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="routeCode" label="路线编码" min-width="130" />
      <el-table-column prop="routeName" label="路线名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="productCode" label="产品编码" min-width="120" />
      <el-table-column prop="productCategory" label="产品类别" min-width="120" />
      <el-table-column prop="machineModel" label="机型" min-width="110" />
      <el-table-column prop="productType" label="产品类型" min-width="110" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="effectiveDate" label="生效日期" width="120" />
      <el-table-column prop="expiryDate" label="失效日期" width="120" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 'ACTIVE'" link type="success" @click.stop="handleActivate(row)">启用</el-button>
          <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click.stop="handleDisable(row)">停用</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工艺路线' : '新增工艺路线'" width="900px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="路线编码" prop="routeCode">
              <el-input v-model="form.routeCode" placeholder="请输入" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路线名称" prop="routeName">
              <el-input v-model="form.routeName" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品编码" prop="productCode">
              <el-input v-model="form.productCode" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品类别" prop="productCategory">
              <el-input v-model="form.productCategory" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机型" prop="machineModel">
              <el-input v-model="form.machineModel" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品类型" prop="productType">
              <el-input v-model="form.productType" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期" prop="effectiveDate">
              <el-date-picker v-model="form.effectiveDate" value-format="YYYY-MM-DD" type="date" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效日期" prop="expiryDate">
              <el-date-picker v-model="form.expiryDate" value-format="YYYY-MM-DD" type="date" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" rows="2" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="step-header">
        <div class="step-title">路线步骤</div>
        <el-button @click="handleAddStep">
          <el-icon><Plus /></el-icon> 添加步骤
        </el-button>
      </div>
      <el-table :data="form.steps" border size="small" class="step-table">
        <el-table-column label="顺序" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.sequenceNo" :min="1" :controls="false" size="small" style="width: 64px" />
          </template>
        </el-table-column>
        <el-table-column label="工序ID" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.processId" :min="1" :controls="false" size="small" style="width: 84px" />
          </template>
        </el-table-column>
        <el-table-column label="工序号" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.processNo" size="small" placeholder="工序号" />
          </template>
        </el-table-column>
        <el-table-column label="工序名称" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.processName" size="small" placeholder="工序名称" />
          </template>
        </el-table-column>
        <el-table-column label="工作中心ID" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.workCenterId" :min="1" :controls="false" size="small" style="width: 92px" />
          </template>
        </el-table-column>
        <el-table-column label="标准工时" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.handleTime" :min="0" :precision="2" :controls="false" size="small" style="width: 82px" />
          </template>
        </el-table-column>
        <el-table-column label="并行" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.parallelFlag" :true-label="1" :false-label="0" />
          </template>
        </el-table-column>
        <el-table-column label="可选" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.optionalFlag" :true-label="1" :false-label="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="handleRemoveStep($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - Element Plus type inference for form/table
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import { routeApi } from '@/api/process/route'
import type { RouteDTO, RouteQuery, RouteStepVO, RouteVO } from '@/api/process/route'

const query = reactive<RouteQuery>({
  routeCode: undefined,
  routeName: undefined,
  productCode: undefined,
  productCategory: undefined,
  machineModel: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<RouteVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<RouteDTO>({
  routeCode: '',
  routeName: '',
  productCode: '',
  productCategory: '',
  machineModel: '',
  productType: '',
  effectiveDate: undefined,
  expiryDate: undefined,
  remark: '',
  steps: [],
})

const formRules = {
  routeCode: [{ required: true, message: '请输入路线编码', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await routeApi.page(query)
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

function handleAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  form.steps = [newStep(1)]
  dialogVisible.value = true
}

async function handleEdit(row: RouteVO) {
  isEdit.value = true
  editId.value = row.id
  const detail = await routeApi.getDetail(row.id)
  Object.assign(form, {
    routeCode: detail.routeCode,
    routeName: detail.routeName || '',
    productCode: detail.productCode || '',
    productCategory: detail.productCategory || '',
    machineModel: detail.machineModel || '',
    productType: detail.productType || '',
    effectiveDate: detail.effectiveDate,
    expiryDate: detail.expiryDate,
    remark: detail.remark || '',
    steps: (detail.steps || []).map(normalizeStep),
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    routeCode: '',
    routeName: '',
    productCode: '',
    productCategory: '',
    machineModel: '',
    productType: '',
    effectiveDate: undefined,
    expiryDate: undefined,
    remark: '',
    steps: [],
  })
  formRef.value?.clearValidate()
}

function handleAddStep() {
  form.steps = [...(form.steps || []), newStep((form.steps?.length || 0) + 1)]
}

function handleRemoveStep(index: number) {
  form.steps = (form.steps || [])
    .filter((_step, i) => i !== index)
    .map((step, i) => ({ ...step, sequenceNo: i + 1 }))
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = normalizePayload()
    if (isEdit.value && editId.value) {
      await routeApi.update(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await routeApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleActivate(row: RouteVO) {
  await routeApi.activate(row.id)
  ElMessage.success('启用成功')
  loadData()
}

async function handleDisable(row: RouteVO) {
  await routeApi.disable(row.id)
  ElMessage.success('停用成功')
  loadData()
}

async function handleDelete(row: RouteVO) {
  await ElMessageBox.confirm('确定要删除该工艺路线吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await routeApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } finally {
    loading.value = false
  }
}

function normalizePayload(): RouteDTO {
  return {
    ...form,
    steps: (form.steps || [])
      .filter(step => step.processId || step.processNo || step.processName)
      .map((step, index) => ({
        ...step,
        sequenceNo: step.sequenceNo || index + 1,
        parallelFlag: step.parallelFlag ? 1 : 0,
        optionalFlag: step.optionalFlag ? 1 : 0,
      })),
  }
}

function normalizeStep(step: RouteStepVO): RouteStepVO {
  return {
    ...step,
    parallelFlag: step.parallelFlag ? 1 : 0,
    optionalFlag: step.optionalFlag ? 1 : 0,
  }
}

function newStep(sequenceNo: number): RouteStepVO {
  return {
    sequenceNo,
    processId: undefined,
    processNo: '',
    processName: '',
    workCenterId: undefined,
    handleTime: undefined,
    parallelFlag: 0,
    optionalFlag: 0,
    remark: '',
  }
}

function statusText(status?: string) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'DISABLED') return '停用'
  return '草稿'
}

function statusTag(status?: string) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED') return 'info'
  return 'warning'
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 10px;
}
.step-title {
  font-size: 14px;
  font-weight: 600;
}
.step-table {
  margin-bottom: 8px;
}
</style>
