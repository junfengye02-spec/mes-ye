<template>
  <div class="sync-config-list">
    <!-- APS Status Banner -->
    <el-card shadow="never" class="status-banner">
      <div class="status-content">
        <div class="status-item">
          <span class="status-dot" :class="status.apsAvailable ? 'available' : 'unavailable'" />
          <span>APS {{ status.apsAvailable ? '可用' : '不可用' }}</span>
        </div>
        <div class="status-item">
          <span>熔断状态：</span>
          <span>{{ status.circuitBreakerState || '-' }}</span>
        </div>
        <div class="status-item">
          <span>待处理队列：</span>
          <span>{{ status.pendingQueueCount ?? 0 }}</span>
        </div>
        <div class="status-actions">
          <el-button
            type="primary"
            :loading="downstreamLoading"
            @click="handleTriggerDownstream"
          >
            手动下行同步
          </el-button>
          <el-button
            type="success"
            :loading="upstreamLoading"
            @click="handleTriggerUpstream"
          >
            手动上行同步
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="配置键">
          <el-input v-model="query.configKey" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="query.description" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="启用">
          <el-select v-model="query.enabled" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('yesNo')"
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
          <span>APS 同步配置</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="configKey" label="配置键" min-width="160" />
        <el-table-column prop="configValue" label="配置值" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column prop="enabled" label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @update:model-value="(val: string | number | boolean) => handleEnabledChange(row, !!val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入配置键" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="formEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getDictList } from '@/utils/dict'
import type { ApsSyncConfigVO, ApsSyncConfigDTO, ApsSyncConfigQuery, ApsSyncStatusVO } from '@/types/aps'
import { apsSyncConfigApi, apsSyncApi } from '@/api/aps/apsSync'

const loading = ref(false)
const list = ref<ApsSyncConfigVO[]>([])
const total = ref(0)
const query = reactive<ApsSyncConfigQuery>({
  configKey: '',
  description: '',
  enabled: undefined,
  pageNum: 1,
  pageSize: 20,
})

const status = ref<ApsSyncStatusVO>({
  apsAvailable: false,
  circuitBreakerState: '',
  pendingQueueCount: 0,
})
const downstreamLoading = ref(false)
const upstreamLoading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增配置')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const form = reactive<ApsSyncConfigDTO>({
  configKey: '',
  configValue: '',
  description: '',
  enabled: 1,
})

const formEnabled = computed({
  get: () => form.enabled === 1,
  set: (val: boolean) => { form.enabled = val ? 1 : 0 },
})

const rules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

async function loadStatus() {
  try {
    const res = await apsSyncApi.getStatus()
    status.value = res ?? status.value
  } catch {
    status.value = { apsAvailable: false, circuitBreakerState: '未知', pendingQueueCount: 0 }
  }
}

async function handleTriggerDownstream() {
  downstreamLoading.value = true
  try {
    await apsSyncApi.triggerDownstream()
    ElMessage.success('下行同步已触发')
    loadStatus()
    loadList()
  } finally {
    downstreamLoading.value = false
  }
}

async function handleTriggerUpstream() {
  upstreamLoading.value = true
  try {
    await apsSyncApi.triggerUpstream()
    ElMessage.success('上行同步已触发')
    loadStatus()
    loadList()
  } finally {
    upstreamLoading.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await apsSyncConfigApi.page(query)
    list.value = res?.list ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.configKey = ''
  query.description = ''
  query.enabled = undefined
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增配置'
  editId.value = null
  Object.assign(form, {
    configKey: '',
    configValue: '',
    description: '',
    enabled: 1,
  })
  dialogVisible.value = true
}

function handleEdit(row: ApsSyncConfigVO) {
  dialogTitle.value = '编辑配置'
  editId.value = row.id
  Object.assign(form, {
    configKey: row.configKey,
    configValue: row.configValue,
    description: row.description ?? '',
    enabled: row.enabled ?? 1,
  })
  dialogVisible.value = true
}

async function handleEnabledChange(row: ApsSyncConfigVO, val: boolean) {
  try {
    await apsSyncConfigApi.update(row.id, { ...row, enabled: val ? 1 : 0 })
    ElMessage.success('更新成功')
    loadList()
  } catch {
    // error handled by request interceptor
  }
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editId.value) {
      await apsSyncConfigApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await apsSyncConfigApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ApsSyncConfigVO) {
  ElMessageBox.confirm('确定要删除该配置吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await apsSyncConfigApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadStatus()
  loadList()
})
</script>

<style scoped>
.sync-config-list {
  padding: 16px;
}
.status-banner {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
}
.status-content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
}
.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.status-dot.available {
  background: #67c23a;
}
.status-dot.unavailable {
  background: #f56c6c;
}
.status-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.search-card {
  margin-bottom: 16px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
