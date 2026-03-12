<template>
  <div class="inspection-work-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="检验类型">
          <el-input v-model="query.inspectionType" placeholder="检验类型" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="检验结果">
          <el-select v-model="query.result" placeholder="检验结果" clearable style="width: 120px">
            <el-option label="合格" value="合格" />
            <el-option label="不合格" value="不合格" />
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
          <span class="table-title">检验作业查询</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="inspectionNo" label="检验单号" min-width="120" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="inspectionType" label="检验类型" width="120" />
        <el-table-column prop="inspector" label="检验人" width="100" />
        <el-table-column prop="inspectionDate" label="检验日期" width="120" />
        <el-table-column prop="result" label="检验结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === '合格' ? 'success' : 'danger'">
              {{ row.result || '-' }}
            </el-tag>
          </template>
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
      title="检验作业详情"
      size="45%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="检验单号">{{ detailData.inspectionNo }}</el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detailData.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="产品编码">{{ detailData.productCode }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailData.productName }}</el-descriptions-item>
          <el-descriptions-item label="检验类型">{{ detailData.inspectionType }}</el-descriptions-item>
          <el-descriptions-item label="检验人">{{ detailData.inspector }}</el-descriptions-item>
          <el-descriptions-item label="检验日期">{{ detailData.inspectionDate }}</el-descriptions-item>
          <el-descriptions-item label="检验结果">
            <el-tag :type="detailData.result === '合格' ? 'success' : 'danger'">
              {{ detailData.result || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { InspectionWorkVO } from '@/types/query'
import { workQueryApi } from '@/api/query/workQuery'

const loading = ref(false)
const tableData = ref<InspectionWorkVO[]>([])
const total = ref(0)
const query = reactive({
  workOrderNo: '',
  inspectionType: '',
  result: '',
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
  query.workOrderNo = ''
  query.inspectionType = ''
  query.result = ''
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
