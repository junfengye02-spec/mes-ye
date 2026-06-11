<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="班组编码">
        <el-input v-model="query.teamCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="班组名称">
        <el-input v-model="query.teamName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-select v-model="query.enabled" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
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
      <template #title>生产班组</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="teamCode" label="班组编码" min-width="120" />
      <el-table-column prop="teamName" label="班组名称" min-width="140" />
      <el-table-column prop="orgCode" label="生产组织编码" min-width="120" />
      <el-table-column prop="orgName" label="生产组织名称" min-width="140" />
      <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column prop="enabled" label="启用" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
            {{ row.enabled === 1 ? '是' : '否' }}
          </el-tag>
          <el-button link type="primary" size="small" @click.stop="handleToggleEnabled(row)">
            {{ row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班组' : '新增班组'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="班组编码" prop="teamCode">
          <el-input v-model="form.teamCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="班组名称" prop="teamName">
          <el-input v-model="form.teamName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生产组织ID" prop="orgId">
          <el-input-number v-model="form.orgId" placeholder="请输入生产组织ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生产组织编码" prop="orgCode">
          <el-input v-model="form.orgCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生产组织名称" prop="orgName">
          <el-input v-model="form.orgName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
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
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import { productionTeamApi } from '@/api/team/productionTeam'
import type { ProductionTeamVO, ProductionTeamDTO, ProductionTeamQuery } from '@/types/team'

const query = reactive<ProductionTeamQuery>({
  teamCode: undefined,
  teamName: undefined,
  enabled: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<ProductionTeamVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<ProductionTeamDTO>({
  teamCode: '',
  teamName: '',
  orgId: undefined,
  orgCode: '',
  orgName: '',
  description: '',
})

const formRules = {
  teamCode: [{ required: true, message: '请输入班组编码', trigger: 'blur' }],
  teamName: [{ required: true, message: '请输入班组名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await productionTeamApi.page(query)
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

function handleEdit(row: ProductionTeamVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    teamCode: row.teamCode,
    teamName: row.teamName,
    orgId: row.orgId,
    orgCode: row.orgCode,
    orgName: row.orgName,
    description: row.description,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    teamCode: '',
    teamName: '',
    orgId: undefined,
    orgCode: '',
    orgName: '',
    description: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await productionTeamApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await productionTeamApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: ProductionTeamVO) {
  await ElMessageBox.confirm('确定要删除该班组吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await productionTeamApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } finally {
    loading.value = false
  }
}

async function handleToggleEnabled(row: ProductionTeamVO) {
  loading.value = true
  try {
    await productionTeamApi.toggleEnabled(row.id)
    ElMessage.success('操作成功')
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
