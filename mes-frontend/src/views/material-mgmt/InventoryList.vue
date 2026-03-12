<template>
  <div class="inventory-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="仓库">
          <el-input v-model="query.warehouse" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="存储地点">
          <el-input v-model="query.storageLocation" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料编码">
          <el-input v-model="query.materialCode" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="query.materialName" placeholder="请输入" clearable style="width: 160px" />
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
        <span>存储地点库存</span>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="factory" label="工厂" min-width="100" />
        <el-table-column prop="warehouse" label="仓库" min-width="120" />
        <el-table-column prop="storageLocation" label="存储地点" min-width="120" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="unrestrictedStock" label="非限制库存" width="110" align="right" />
        <el-table-column prop="qualityStock" label="质检库存" width="100" align="right" />
        <el-table-column prop="frozenStock" label="冻结库存" width="100" align="right" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { StorageInventoryVO, InventoryQuery } from '@/types/material-mgmt'
import { inventoryApi } from '@/api/material-mgmt/inventory'

const loading = ref(false)
const list = ref<StorageInventoryVO[]>([])
const total = ref(0)
const query = reactive<InventoryQuery>({
  warehouse: '',
  storageLocation: '',
  materialCode: '',
  materialName: '',
  pageNum: 1,
  pageSize: 20,
})

async function loadList() {
  loading.value = true
  try {
    const res = await inventoryApi.page(query)
    list.value = res?.list ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.warehouse = ''
  query.storageLocation = ''
  query.materialCode = ''
  query.materialName = ''
  query.pageNum = 1
  loadList()
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.inventory-list {
  padding: 16px;
}
.search-card {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
