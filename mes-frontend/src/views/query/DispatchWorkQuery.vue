<template>
  <div class="dispatch-work-query">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="订单编号">
          <el-input v-model="query.orderNo" placeholder="订单编号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="工序号">
          <el-input v-model="query.processNo" placeholder="工序号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.dispatchStatus" placeholder="状态" clearable style="width: 130px">
            <el-option
              v-for="item in getDictList('dispatchStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
          <span class="table-title">派工工作查询</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe @row-click="handleRowClick">
        <el-table-column prop="orderNo" label="订单编号" min-width="130" />
        <el-table-column prop="processNo" label="工序号" min-width="100" />
        <el-table-column prop="workName" label="工作名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="projectName" label="项目" min-width="120" show-overflow-tooltip />
        <el-table-column prop="planQty" label="计划数量" width="90" align="center" />
        <el-table-column prop="qtyUnit" label="单位" width="70" align="center" />
        <el-table-column prop="dispatchStatus" label="分派状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('dispatchStatus', row.dispatchStatus)">
              {{ getDictLabel('dispatchStatus', row.dispatchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planStartTime" label="计划开始时间" width="170" />
        <el-table-column prop="planEndTime" label="计划结束时间" width="170" />
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

    <el-drawer v-model="drawerVisible" title="派工任务详情" size="45%" destroy-on-close @close="detailData = null">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="工序号">{{ detailData.processNo }}</el-descriptions-item>
          <el-descriptions-item label="工作名称">{{ detailData.workName }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ detailData.projectName }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ detailData.planQty }} {{ detailData.qtyUnit }}</el-descriptions-item>
          <el-descriptions-item label="分派状态">
            <el-tag :type="getDictType('dispatchStatus', detailData.dispatchStatus)">
              {{ getDictLabel('dispatchStatus', detailData.dispatchStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="计划开始">{{ detailData.planStartTime }}</el-descriptions-item>
          <el-descriptions-item label="计划结束">{{ detailData.planEndTime }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createdTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getDictList, getDictLabel, getDictType } from '@/utils/dict'
import type { DispatchTaskVO, DispatchTaskQuery } from '@/types/dispatch'
import { workQueryApi } from '@/api/query/workQuery'

const loading = ref(false)
const tableData = ref<DispatchTaskVO[]>([])
const total = ref(0)
const query = reactive<DispatchTaskQuery>({
  orderNo: '',
  processNo: '',
  dispatchStatus: undefined,
  pageNum: 1,
  pageSize: 20,
})

const drawerVisible = ref(false)
const detailData = ref<DispatchTaskVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await workQueryApi.dispatchWorkPage(query)
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
  query.orderNo = ''
  query.processNo = ''
  query.dispatchStatus = undefined
  query.pageNum = 1
  fetchList()
}

async function handleRowClick(row: DispatchTaskVO) {
  const res = await workQueryApi.dispatchWorkDetail(Number(row.id))
  detailData.value = res
  drawerVisible.value = true
}

onMounted(() => { fetchList() })
</script>

<style scoped>
.dispatch-work-query { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-title { font-weight: 600; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
:deep(.el-table__row) { cursor: pointer; }
</style>
