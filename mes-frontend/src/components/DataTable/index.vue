<template>
  <el-card shadow="never">
    <template #header>
      <div class="table-header">
        <div class="table-title">
          <slot name="title" />
        </div>
        <div class="table-actions">
          <slot name="toolbar" />
        </div>
      </div>
    </template>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      border
      stripe
      highlight-current-row
      @selection-change="(val: any[]) => $emit('selection-change', val)"
      @row-click="(row: any) => $emit('row-click', row)"
    >
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
      <el-table-column v-if="showSelection" type="selection" width="50" align="center" />
      <el-table-column v-if="showIndex" type="index" label="序号" width="60" align="center" />
      <slot />
    </el-table>

    <div v-if="showPagination" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageChange"
        @current-change="handlePageChange"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  data: any[]
  loading?: boolean
  total?: number
  pageNum?: number
  pageSize?: number
  showSelection?: boolean
  showIndex?: boolean
  showPagination?: boolean
}>(), {
  loading: false,
  total: 0,
  pageNum: 1,
  pageSize: 20,
  showSelection: false,
  showIndex: true,
  showPagination: true,
})

const emit = defineEmits<{
  'page-change': [params: { pageNum: number; pageSize: number }]
  'selection-change': [rows: any[]]
  'row-click': [row: any]
}>()

const tableRef = ref()
const currentPage = ref(props.pageNum)
const currentPageSize = ref(props.pageSize)

watch(() => props.pageNum, val => { currentPage.value = val })
watch(() => props.pageSize, val => { currentPageSize.value = val })

function handlePageChange() {
  emit('page-change', { pageNum: currentPage.value, pageSize: currentPageSize.value })
}
</script>

<style scoped>
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-actions {
  display: flex;
  gap: 8px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
