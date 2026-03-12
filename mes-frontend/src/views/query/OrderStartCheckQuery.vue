<template>
  <div class="order-start-check-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="检查结果">
          <el-select v-model="query.checkResult" placeholder="检查结果" clearable style="width: 120px">
            <el-option label="已通过" value="已通过" />
            <el-option label="未通过" value="未通过" />
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
          <span class="table-title">生产工单开工检查实绩</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="checkNo" label="检查单号" min-width="140" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="140" />
        <el-table-column prop="checkResult" label="开工检查状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.checkResult === '已通过' ? 'success' : 'danger'">
              {{ row.checkResult || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checker" label="检查人" width="100" />
        <el-table-column prop="checkDate" label="检查日期" width="120" />
        <el-table-column prop="remark" label="开工检查备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="updatedTime" label="修改时间" width="170" />
        <el-table-column prop="updatedBy" label="修改人" width="100" />
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
      title="工单开工检查详情"
      size="40%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="检查单号">{{ detailData.checkNo }}</el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detailData.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="检查人">{{ detailData.checker }}</el-descriptions-item>
          <el-descriptions-item label="检查日期">{{ detailData.checkDate }}</el-descriptions-item>
          <el-descriptions-item label="检查结果">
            <el-tag :type="detailData.checkResult === '已通过' ? 'success' : 'danger'">
              {{ detailData.checkResult || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createdTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { OrderStartCheckVO, OrderStartCheckQuery } from '@/types/quality'
import { orderStartCheckApi } from '@/api/quality/orderStartCheck'

const loading = ref(false)
const tableData = ref<OrderStartCheckVO[]>([])
const total = ref(0)
const query = reactive<OrderStartCheckQuery>({
  workOrderNo: '',
  checkResult: '',
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<OrderStartCheckVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await orderStartCheckApi.page(query)
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
  query.checkResult = ''
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: OrderStartCheckVO) {
  const res = await orderStartCheckApi.getDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.order-start-check-query {
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
