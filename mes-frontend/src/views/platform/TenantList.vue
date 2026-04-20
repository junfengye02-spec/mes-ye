<template>
  <div class="tenant-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="租户编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
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
          <span class="table-title">租户管理</span>
          <el-button type="primary" @click="registerVisible = true">
            <el-icon><Plus /></el-icon> 新建租户
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="tenantCode" label="租户编码" min-width="140">
          <template #default="{ row }">
            <span class="code-text">{{ row.tenantCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tenantName" label="租户名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="schemaMode" label="Schema 模式" width="120" align="center">
          <template #default="{ row }">{{ row.schemaMode || '-' }}</template>
        </el-table-column>
        <el-table-column label="用户配额" width="100" align="center">
          <template #default="{ row }">{{ row.quotaUsers ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="expireAt" label="过期时间" width="170">
          <template #default="{ row }">{{ row.expireAt || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100">
          <template #default="{ row }">{{ row.contactName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === TENANT_STATUS.ACTIVE"
              link
              type="warning"
              @click="confirmAction('suspend', row)"
            >停用</el-button>
            <el-button
              v-if="row.status === TENANT_STATUS.SUSPENDED"
              link
              type="success"
              @click="confirmAction('resume', row)"
            >恢复</el-button>
            <el-button
              v-if="row.status !== TENANT_STATUS.ARCHIVED"
              link
              type="info"
              @click="confirmAction('archive', row)"
            >归档</el-button>
            <el-button link type="primary" @click="confirmAction('reprovision', row)">重新配置</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <TenantRegisterDialog v-model="registerVisible" @success="fetchList" />

    <TenantDetail v-model="detailVisible" :tenant-id="currentId" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  platformTenantApi,
  TENANT_STATUS,
  TENANT_STATUS_LABEL,
  TENANT_STATUS_TAG_TYPE,
} from '@/api/platform/tenant'
import type { TenantVO, TenantQuery } from '@/api/platform/tenant'
import TenantRegisterDialog from './TenantRegisterDialog.vue'
import TenantDetail from './TenantDetail.vue'

const loading = ref(false)
const tableData = ref<TenantVO[]>([])
const total = ref(0)
const query = reactive<TenantQuery>({
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const registerVisible = ref(false)
const detailVisible = ref(false)
const currentId = ref<number | null>(null)

const statusOptions = [
  { label: 'PENDING', value: TENANT_STATUS.PENDING },
  { label: 'ACTIVE', value: TENANT_STATUS.ACTIVE },
  { label: 'PROVISIONING', value: TENANT_STATUS.PROVISIONING },
  { label: 'SUSPENDED', value: TENANT_STATUS.SUSPENDED },
  { label: 'ARCHIVED', value: TENANT_STATUS.ARCHIVED },
]

function statusLabel(s: number): string {
  return TENANT_STATUS_LABEL[s] || String(s)
}
function statusTagType(s: number) {
  return TENANT_STATUS_TAG_TYPE[s]
}

async function fetchList() {
  loading.value = true
  try {
    const payload: TenantQuery = { ...query }
    if (payload.status === '' || payload.status === undefined || payload.status === null) {
      delete payload.status
    }
    if (!payload.keyword) delete payload.keyword
    const res = await platformTenantApi.list(payload)
    tableData.value = res?.list || []
    total.value = Number(res?.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}
function handleReset() {
  query.keyword = ''
  query.status = ''
  query.pageNum = 1
  fetchList()
}

function openDetail(row: TenantVO) {
  currentId.value = row.id
  detailVisible.value = true
}

type Action = 'suspend' | 'resume' | 'archive' | 'reprovision'
const actionConfig: Record<
  Action,
  { title: string; confirm: string; api: (id: number) => Promise<unknown>; success: string }
> = {
  suspend: {
    title: '停用租户',
    confirm: '停用后该租户所有用户登录与接口请求都将被网关拦截，确认继续？',
    api: platformTenantApi.suspend,
    success: '已停用',
  },
  resume: {
    title: '恢复租户',
    confirm: '确认恢复该租户的访问？',
    api: platformTenantApi.resume,
    success: '已恢复',
  },
  archive: {
    title: '归档租户',
    confirm: '归档后租户将只读，且无法再恢复为 ACTIVE，确认继续？',
    api: platformTenantApi.archive,
    success: '已归档',
  },
  reprovision: {
    title: '重新配置租户',
    confirm: '将重新异步执行租户初始化流程（适用于 Provision 失败的情况），确认继续？',
    api: platformTenantApi.reprovision,
    success: '已触发重新配置',
  },
}

async function confirmAction(action: Action, row: TenantVO) {
  const cfg = actionConfig[action]
  try {
    await ElMessageBox.confirm(cfg.confirm, cfg.title, {
      type: action === 'reprovision' ? 'info' : 'warning',
    })
  } catch {
    return
  }
  await cfg.api(row.id)
  ElMessage.success(cfg.success)
  fetchList()
}

onMounted(() => fetchList())
</script>

<style scoped>
.tenant-list {
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
.code-text {
  font-family: Consolas, 'Courier New', monospace;
  font-weight: 600;
}
</style>
