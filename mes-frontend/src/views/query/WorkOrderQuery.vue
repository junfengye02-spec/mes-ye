<template>
  <div class="work-order-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="产品编码" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
            <el-option label="创建" value="CREATED" />
            <el-option label="已下达" value="RELEASED" />
            <el-option label="执行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="强制完工" value="FORCE_COMPLETED" />
            <el-option label="已关闭" value="CLOSED" />
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
          <span class="table-title">生产工单查询</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="workOrderNo" label="工单号" min-width="130" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="newOrRepairType" label="新制维修类型" width="120" />
        <el-table-column prop="machineModel" label="机型" width="100" />
        <el-table-column prop="productCategory" label="产品类别" width="100" />
        <el-table-column prop="planQty" label="计划数量" width="90" align="center" />
        <el-table-column prop="factoryOrg" label="工厂组织" width="110" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planStartTime" label="计划开始时间" width="170" />
        <el-table-column prop="planEndTime" label="计划结束时间" width="170" />
        <el-table-column prop="actualStartTime" label="实际开始时间" width="170" />
        <el-table-column prop="actualEndTime" label="实际结束时间" width="170" />
        <el-table-column prop="serialNo" label="序列号" width="120" />
        <el-table-column prop="createdTime" label="创建时间" width="170" />
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
      title="生产工单详情"
      size="50%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ detailData.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="产品编码">{{ detailData.productCode }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailData.productName }}</el-descriptions-item>
          <el-descriptions-item label="机型">{{ detailData.machineModel }}</el-descriptions-item>
          <el-descriptions-item label="产品类别">{{ detailData.productCategory }}</el-descriptions-item>
          <el-descriptions-item label="产品类型">{{ detailData.productType }}</el-descriptions-item>
          <el-descriptions-item label="新制维修类型">{{ detailData.newOrRepairType }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ detailData.planQty }} {{ detailData.qtyUnit }}</el-descriptions-item>
          <el-descriptions-item label="工厂组织">{{ detailData.factoryOrg }}</el-descriptions-item>
          <el-descriptions-item label="计划组织">{{ detailData.planOrg }}</el-descriptions-item>
          <el-descriptions-item label="主制组织">{{ detailData.mainOrg }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailData.status)">{{ getStatusLabel(detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="计划开始">{{ detailData.planStartTime }}</el-descriptions-item>
          <el-descriptions-item label="计划结束">{{ detailData.planEndTime }}</el-descriptions-item>
          <el-descriptions-item label="实际开始">{{ detailData.actualStartTime }}</el-descriptions-item>
          <el-descriptions-item label="实际结束">{{ detailData.actualEndTime }}</el-descriptions-item>
          <el-descriptions-item label="序列号">{{ detailData.serialNo }}</el-descriptions-item>
          <el-descriptions-item label="订单编号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { WorkOrderVO, WorkOrderQuery } from '@/types/workorder'
import { workOrderApi } from '@/api/workorder/workOrder'

const statusMap: Record<string, { label: string; type: string }> = {
  CREATED: { label: '创建', type: 'info' },
  RELEASED: { label: '已下达', type: '' },
  IN_PROGRESS: { label: '执行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  FORCE_COMPLETED: { label: '强制完工', type: 'danger' },
  CLOSED: { label: '已关闭', type: 'info' },
}

function getStatusLabel(status?: string) {
  return statusMap[status || '']?.label || status || '-'
}

function getStatusType(status?: string): any {
  return statusMap[status || '']?.type ?? 'info'
}

const loading = ref(false)
const tableData = ref<WorkOrderVO[]>([])
const total = ref(0)
const query = reactive<WorkOrderQuery>({
  workOrderNo: '',
  productCode: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<WorkOrderVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await workOrderApi.page(query)
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
  query.productCode = ''
  query.status = ''
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: WorkOrderVO) {
  const res = await workOrderApi.getDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.work-order-query {
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
