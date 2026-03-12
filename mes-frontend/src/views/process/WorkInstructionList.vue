<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="指导书编码">
        <el-input v-model="query.instructionCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="指导书名称">
        <el-input v-model="query.instructionName" placeholder="请输入" clearable style="width: 180px" />
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
      <template #title>指导书管理</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="instructionCode" label="指导书编码" min-width="120" />
      <el-table-column prop="instructionName" label="指导书名称" min-width="140" />
      <el-table-column prop="processName" label="工序名称" min-width="120" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑指导书' : '新增指导书'" width="640px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="指导书编码" prop="instructionCode">
          <el-input v-model="form.instructionCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="指导书名称" prop="instructionName">
          <el-input v-model="form.instructionName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工序" prop="processId">
          <el-input-number v-model="form.processId" placeholder="请输入工序ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="form.version" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入" />
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
import { workInstructionApi } from '@/api/process/workInstruction'
import type { WorkInstructionVO, WorkInstructionDTO, WorkInstructionQuery } from '@/types/process'

const query = reactive<WorkInstructionQuery>({
  instructionCode: undefined,
  instructionName: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<WorkInstructionVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<WorkInstructionDTO>({
  instructionCode: '',
  instructionName: '',
  processId: undefined,
  version: '',
  content: '',
  remark: '',
})

const formRules = {
  instructionCode: [{ required: true, message: '请输入指导书编码', trigger: 'blur' }],
  instructionName: [{ required: true, message: '请输入指导书名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await workInstructionApi.page(query)
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

function handleEdit(row: WorkInstructionVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    instructionCode: row.instructionCode,
    instructionName: row.instructionName,
    processId: row.processId,
    version: row.version,
    content: row.content,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    instructionCode: '',
    instructionName: '',
    processId: undefined,
    version: '',
    content: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await workInstructionApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await workInstructionApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: WorkInstructionVO) {
  await ElMessageBox.confirm('确定要删除该指导书吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await workInstructionApi.delete(row.id)
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
