<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="指示书编码">
        <el-input v-model="query.instructionCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="指示书名称">
        <el-input v-model="query.instructionName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="产品编码">
        <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 180px" />
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
      <el-table-column prop="instructionCode" label="指示书编码" min-width="120" />
      <el-table-column prop="instructionName" label="指示书名称" min-width="140" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="productCode" label="产品编码" min-width="120" />
      <el-table-column prop="productName" label="产品名称" min-width="140" />
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
        <el-form-item label="指示书编码" prop="instructionCode">
          <el-input v-model="form.instructionCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="指示书名称" prop="instructionName">
          <el-input v-model="form.instructionName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="form.version" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入" />
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
  instructionCode: undefined,
  instructionName: undefined,
  productCode: undefined,
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
  instructionCode: '',
  instructionName: '',
  version: '',
  productCode: '',
  productName: '',
  remark: '',
})

const formRules = {
  instructionCode: [{ required: true, message: '请输入指示书编码', trigger: 'blur' }],
  instructionName: [{ required: true, message: '请输入指示书名称', trigger: 'blur' }],
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
    instructionCode: row.instructionCode,
    instructionName: row.instructionName,
    version: row.version,
    productCode: row.productCode,
    productName: row.productName,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    instructionCode: '',
    instructionName: '',
    version: '',
    productCode: '',
    productName: '',
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
