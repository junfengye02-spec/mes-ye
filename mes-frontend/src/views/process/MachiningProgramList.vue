<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="程序编码">
        <el-input v-model="query.programCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="程序名称">
        <el-input v-model="query.programName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="机床类型">
        <el-input v-model="query.machineType" placeholder="请输入" clearable style="width: 180px" />
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
      <template #title>机械加工程序表</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="programCode" label="程序编码" min-width="120" />
      <el-table-column prop="programName" label="程序名称" min-width="140" />
      <el-table-column prop="machineType" label="机床类型" min-width="100" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑加工程序' : '新增加工程序'" width="640px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="程序编码" prop="programCode">
          <el-input v-model="form.programCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="程序名称" prop="programName">
          <el-input v-model="form.programName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="机床类型" prop="machineType">
          <el-input v-model="form.machineType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="form.version" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="程序内容" prop="programContent">
          <el-input v-model="form.programContent" type="textarea" :rows="6" placeholder="请输入" />
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
import { machiningProgramApi } from '@/api/process/machiningProgram'
import type { MachiningProgramVO, MachiningProgramDTO, MachiningProgramQuery } from '@/types/process'

const query = reactive<MachiningProgramQuery>({
  programCode: undefined,
  programName: undefined,
  machineType: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<MachiningProgramVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<MachiningProgramDTO>({
  programCode: '',
  programName: '',
  machineType: '',
  version: '',
  programContent: '',
  remark: '',
})

const formRules = {
  programCode: [{ required: true, message: '请输入程序编码', trigger: 'blur' }],
  programName: [{ required: true, message: '请输入程序名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await machiningProgramApi.page(query)
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

function handleEdit(row: MachiningProgramVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    programCode: row.programCode,
    programName: row.programName,
    machineType: row.machineType,
    version: row.version,
    programContent: row.programContent,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    programCode: '',
    programName: '',
    machineType: '',
    version: '',
    programContent: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await machiningProgramApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await machiningProgramApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: MachiningProgramVO) {
  await ElMessageBox.confirm('确定要删除该加工程序吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await machiningProgramApi.delete(row.id)
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
