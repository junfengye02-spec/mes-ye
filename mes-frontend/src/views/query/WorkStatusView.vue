<template>
  <div class="work-status-view">
    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">工单状态查询</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待执行" name="RELEASED" />
        <el-tab-pane label="执行中" name="IN_PROGRESS" />
        <el-tab-pane label="已完工" name="COMPLETED" />
        <el-tab-pane label="强制完工" name="FORCE_COMPLETED" />
        <el-tab-pane label="已关闭" name="CLOSED" />
      </el-tabs>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="planQty" label="计划数量" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(getDictType('workOrderStatus', row.status) || undefined) as any">
              {{ getDictLabel('workOrderStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planStartTime" label="计划开始时间" width="170" />
        <el-table-column prop="planEndTime" label="计划结束时间" width="170" />
        <el-table-column prop="actualStartTime" label="实际开始时间" width="170" />
        <el-table-column prop="actualEndTime" label="实际结束时间" width="170" />
        <el-table-column prop="workCenterName" label="工作中心" width="120" />
        <el-table-column prop="assignedPerson" label="负责人" width="100" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDictLabel, getDictType } from '@/utils/dict'
import type { WorkStatusViewVO } from '@/types/query'
import { workQueryApi } from '@/api/query/workQuery'

const activeTab = ref<string>('')
const loading = ref(false)
const tableData = ref<WorkStatusViewVO[]>([])
const total = ref(0)
const query = reactive({
  status: '',
  pageNum: 1,
  pageSize: 20,
})

function handleTabClick(pane: any) {
  query.status = String(pane?.props?.name ?? '')
  query.pageNum = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await workQueryApi.workStatusPage(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.work-status-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
