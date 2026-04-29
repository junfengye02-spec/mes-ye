<template>
  <div class="shift-handover-list">
    <el-card shadow="never" class="search-card" role="search" aria-label="班次交接查询条件">
      <el-form :model="query" inline>
        <el-form-item label="项目名称">
          <el-input v-model="query.projectName" placeholder="项目名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="交接日期">
          <el-date-picker
            v-model="query.handoverDate"
            type="date"
            placeholder="交接日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option
              v-for="item in getDictList('handoverStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon aria-hidden="true"><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon aria-hidden="true"><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header" role="toolbar" aria-label="班次交接操作工具栏">
          <span class="table-title">班次交接列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon aria-hidden="true"><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        role="region"
        aria-label="班次交接列表"
      >
        <el-table-column prop="projectName" label="项目名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productSerialNo" label="产品序列号" min-width="120" />
        <el-table-column prop="handoverDate" label="交接日期" width="120" />
        <el-table-column prop="handoverTeamName" label="交接班组" min-width="120" show-overflow-tooltip />
        <el-table-column prop="handoverPerson" label="交接人" width="100" />
        <el-table-column prop="takeoverPerson" label="接班人" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('handoverStatus', row.status) as any">
              {{ getDictLabel('handoverStatus', row.status) || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handoverContent" label="交接内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{
              row.handoverContent
                ? row.handoverContent.length > 50
                  ? row.handoverContent.slice(0, 50) + '...'
                  : row.handoverContent
                : '-'
            }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="primary"
              size="small"
              @click="handleReceive(row)"
            >
              接收
            </el-button>
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
      :title="isEdit ? '编辑班次交接' : '新增班次交接'"
      width="560px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="项目名称" />
        </el-form-item>
        <el-form-item label="产品序列号" prop="productSerialNo">
          <el-input v-model="form.productSerialNo" placeholder="产品序列号" />
        </el-form-item>
        <el-form-item label="交接日期" prop="handoverDate">
          <el-date-picker
            v-model="form.handoverDate"
            type="date"
            placeholder="交接日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="交接人" prop="handoverPerson">
          <el-input v-model="form.handoverPerson" placeholder="交接人" />
        </el-form-item>
        <el-form-item label="接班人" prop="takeoverPerson">
          <el-input v-model="form.takeoverPerson" placeholder="接班人" />
        </el-form-item>
        <el-form-item label="交接班组" prop="handoverTeamName">
          <el-input v-model="form.handoverTeamName" placeholder="交接班组" />
        </el-form-item>
        <el-form-item label="交接内容" prop="handoverContent">
          <el-input v-model="form.handoverContent" type="textarea" :rows="5" placeholder="交接内容" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="状态" style="width: 100%">
            <el-option
              v-for="item in getDictList('handoverStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { ShiftHandoverVO, ShiftHandoverDTO, ShiftHandoverQuery } from '@/types/quality'
import { shiftHandoverApi } from '@/api/quality/shiftHandover'

type ShiftHandoverForm = ShiftHandoverDTO & { status?: string }

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<ShiftHandoverVO[]>([])
const total = ref(0)
const query = reactive<ShiftHandoverQuery>({
  projectName: '',
  handoverDate: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<ShiftHandoverForm>({
  projectName: '',
  productSerialNo: '',
  handoverDate: '',
  handoverPerson: '',
  takeoverPerson: '',
  handoverTeamName: '',
  handoverContent: '',
  status: '',
})
const formRules: FormRules = {
  handoverPerson: [{ required: true, message: '请输入交接人', trigger: 'blur' }],
}

async function fetchList() {
  loading.value = true
  try {
    const res = await shiftHandoverApi.page(query)
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
  query.projectName = ''
  query.handoverDate = ''
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

function handleEdit(row: ShiftHandoverVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    projectName: row.projectName,
    productSerialNo: row.productSerialNo,
    handoverDate: row.handoverDate,
    handoverPerson: row.handoverPerson,
    takeoverPerson: row.takeoverPerson,
    handoverTeamName: row.handoverTeamName,
    handoverContent: row.handoverContent,
    status: row.status,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    projectName: '',
    productSerialNo: '',
    handoverDate: '',
    handoverPerson: '',
    takeoverPerson: '',
    handoverTeamName: '',
    handoverContent: '',
    status: '',
  })
}

async function handleSubmitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const { status, ...rest } = form
    const payload: ShiftHandoverDTO = { ...rest }
    if (isEdit.value && editId.value !== null) {
      await shiftHandoverApi.update(editId.value!, payload)
      ElMessage.success('更新成功')
    } else {
      await shiftHandoverApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

async function handleReceive(row: ShiftHandoverVO) {
  if (row.status !== 'PENDING') return
  await shiftHandoverApi.receive(row.id)
  ElMessage.success('接收成功')
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.shift-handover-list {
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
