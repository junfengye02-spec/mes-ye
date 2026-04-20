<template>
  <div class="inspection-work-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="作业号">
          <el-input v-model="query.workNo" placeholder="作业号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="作业名称">
          <el-input v-model="query.workName" placeholder="作业名称" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="作业状态">
          <el-input v-model="query.workStatus" placeholder="作业状态" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="工单ID">
          <el-input-number
            v-model="query.workOrderId"
            placeholder="工单ID"
            :controls="false"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="检验类别">
          <el-input v-model="query.inspectCategory" placeholder="检验类别" clearable style="width: 140px" />
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
          <span class="table-title">检验作业查询</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="workNo" label="作业号" min-width="120" />
        <el-table-column prop="workName" label="作业名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="planInspectQty" label="计划检验数量" width="110" align="center" />
        <el-table-column prop="inspectedQty" label="已检数量" width="100" align="center" />
        <el-table-column prop="qualifiedQty" label="合格数量" width="100" align="center" />
        <el-table-column prop="unqualifiedQty" label="不合格数量" width="110" align="center" />
        <el-table-column prop="judgment" label="判定" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.judgment" :type="row.judgment === '合格' ? 'success' : 'danger'">
              {{ row.judgment }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="isCheckPoint" label="检验点" width="80" align="center">
          <template #default="{ row }">{{ row.isCheckPoint ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="dispatchStatus" label="派工状态" width="100" />
        <el-table-column prop="workStatus" label="作业状态" width="100" />
        <el-table-column prop="inspectType" label="检验类型" width="120" />
        <el-table-column prop="inspectCategory" label="检验类别" width="120" />
        <el-table-column prop="qcOrg" label="质检组织" min-width="120" show-overflow-tooltip />
        <el-table-column prop="inspectFactory" label="检验工厂" min-width="120" />
        <el-table-column prop="planTeamLab" label="计划班组/实验室" min-width="130" show-overflow-tooltip />
        <el-table-column prop="actualStartTime" label="实际开始时间" width="170" />
        <el-table-column prop="actualEndTime" label="实际结束时间" width="170" />
        <el-table-column prop="isReportPoint" label="报工点" width="80" align="center">
          <template #default="{ row }">{{ row.isReportPoint ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="workOrderId" label="工单ID" width="100" align="center" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="orderStatus" label="订单状态" width="100" />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
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

    <el-drawer
      v-model="drawerVisible"
      title="检验作业详情"
      size="45%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="作业号">{{ detailData.workNo }}</el-descriptions-item>
          <el-descriptions-item label="作业名称">{{ detailData.workName }}</el-descriptions-item>
          <el-descriptions-item label="计划检验数量">{{ detailData.planInspectQty }}</el-descriptions-item>
          <el-descriptions-item label="已检数量">{{ detailData.inspectedQty }}</el-descriptions-item>
          <el-descriptions-item label="合格数量">{{ detailData.qualifiedQty }}</el-descriptions-item>
          <el-descriptions-item label="不合格数量">{{ detailData.unqualifiedQty }}</el-descriptions-item>
          <el-descriptions-item label="判定">
            <el-tag v-if="detailData.judgment" :type="detailData.judgment === '合格' ? 'success' : 'danger'">
              {{ detailData.judgment }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="检验点">{{ detailData.isCheckPoint ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="派工状态">{{ detailData.dispatchStatus }}</el-descriptions-item>
          <el-descriptions-item label="作业状态">{{ detailData.workStatus }}</el-descriptions-item>
          <el-descriptions-item label="检验类型">{{ detailData.inspectType }}</el-descriptions-item>
          <el-descriptions-item label="检验类别">{{ detailData.inspectCategory }}</el-descriptions-item>
          <el-descriptions-item label="质检组织">{{ detailData.qcOrg }}</el-descriptions-item>
          <el-descriptions-item label="检验工厂">{{ detailData.inspectFactory }}</el-descriptions-item>
          <el-descriptions-item label="计划班组/实验室">{{ detailData.planTeamLab }}</el-descriptions-item>
          <el-descriptions-item label="实际开始时间">{{ detailData.actualStartTime }}</el-descriptions-item>
          <el-descriptions-item label="实际结束时间">{{ detailData.actualEndTime }}</el-descriptions-item>
          <el-descriptions-item label="报工点">{{ detailData.isReportPoint ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="工单ID">{{ detailData.workOrderId }}</el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detailData.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ detailData.orderStatus }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailData.description }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { InspectionWorkVO, InspectionWorkQuery } from '@/types/query'
import { workQueryApi } from '@/api/query/workQuery'

const loading = ref(false)
const tableData = ref<InspectionWorkVO[]>([])
const total = ref(0)
const query = reactive<InspectionWorkQuery>({
  workNo: '',
  workName: '',
  workStatus: '',
  workOrderId: undefined,
  inspectCategory: '',
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<InspectionWorkVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await workQueryApi.inspectionWorkPage(query)
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
  query.workNo = ''
  query.workName = ''
  query.workStatus = ''
  query.workOrderId = undefined
  query.inspectCategory = ''
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: InspectionWorkVO) {
  const res = await workQueryApi.inspectionWorkDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.inspection-work-query {
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
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
