<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="条件编码">
        <el-input v-model="query.conditionNo" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="喷枪型号">
        <el-input v-model="query.sprayGunModel" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="设备">
        <el-input v-model="query.equipment" placeholder="请输入" clearable style="width: 180px" />
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
      <el-table-column prop="conditionNo" label="条件号" min-width="120" />
      <el-table-column prop="sprayGunModel" label="喷枪型号" min-width="120" />
      <el-table-column prop="equipment" label="设备" min-width="120" />
      <el-table-column prop="powderFeedRate" label="送粉量(g/min)" width="120" />
      <el-table-column prop="sprayDistance" label="喷涂距离(mm)" width="120" />
      <el-table-column prop="powderType" label="对应粉末" min-width="120" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑喷涂条件' : '新增喷涂条件'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="条件号" prop="conditionNo">
          <el-input v-model="form.conditionNo" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="部长审批人" prop="ministerApprover">
          <el-input v-model="form.ministerApprover" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工段审批人" prop="sectionApprover">
          <el-input v-model="form.sectionApprover" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="系长审批人" prop="leaderApprover">
          <el-input v-model="form.leaderApprover" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="送粉量(g/min)" prop="powderFeedRate">
          <el-input-number v-model="form.powderFeedRate" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="喷涂距离(mm)" prop="sprayDistance">
          <el-input-number v-model="form.sprayDistance" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="喷枪型号" prop="sprayGunModel">
          <el-input v-model="form.sprayGunModel" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="设备" prop="equipment">
          <el-input v-model="form.equipment" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="对应粉末" prop="powderType">
          <el-input v-model="form.powderType" placeholder="请输入" />
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
  conditionNo: undefined,
  sprayGunModel: undefined,
  equipment: undefined,
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
  conditionNo: '',
  ministerApprover: '',
  sectionApprover: '',
  leaderApprover: '',
  powderFeedRate: undefined,
  sprayDistance: undefined,
  sprayGunModel: '',
  equipment: '',
  powderType: '',
})

const formRules = {
  conditionNo: [{ required: true, message: '请输入条件号', trigger: 'blur' }],
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
    conditionNo: row.conditionNo,
    ministerApprover: row.ministerApprover,
    sectionApprover: row.sectionApprover,
    leaderApprover: row.leaderApprover,
    powderFeedRate: row.powderFeedRate,
    sprayDistance: row.sprayDistance,
    sprayGunModel: row.sprayGunModel,
    equipment: row.equipment,
    powderType: row.powderType,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    conditionNo: '',
    ministerApprover: '',
    sectionApprover: '',
    leaderApprover: '',
    powderFeedRate: undefined,
    sprayDistance: undefined,
    sprayGunModel: '',
    equipment: '',
    powderType: '',
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
