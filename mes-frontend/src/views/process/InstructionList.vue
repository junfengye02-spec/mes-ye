<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="指示书编码">
        <el-input v-model="query.instructionNo" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="产品类别">
        <el-input v-model="query.productCategory" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="产品类型">
        <el-input v-model="query.productType" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-input v-model="query.status" placeholder="请输入" clearable style="width: 180px" />
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
      <template #title>指示书管理</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="instructionNo" label="指示书号" min-width="120" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="workInstructionId" label="关联指导书ID" width="120" />
      <el-table-column prop="projectNo" label="项目编号" min-width="120" />
      <el-table-column prop="productCategory" label="产品类别" min-width="120" />
      <el-table-column prop="productType" label="产品类型" min-width="120" />
      <el-table-column prop="workOrderNo" label="生产订单编号" min-width="130" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(getDictType('instructionStatus', row.status) || undefined) as any">
            {{ getDictLabel('instructionStatus', row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" @click.stop="handleUpgrade(row)">升级版本</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑指示书' : '新增指示书'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="指示书号" prop="instructionNo">
          <el-input v-model="form.instructionNo" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="项目编号" prop="projectNo">
          <el-input v-model="form.projectNo" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="WBS" prop="wbs">
          <el-input v-model="form.wbs" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品类别" prop="productCategory">
          <el-input v-model="form.productCategory" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品类型" prop="productType">
          <el-input v-model="form.productType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生产订单编号" prop="workOrderNo">
          <el-input v-model="form.workOrderNo" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="数量" prop="qty">
          <el-input-number v-model="form.qty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="关联指导书ID" prop="workInstructionId">
          <el-input-number v-model="form.workInstructionId" placeholder="请输入指导书ID" style="width: 100%" />
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
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - Element Plus type inference for form/table
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import { getDictLabel, getDictType } from '@/utils/dict'
import { instructionApi } from '@/api/process/instruction'
import type { InstructionVO, InstructionDTO, InstructionQuery } from '@/types/process'

const query = reactive<InstructionQuery>({
  instructionNo: undefined,
  productCategory: undefined,
  productType: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<InstructionVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<InstructionDTO>({
  instructionNo: '',
  projectNo: '',
  wbs: '',
  productCategory: '',
  productType: '',
  workOrderNo: '',
  qty: undefined,
  workInstructionId: undefined,
  remark: '',
})

const formRules = {
  instructionNo: [{ required: true, message: '请输入指示书号', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await instructionApi.page(query)
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
  dialogVisible.value = true
}

function handleEdit(row: InstructionVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    instructionNo: row.instructionNo,
    projectNo: row.projectNo,
    wbs: row.wbs,
    productCategory: row.productCategory,
    productType: row.productType,
    workOrderNo: row.workOrderNo,
    qty: row.qty,
    workInstructionId: row.workInstructionId,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    instructionNo: '',
    projectNo: '',
    wbs: '',
    productCategory: '',
    productType: '',
    workOrderNo: '',
    qty: undefined,
    workInstructionId: undefined,
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await instructionApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await instructionApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleUpgrade(row: InstructionVO) {
  loading.value = true
  try {
    await instructionApi.upgrade(row.id)
    ElMessage.success('版本升级成功')
    loadData()
  } finally {
    loading.value = false
  }
}

async function handleDelete(row: InstructionVO) {
  await ElMessageBox.confirm('确定要删除该指示书吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await instructionApi.delete(row.id)
    ElMessage.success('删除成功')
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
