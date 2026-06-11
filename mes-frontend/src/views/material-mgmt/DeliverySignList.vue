<template>
  <div class="delivery-sign-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物料编码">
          <el-input v-model="query.materialCode" placeholder="请输入" clearable style="width: 160px" />
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
          <span>发货签收列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="lineNo" label="行号" width="90" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="140" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="planDeliveryQty" label="计划发货数量" width="120" align="right" />
        <el-table-column prop="pendingSignQty" label="待签收数量" width="120" align="right" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="deliveryWarehouse" label="发货仓库" min-width="120" />
        <el-table-column prop="deliverer" label="发货人" width="100" />
        <el-table-column prop="deliveryTime" label="发货时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleConfirmSign(row)"
            >
              确认签收
            </el-button>
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="dialogVisible"
      title="新增发货签收"
      width="520px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="行号" prop="lineNo">
          <el-input v-model="form.lineNo" placeholder="请输入行号" />
        </el-form-item>
        <el-form-item label="工单ID" prop="workOrderId">
          <el-input-number v-model="form.workOrderId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工单号" prop="workOrderNo">
          <el-input v-model="form.workOrderNo" placeholder="请输入工单号" />
        </el-form-item>
        <el-form-item label="物料ID" prop="materialId">
          <el-input-number v-model="form.materialId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="物料编码" prop="materialCode">
          <el-input v-model="form.materialCode" placeholder="请输入物料编码" />
        </el-form-item>
        <el-form-item label="物料名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="请输入物料名称" />
        </el-form-item>
        <el-form-item label="计划发货数量" prop="planDeliveryQty">
          <el-input-number v-model="form.planDeliveryQty" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="待签收数量" prop="pendingSignQty">
          <el-input-number v-model="form.pendingSignQty" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="发货仓库" prop="deliveryWarehouse">
          <el-input v-model="form.deliveryWarehouse" placeholder="请输入发货仓库" />
        </el-form-item>
        <el-form-item label="发货存储地点" prop="deliveryLocation">
          <el-input v-model="form.deliveryLocation" placeholder="请输入发货存储地点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import type { DeliverySignVO, DeliverySignDTO, DeliverySignQuery } from '@/types/material-mgmt'
import { deliverySignApi } from '@/api/material-mgmt/deliverySign'

const loading = ref(false)
const list = ref<DeliverySignVO[]>([])
const total = ref(0)
const query = reactive<DeliverySignQuery>({
  workOrderNo: '',
  materialCode: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive<DeliverySignDTO>({
  lineNo: '',
  workOrderId: undefined,
  workOrderNo: '',
  materialId: undefined,
  materialCode: '',
  materialName: '',
  planDeliveryQty: undefined,
  pendingSignQty: undefined,
  unit: '',
  deliveryWarehouse: '',
  deliveryLocation: '',
})

const rules: FormRules = {
  workOrderNo: [{ required: true, message: '请输入工单号', trigger: 'blur' }],
  materialCode: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  planDeliveryQty: [{ required: true, message: '请输入计划发货数量', trigger: 'blur' }],
  pendingSignQty: [{ required: true, message: '请输入待签收数量', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await deliverySignApi.page(query)
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
  query.workOrderNo = ''
  query.materialCode = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  Object.assign(form, {
    lineNo: '',
    workOrderId: undefined,
    workOrderNo: '',
    materialId: undefined,
    materialCode: '',
    materialName: '',
    planDeliveryQty: undefined,
    pendingSignQty: undefined,
    unit: '',
    deliveryWarehouse: '',
    deliveryLocation: '',
  })
  dialogVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    await deliverySignApi.create(form)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleConfirmSign(row: DeliverySignVO) {
  ElMessageBox.confirm('确定要确认签收该发货单吗？', '提示', {
    type: 'info',
  }).then(async () => {
    await deliverySignApi.confirm(row.id)
    ElMessage.success('签收成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.delivery-sign-list {
  padding: 16px;
}
.search-card {
  margin-bottom: 16px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
