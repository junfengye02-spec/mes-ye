<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="条件编码">
        <el-input v-model="query.conditionCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="条件名称">
        <el-input v-model="query.conditionName" placeholder="请输入" clearable style="width: 180px" />
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
      <template #title>喷涂条件表</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="conditionCode" label="条件编码" min-width="120" />
      <el-table-column prop="conditionName" label="条件名称" min-width="140" />
      <el-table-column prop="temperature" label="温度" width="100" />
      <el-table-column prop="humidity" label="湿度" width="100" />
      <el-table-column prop="paintType" label="涂料类型" min-width="100" />
      <el-table-column prop="thickness" label="厚度" width="100" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑喷涂条件' : '新增喷涂条件'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="条件编码" prop="conditionCode">
          <el-input v-model="form.conditionCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="条件名称" prop="conditionName">
          <el-input v-model="form.conditionName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="温度" prop="temperature">
          <el-input v-model="form.temperature" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="湿度" prop="humidity">
          <el-input v-model="form.humidity" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="涂料类型" prop="paintType">
          <el-input v-model="form.paintType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="厚度" prop="thickness">
          <el-input v-model="form.thickness" placeholder="请输入" />
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
import { sprayConditionApi } from '@/api/process/sprayCondition'
import type { SprayConditionVO, SprayConditionDTO, SprayConditionQuery } from '@/types/process'

const query = reactive<SprayConditionQuery>({
  conditionCode: undefined,
  conditionName: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<SprayConditionVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<SprayConditionDTO>({
  conditionCode: '',
  conditionName: '',
  temperature: '',
  humidity: '',
  paintType: '',
  thickness: '',
  remark: '',
})

const formRules = {
  conditionCode: [{ required: true, message: '请输入条件编码', trigger: 'blur' }],
  conditionName: [{ required: true, message: '请输入条件名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await sprayConditionApi.page(query)
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

function handleEdit(row: SprayConditionVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    conditionCode: row.conditionCode,
    conditionName: row.conditionName,
    temperature: row.temperature,
    humidity: row.humidity,
    paintType: row.paintType,
    thickness: row.thickness,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    conditionCode: '',
    conditionName: '',
    temperature: '',
    humidity: '',
    paintType: '',
    thickness: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await sprayConditionApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await sprayConditionApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: SprayConditionVO) {
  await ElMessageBox.confirm('确定要删除该喷涂条件吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await sprayConditionApi.delete(row.id)
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
