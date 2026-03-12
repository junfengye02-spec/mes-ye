<template>
  <div class="shift-handover-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="交接人">
          <el-input v-model="query.handoverPerson" placeholder="交接人" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="待接收" value="待接收" />
            <el-option label="已接收" value="已接收" />
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
          <span class="table-title">班次交接列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="handoverNo" label="交接单号" min-width="120" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="handoverPerson" label="交接人" width="100" />
        <el-table-column prop="receivePerson" label="接收人" width="100" />
        <el-table-column prop="handoverTime" label="交接时间" width="170" />
        <el-table-column prop="receiveTime" label="接收时间" width="170" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '已接收' ? 'success' : 'warning'">
              {{ row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="交接内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.content ? (row.content.length > 50 ? row.content.slice(0, 50) + '...' : row.content) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === '待接收'"
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
        <el-form-item label="工单号" prop="workOrderNo">
          <el-input v-model="form.workOrderNo" placeholder="输入工单号" />
        </el-form-item>
        <el-form-item label="交接人" prop="handoverPerson">
          <el-input v-model="form.handoverPerson" placeholder="交接人" />
        </el-form-item>
        <el-form-item label="交接内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="交接内容" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
import type { ShiftHandoverVO, ShiftHandoverDTO } from '@/types/quality'
import { shiftHandoverApi } from '@/api/quality/shiftHandover'
import { workOrderApi } from '@/api/workorder/workOrder'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<ShiftHandoverVO[]>([])
const total = ref(0)
const query = reactive({
  workOrderNo: '',
  handoverPerson: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<ShiftHandoverDTO & { workOrderNo?: string }>({
  workOrderId: undefined,
  workOrderNo: '',
  handoverPerson: '',
  content: '',
  remark: '',
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
  query.workOrderNo = ''
  query.handoverPerson = ''
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
    workOrderId: row.workOrderId,
    workOrderNo: row.workOrderNo,
    handoverPerson: row.handoverPerson,
    content: row.content,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    workOrderId: undefined,
    workOrderNo: '',
    handoverPerson: '',
    content: '',
    remark: '',
  })
}

async function resolveWorkOrderId(workOrderNo: string): Promise<number | undefined> {
  const res = await workOrderApi.page({ workOrderNo, pageNum: 1, pageSize: 1 })
  return res?.list?.[0]?.id
}

async function handleSubmitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const workOrderId = form.workOrderId ?? (form.workOrderNo ? await resolveWorkOrderId(form.workOrderNo) : undefined)
      const payload: ShiftHandoverDTO = {
        workOrderId,
        handoverPerson: form.handoverPerson,
        content: form.content,
        remark: form.remark,
      }
      if (isEdit.value && editId.value) {
        await shiftHandoverApi.update(editId.value, payload)
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
  })
}

async function handleReceive(row: ShiftHandoverVO) {
  if (row.status !== '待接收') return
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
