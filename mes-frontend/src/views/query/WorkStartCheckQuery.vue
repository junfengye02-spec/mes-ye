<template>
  <div class="work-start-check-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="工单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="检查状态">
          <el-select v-model="query.checkStatus" placeholder="检查状态" clearable style="width: 120px">
            <el-option label="通过" value="PASSED" />
            <el-option label="不通过" value="FAILED" />
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
          <span class="table-title">生产工作开工检查实绩</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="workOrderNo" label="工单号" min-width="140" />
        <el-table-column prop="workNo" label="工作编号" min-width="120" />
        <el-table-column prop="checkItem" label="检查项" min-width="130" show-overflow-tooltip />
        <el-table-column prop="checkResult" label="检查结果" min-width="120" show-overflow-tooltip />
        <el-table-column prop="checkStatus" label="检查状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.checkStatus === 'PASSED' ? 'success' : 'danger'">
              {{ row.checkStatus === 'PASSED' ? '通过' : row.checkStatus === 'FAILED' ? '不通过' : row.checkStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checker" label="检查人" width="100" />
        <el-table-column prop="checkTime" label="检查时间" width="170" />
        <el-table-column prop="checkRemark" label="检查备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
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
      title="开工检查详情"
      size="40%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ detailData.workOrderNo }}</el-descriptions-item>
          <el-descriptions-item label="工作编号">{{ detailData.workNo }}</el-descriptions-item>
          <el-descriptions-item label="检查项" :span="2">{{ detailData.checkItem }}</el-descriptions-item>
          <el-descriptions-item label="检查人">{{ detailData.checker }}</el-descriptions-item>
          <el-descriptions-item label="检查时间">{{ detailData.checkTime }}</el-descriptions-item>
          <el-descriptions-item label="检查结果" :span="2">{{ detailData.checkResult }}</el-descriptions-item>
          <el-descriptions-item label="检查状态">
            <el-tag :type="detailData.checkStatus === 'PASSED' ? 'success' : 'danger'">
              {{ detailData.checkStatus === 'PASSED' ? '通过' : detailData.checkStatus === 'FAILED' ? '不通过' : detailData.checkStatus || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createdTime }}</el-descriptions-item>
          <el-descriptions-item label="检查备注" :span="2">{{ detailData.checkRemark }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { WorkStartCheckVO, WorkStartCheckQuery } from '@/types/quality'
import { workStartCheckApi } from '@/api/quality/workStartCheck'

const loading = ref(false)
const tableData = ref<WorkStartCheckVO[]>([])
const total = ref(0)
const query = reactive<WorkStartCheckQuery>({
  workOrderNo: '',
  checkStatus: '',
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<WorkStartCheckVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await workStartCheckApi.page(query)
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
  query.checkStatus = ''
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: WorkStartCheckVO) {
  const res = await workStartCheckApi.getDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.work-start-check-query {
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
