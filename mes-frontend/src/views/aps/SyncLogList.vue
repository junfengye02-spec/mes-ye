<template>
  <div class="sync-log-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="同步方向">
          <el-select v-model="query.syncDirection" placeholder="请选择" clearable style="width: 140px">
            <el-option v-for="item in getDictList('syncDirection')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="同步类型">
          <el-select v-model="query.syncType" placeholder="请选择" clearable style="width: 140px">
            <el-option v-for="item in getDictList('syncType')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option v-for="item in getDictList('syncStatus')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width: 260px" @change="handleDateChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="batchId" label="批次ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="syncDirection" label="方向" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('syncDirection', row.syncDirection)">{{ getDictLabel('syncDirection', row.syncDirection) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="syncType" label="类型" width="80" align="center">
          <template #default="{ row }">{{ getDictLabel('syncType', row.syncType) }}</template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="70" align="right" />
        <el-table-column prop="successCount" label="成功" width="70" align="right" />
        <el-table-column prop="failCount" label="失败" width="70" align="right" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('syncStatus', row.status)">{{ getDictLabel('syncStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" title="同步日志详情" size="500px">
      <template v-if="currentLog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="批次ID">{{ currentLog.batchId }}</el-descriptions-item>
          <el-descriptions-item label="同步方向">{{ getDictLabel('syncDirection', currentLog.syncDirection) }}</el-descriptions-item>
          <el-descriptions-item label="同步类型">{{ getDictLabel('syncType', currentLog.syncType) }}</el-descriptions-item>
          <el-descriptions-item label="总数">{{ currentLog.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="成功">{{ currentLog.successCount }}</el-descriptions-item>
          <el-descriptions-item label="失败">{{ currentLog.failCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getDictType('syncStatus', currentLog.status)">{{ getDictLabel('syncStatus', currentLog.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ currentLog.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ currentLog.endTime }}</el-descriptions-item>
          <el-descriptions-item v-if="currentLog.errorMessage" label="错误信息">
            <el-text type="danger">{{ currentLog.errorMessage }}</el-text>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { apsSyncLogApi } from '@/api/aps/apsSync'
import { getDictList, getDictLabel, getDictType } from '@/utils/dict'
import type { ApsSyncLogVO, ApsSyncLogQuery } from '@/types/aps'

const loading = ref(false)
const tableData = ref<ApsSyncLogVO[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const drawerVisible = ref(false)
const currentLog = ref<ApsSyncLogVO | null>(null)

const query = reactive<ApsSyncLogQuery>({
  pageNum: 1,
  pageSize: 20,
  syncDirection: undefined,
  syncType: undefined,
  status: undefined,
  startTime: undefined,
  endTime: undefined,
})

function handleDateChange(val: string[] | null) {
  if (val && val.length === 2) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = undefined
    query.endTime = undefined
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await apsSyncLogApi.page(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.syncDirection = undefined
  query.syncType = undefined
  query.status = undefined
  query.startTime = undefined
  query.endTime = undefined
  dateRange.value = []
  handleSearch()
}

async function handleRowClick(row: ApsSyncLogVO) {
  try {
    const res = await apsSyncLogApi.getDetail(row.id!)
    currentLog.value = res
    drawerVisible.value = true
  } catch { /* handled by interceptor */ }
}

onMounted(() => loadData())
</script>

<style scoped>
.search-card { margin-bottom: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
