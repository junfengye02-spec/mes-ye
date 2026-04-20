<template>
  <div class="work-status-view">
    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">工单状态查询</span>
        </div>
      </template>

      <el-form :model="query" inline class="search-form">
        <el-form-item label="作业号">
          <el-input v-model="query.workNo" placeholder="作业号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="作业名称">
          <el-input v-model="query.workName" placeholder="作业名称" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="工厂">
          <el-input v-model="query.factory" placeholder="工厂" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="业务组织">
          <el-input v-model="query.businessOrg" placeholder="业务组织" clearable style="width: 140px" />
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

      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待执行" name="RELEASED" />
        <el-tab-pane label="执行中" name="IN_PROGRESS" />
        <el-tab-pane label="已完工" name="COMPLETED" />
        <el-tab-pane label="强制完工" name="FORCE_COMPLETED" />
        <el-tab-pane label="已关闭" name="CLOSED" />
      </el-tabs>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="workNo" label="作业号" min-width="120" />
        <el-table-column prop="sequenceNo" label="序号" width="80" align="center" />
        <el-table-column prop="processNo" label="工序号" min-width="100" />
        <el-table-column prop="workName" label="作业名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="isOutput" label="是否产出" width="90" align="center">
          <template #default="{ row }">{{ row.isOutput ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="processForm" label="工艺表单" min-width="120" show-overflow-tooltip />
        <el-table-column prop="processDrawing" label="工艺图纸" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(getDictType('workOrderStatus', row.status) || undefined) as any">
              {{ getDictLabel('workOrderStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />
        <el-table-column prop="furnaceNo" label="炉号" width="100" />
        <el-table-column prop="belongProcess" label="所属工序" min-width="120" show-overflow-tooltip />
        <el-table-column prop="factory" label="工厂" width="100" />
        <el-table-column prop="businessOrg" label="业务组织" min-width="120" show-overflow-tooltip />
        <el-table-column prop="planWorkCenterName" label="计划工作中心" min-width="130" show-overflow-tooltip />
        <el-table-column prop="specifiedWorkCenterName" label="指定工作中心" min-width="130" show-overflow-tooltip />
        <el-table-column prop="planTeamName" label="计划班组" min-width="120" show-overflow-tooltip />
        <el-table-column prop="planShift" label="计划班次" width="100" />
        <el-table-column prop="sourceNo" label="来源单号" min-width="120" />
        <el-table-column prop="planStartTime" label="计划开始时间" width="170" />
        <el-table-column prop="planEndTime" label="计划结束时间" width="170" />
        <el-table-column prop="actualStartTime" label="实际开始时间" width="170" />
        <el-table-column prop="actualEndTime" label="实际结束时间" width="170" />
        <el-table-column prop="issued" label="已下达" width="80" align="center">
          <template #default="{ row }">{{ row.issued ? '是' : '否' }}</template>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getDictLabel, getDictType } from '@/utils/dict'
import type { WorkStatusViewVO, WorkStatusQuery } from '@/types/query'
import { workQueryApi } from '@/api/query/workQuery'

const activeTab = ref<string>('')
const loading = ref(false)
const tableData = ref<WorkStatusViewVO[]>([])
const total = ref(0)
const query = reactive<WorkStatusQuery>({
  status: '',
  workNo: '',
  workName: '',
  factory: '',
  businessOrg: '',
  pageNum: 1,
  pageSize: 20,
})

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.workNo = ''
  query.workName = ''
  query.factory = ''
  query.businessOrg = ''
  query.status = ''
  activeTab.value = ''
  query.pageNum = 1
  fetchList()
}

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
.search-form {
  margin-bottom: 8px;
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
