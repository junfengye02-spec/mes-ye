<template>
  <div class="recheck-request-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="项目编码">
          <el-input v-model="query.projectCode" placeholder="项目编码" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料编码">
          <el-input v-model="query.materialCode" placeholder="物料编码" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 120px">
            <el-option
              v-for="item in getDictList('recheckStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">复检申请列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="projectCode" label="项目编码" min-width="120" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="productionOrderNo" label="生产订单号" min-width="140" />
        <el-table-column prop="recheckReason" label="复检原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="recheckProposer" label="发起人" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status" :type="getDictType('recheckStatus', row.status) as any">
              {{ getDictLabel('recheckStatus', row.status) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑复检申请' : '新增复检申请'"
      width="560px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="项目编码" prop="projectCode">
          <el-input v-model="form.projectCode" placeholder="项目编码" />
        </el-form-item>
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="项目名称" />
        </el-form-item>
        <el-form-item label="物料编码" prop="materialCode">
          <el-input v-model="form.materialCode" placeholder="物料编码" />
        </el-form-item>
        <el-form-item label="物料名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="物料名称" />
        </el-form-item>
        <el-form-item label="生产订单号" prop="productionOrderNo">
          <el-input v-model="form.productionOrderNo" placeholder="生产订单号" />
        </el-form-item>
        <el-form-item label="复检原因" prop="recheckReason">
          <el-input v-model="form.recheckReason" type="textarea" :rows="3" placeholder="复检原因" />
        </el-form-item>
        <el-form-item label="复检要求" prop="recheckRequirement">
          <el-input v-model="form.recheckRequirement" type="textarea" :rows="3" placeholder="复检要求" />
        </el-form-item>
        <el-form-item label="发起人" prop="recheckProposer">
          <el-input v-model="form.recheckProposer" placeholder="发起人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitForm">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { RecheckRequestVO, RecheckRequestDTO, RecheckRequestQuery } from '@/types/quality'
import { recheckRequestApi } from '@/api/quality/recheckRequest'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<RecheckRequestVO[]>([])
const total = ref(0)
const query = reactive<RecheckRequestQuery>({
  projectCode: '',
  materialCode: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<RecheckRequestDTO>({
  projectCode: '',
  projectName: '',
  materialCode: '',
  materialName: '',
  productionOrderNo: '',
  recheckRequirement: '',
  recheckReason: '',
  recheckProposer: '',
})
const formRules: FormRules = {
  productionOrderNo: [{ required: true, message: '请输入生产订单号', trigger: 'blur' }],
}

async function fetchList() {
  loading.value = true
  try {
    const res = await recheckRequestApi.page(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.projectCode = ''
  query.materialCode = ''
  query.status = ''
  query.pageNum = 1
  fetchList()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: RecheckRequestVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    projectCode: row.projectCode,
    projectName: row.projectName,
    materialCode: row.materialCode,
    materialName: row.materialName,
    productionOrderNo: row.productionOrderNo,
    recheckRequirement: row.recheckRequirement,
    recheckReason: row.recheckReason,
    recheckProposer: row.recheckProposer,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    projectCode: '',
    projectName: '',
    materialCode: '',
    materialName: '',
    productionOrderNo: '',
    recheckRequirement: '',
    recheckReason: '',
    recheckProposer: '',
  })
}

async function handleSubmitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && editId.value !== null) {
      await recheckRequestApi.update(editId.value!, form)
      ElMessage.success('更新成功')
    } else {
      await recheckRequestApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: RecheckRequestVO) {
  try {
    await ElMessageBox.confirm('确定要删除该复检申请吗？', '提示', {
      type: 'warning',
    })
    await recheckRequestApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch { /* user cancelled */ }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.recheck-request-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-title {
  font-weight: 600;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
