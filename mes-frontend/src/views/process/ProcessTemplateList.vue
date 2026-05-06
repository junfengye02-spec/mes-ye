<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <div class="table-title">工序模板</div>
          <div class="table-actions">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon> 新增
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="list"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
        stripe
        v-loading="loading"
      >
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
        <el-table-column prop="processNo" label="工序号" min-width="120" />
        <el-table-column prop="processName" label="工序名" min-width="140" />
        <el-table-column prop="processType" label="工序类型" min-width="100" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工序模板' : '新增工序模板'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="工序号" prop="processNo">
          <el-input v-model="form.processNo" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="工序名" prop="processName">
          <el-input v-model="form.processName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="父工序" prop="parentProcessNo">
          <el-tree-select
            v-model="form.parentProcessNo"
            :data="(list as any)"
            node-key="processNo"
            :props="{ label: 'processName', children: 'children' }"
            placeholder="请选择（可选）"
            clearable
            check-strictly
            style="width: 100%"
            :render-after-expand="false"
          />
        </el-form-item>
        <el-form-item label="工序类型" prop="processType">
          <el-input v-model="form.processType" placeholder="请输入" />
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
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - Element Plus type inference for form/table
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { processTemplateApi } from '@/api/process/processTemplate'
import type { ProcessTemplateVO, ProcessTemplateDTO } from '@/types/process'

const list = ref<ProcessTemplateVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive({
  processNo: '',
  processName: '',
  parentProcessNo: undefined as string | undefined,
  processType: '',
  description: '',
} as ProcessTemplateDTO)

const formRules = {
  processNo: [{ required: true, message: '请输入工序号', trigger: 'blur' }],
  processName: [{ required: true, message: '请输入工序名', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    list.value = await processTemplateApi.tree()
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: ProcessTemplateVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    processNo: row.processNo,
    processName: row.processName,
    parentProcessNo: row.parentProcessNo,
    processType: row.processType,
    description: row.description,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    processNo: '',
    processName: '',
    parentProcessNo: undefined,
    processType: '',
    description: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await processTemplateApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await processTemplateApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: ProcessTemplateVO) {
  await ElMessageBox.confirm('确定要删除该工序模板吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await processTemplateApi.delete(row.id)
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
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-actions {
  display: flex;
  gap: 8px;
}
</style>
