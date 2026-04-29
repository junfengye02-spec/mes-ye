<template>
  <el-card shadow="never">
    <template #header>
      <div class="table-header">
        <div class="table-title">
          <slot name="title" />
        </div>
        <div class="table-actions" role="toolbar" aria-label="表格操作工具栏">
          <slot name="toolbar" />
        </div>
      </div>
    </template>

    <!--
      无障碍说明：
      1) el-table 内部已渲染合适的 table/row/cell 语义
      2) 外层用 role="region" + aria-label 作为数据表格地标，便于屏幕阅读器定位
    -->
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      border
      stripe
      highlight-current-row
      role="region"
      aria-label="数据列表"
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

    <nav
      v-if="showPagination"
      class="pagination-wrapper"
      aria-label="分页导航"
    >
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageChange"
        @current-change="handlePageChange"
      />
    </nav>
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
