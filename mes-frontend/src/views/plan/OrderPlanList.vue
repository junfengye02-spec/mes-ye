<template>
  <div class="order-plan-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="query.productName" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('orderPlanStatus')"
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
          <span>订单计划列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="orderNo" label="订单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="100" />
        <el-table-column prop="productName" label="产品名称" min-width="120" />
        <el-table-column prop="projectName" label="项目" min-width="100" show-overflow-tooltip />
        <el-table-column prop="workType" label="类型" width="80" />
        <el-table-column prop="machineModel" label="机型" width="80" />
        <el-table-column prop="planQty" label="计划数量" width="90" align="right" />
        <el-table-column prop="qtyUnit" label="单位" width="60" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('orderPlanStatus', row.status || '') as any">
              {{ getDictLabel('orderPlanStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataSource" label="来源" width="70" align="center" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'CREATED'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            <el-button v-if="row.status === 'CREATED'" type="primary" link size="small" @click="handleRelease(row)">下达</el-button>
            <el-button v-if="row.status === 'RELEASED'" type="success" link size="small" @click="handleComplete(row)">完成</el-button>
            <el-button v-if="row.status === 'RELEASED'" type="danger" link size="small" @click="handleTerminate(row)">终止</el-button>
          </template>
        </el-table-column>
      </el-table>
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
      :title="dialogTitle"
      width="650px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="项目" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="类型" prop="workType">
          <el-input v-model="form.workType" placeholder="维修/检查/主机" />
        </el-form-item>
        <el-form-item label="机型" prop="machineModel">
          <el-input v-model="form.machineModel" placeholder="请输入机型" />
        </el-form-item>
        <el-form-item label="计划数量" prop="planQty">
          <el-input-number v-model="form.planQty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="qtyUnit">
          <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="计划开始时间" prop="planStartTime">
          <el-date-picker v-model="form.planStartTime" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束时间" prop="planEndTime">
          <el-date-picker v-model="form.planEndTime" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
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
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { OrderPlanVO, OrderPlanDTO, OrderPlanQuery } from '@/types/plan'
import { orderPlanApi } from '@/api/plan/orderPlan'

const loading = ref(false)
const list = ref<OrderPlanVO[]>([])
const total = ref(0)
const query = reactive<OrderPlanQuery>({
  orderNo: '',
  productCode: '',
  productName: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增订单计划')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const form = reactive<OrderPlanDTO>({
  orderNo: '',
  productCode: '',
  productName: '',
  projectName: '',
  workType: '',
  machineModel: '',
  planQty: 0,
  qtyUnit: '',
  planStartTime: '',
  planEndTime: '',
})

const rules: FormRules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await orderPlanApi.page(query)
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
  query.orderNo = ''
  query.productCode = ''
  query.productName = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增订单计划'
  editId.value = null
  Object.assign(form, { orderNo: '', productCode: '', productName: '', projectName: '', workType: '', machineModel: '', planQty: 0, qtyUnit: '', planStartTime: '', planEndTime: '' })
  dialogVisible.value = true
}

function handleEdit(row: OrderPlanVO) {
  dialogTitle.value = '编辑订单计划'
  editId.value = row.id
  Object.assign(form, {
    orderNo: row.orderNo ?? '',
    productCode: row.productCode ?? '',
    productName: row.productName ?? '',
    projectName: row.projectName ?? '',
    workType: row.workType ?? '',
    machineModel: row.machineModel ?? '',
    planQty: row.planQty ?? 0,
    qtyUnit: row.qtyUnit ?? '',
    planStartTime: row.planStartTime ?? '',
    planEndTime: row.planEndTime ?? '',
  })
  dialogVisible.value = true
}

function handleDialogClose() { formRef.value?.resetFields() }

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (editId.value !== null) {
      await orderPlanApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await orderPlanApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: OrderPlanVO) {
  ElMessageBox.confirm('确定要删除该订单计划吗？', '提示', { type: 'warning' }).then(async () => {
    await orderPlanApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

function handleRelease(row: OrderPlanVO) {
  ElMessageBox.confirm('确定要下达该订单计划吗？', '提示', { type: 'info' }).then(async () => {
    await orderPlanApi.release(row.id)
    ElMessage.success('下达成功')
    loadList()
  }).catch(() => {})
}

function handleComplete(row: OrderPlanVO) {
  ElMessageBox.confirm('确定要完成该订单计划吗？', '提示', { type: 'success' }).then(async () => {
    await orderPlanApi.complete(row.id)
    ElMessage.success('完成成功')
    loadList()
  }).catch(() => {})
}

function handleTerminate(row: OrderPlanVO) {
  ElMessageBox.prompt('请输入终止原因', '终止订单计划', {
    confirmButtonText: '确认终止',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '终止原因不能为空',
    type: 'warning',
  }).then(async ({ value: reason }) => {
    await orderPlanApi.terminate(row.id, reason)
    ElMessage.success('终止成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => { loadList() })
</script>

<style scoped>
.order-plan-list { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
