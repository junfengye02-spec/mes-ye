<template>
  <div class="production-work-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="作业号">
          <el-input v-model="query.workNo" placeholder="作业号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="作业名称">
          <el-input v-model="query.workName" placeholder="作业名称" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="工单ID">
          <el-input-number
            v-model="query.workOrderId"
            placeholder="工单ID"
            :controls="false"
            style="width: 120px"
          />
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
          <span class="table-title">生产作业查询</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="workNo" label="作业号" min-width="120" />
        <el-table-column prop="workName" label="作业名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="workOrderId" label="工单ID" width="100" align="center" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="productMaterial" label="产品物料" min-width="120" show-overflow-tooltip />
        <el-table-column prop="productionFactory" label="生产工厂" min-width="120" />
        <el-table-column prop="productionOrg" label="生产组织" min-width="120" show-overflow-tooltip />
        <el-table-column prop="actualStartTime" label="实际开始时间" width="170" />
        <el-table-column prop="actualEndTime" label="实际结束时间" width="170" />
        <el-table-column prop="planStartTime" label="计划开始时间" width="170" />
        <el-table-column prop="planEndTime" label="计划结束时间" width="170" />
        <el-table-column prop="actualProcessTime" label="实际加工时长" width="120" align="center" />
        <el-table-column prop="timeUnit" label="时间单位" width="90" align="center" />
        <el-table-column prop="isReportPoint" label="报工点" width="80" align="center">
          <template #default="{ row }">{{ row.isReportPoint ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="isCheckPoint" label="检验点" width="80" align="center">
          <template #default="{ row }">{{ row.isCheckPoint ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="isHandoverPoint" label="交接点" width="80" align="center">
          <template #default="{ row }">{{ row.isHandoverPoint ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
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
      title="生产作业详情"
      size="45%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="作业号">{{ detailData.workNo }}</el-descriptions-item>
          <el-descriptions-item label="作业名称">{{ detailData.workName }}</el-descriptions-item>
          <el-descriptions-item label="工单ID">{{ detailData.workOrderId }}</el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detailData.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="产品物料">{{ detailData.productMaterial }}</el-descriptions-item>
          <el-descriptions-item label="生产工厂">{{ detailData.productionFactory }}</el-descriptions-item>
          <el-descriptions-item label="生产组织">{{ detailData.productionOrg }}</el-descriptions-item>
          <el-descriptions-item label="实际开始时间">{{ detailData.actualStartTime }}</el-descriptions-item>
          <el-descriptions-item label="实际结束时间">{{ detailData.actualEndTime }}</el-descriptions-item>
          <el-descriptions-item label="计划开始时间">{{ detailData.planStartTime }}</el-descriptions-item>
          <el-descriptions-item label="计划结束时间">{{ detailData.planEndTime }}</el-descriptions-item>
          <el-descriptions-item label="实际加工时长">{{ detailData.actualProcessTime }}</el-descriptions-item>
          <el-descriptions-item label="时间单位">{{ detailData.timeUnit }}</el-descriptions-item>
          <el-descriptions-item label="报工点">{{ detailData.isReportPoint ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="检验点">{{ detailData.isCheckPoint ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="交接点">{{ detailData.isHandoverPoint ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { ProductionWorkVO, ProductionWorkQuery } from '@/types/query'
import { workQueryApi } from '@/api/query/workQuery'

const loading = ref(false)
const tableData = ref<ProductionWorkVO[]>([])
const total = ref(0)
const query = reactive<ProductionWorkQuery>({
  workNo: '',
  workName: '',
  workOrderNo: '',
  workOrderId: undefined,
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<ProductionWorkVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await workQueryApi.productionWorkPage(query)
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
  query.workOrderNo = ''
  query.workOrderId = undefined
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: ProductionWorkVO) {
  const res = await workQueryApi.productionWorkDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.production-work-query {
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
