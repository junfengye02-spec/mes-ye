<template>
  <div class="shift-handover-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="项目名称">
          <el-input v-model="query.projectName" placeholder="项目名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="交接日期">
          <el-date-picker
            v-model="query.handoverDate"
            type="date"
            placeholder="交接日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="待接收" value="待接收" />
            <el-option label="已接收" value="已接收" />
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
          <span class="table-title">交班记录查询</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="projectName" label="项目名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productSerialNo" label="产品序列号" min-width="120" />
        <el-table-column prop="handoverDate" label="交接日期" width="120" />
        <el-table-column prop="handoverTeamName" label="交接班组" min-width="120" show-overflow-tooltip />
        <el-table-column prop="handoverPerson" label="交接人" width="100" />
        <el-table-column prop="takeoverPerson" label="接班人" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('handoverStatus', row.status) as any">
              {{ getDictLabel('handoverStatus', row.status) || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handoverContent" label="交接内容" min-width="180" show-overflow-tooltip />
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
      title="交班记录详情"
      size="45%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ detailData.projectName }}</el-descriptions-item>
          <el-descriptions-item label="产品序列号">{{ detailData.productSerialNo }}</el-descriptions-item>
          <el-descriptions-item label="交接日期">{{ detailData.handoverDate }}</el-descriptions-item>
          <el-descriptions-item label="交接班组">{{ detailData.handoverTeamName }}</el-descriptions-item>
          <el-descriptions-item label="交接人">{{ detailData.handoverPerson }}</el-descriptions-item>
          <el-descriptions-item label="接班人">{{ detailData.takeoverPerson }}</el-descriptions-item>
          <el-descriptions-item label="交接时间">{{ detailData.handoverTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getDictType('handoverStatus', detailData.status) as any">
              {{ getDictLabel('handoverStatus', detailData.status) || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="交接内容" :span="2">{{ detailData.handoverContent }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDictLabel, getDictType } from '@/utils/dict'
import type { ShiftHandoverVO, ShiftHandoverQuery } from '@/types/quality'
import { workQueryApi } from '@/api/query/workQuery'

const loading = ref(false)
const tableData = ref<ShiftHandoverVO[]>([])
const total = ref(0)
const query = reactive<ShiftHandoverQuery>({
  projectName: '',
  handoverDate: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<ShiftHandoverVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await workQueryApi.shiftHandoverPage(query)
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
  query.projectName = ''
  query.handoverDate = ''
  query.status = ''
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: ShiftHandoverVO) {
  const res = await workQueryApi.shiftHandoverDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.shift-handover-query {
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
