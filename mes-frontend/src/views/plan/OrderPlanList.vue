<template>
  <div class="order-plan-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="query.customerName" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 160px" />
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
        <el-table-column prop="customerName" label="客户名称" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="100" />
        <el-table-column prop="productName" label="产品名称" min-width="120" />
        <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
        <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
        <el-table-column prop="deliveryDate" label="交货日期" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('orderPlanStatus', row.status || '') as any">
              {{ getDictLabel('orderPlanStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" align="center" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button
              v-if="row.status === 'CREATED'"
              type="primary"
              link
              size="small"
              @click="handleRelease(row)"
            >
              下达
            </el-button>
            <el-button
              v-if="row.status === 'RELEASED'"
              type="success"
              link
              size="small"
              @click="handleComplete(row)"
            >
              完成
            </el-button>
            <el-button
              v-if="row.status === 'RELEASED'"
              type="danger"
              link
              size="small"
              @click="handleTerminate(row)"
            >
              终止
            </el-button>
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
      width="600px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="计划数量" prop="planQty">
          <el-input-number v-model="form.planQty" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="qtyUnit">
          <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="交货日期" prop="deliveryDate">
          <el-date-picker
            v-model="form.deliveryDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
  customerName: '',
  productCode: '',
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
  customerName: '',
  productCode: '',
  productName: '',
  planQty: 1,
  qtyUnit: '',
  deliveryDate: '',
  priority: 0,
  remark: '',
})

const rules: FormRules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  planQty: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
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
  query.customerName = ''
  query.productCode = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增订单计划'
  editId.value = null
  Object.assign(form, {
    orderNo: '',
    customerName: '',
    productCode: '',
    productName: '',
    planQty: 1,
    qtyUnit: '',
    deliveryDate: '',
    priority: 0,
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: OrderPlanVO) {
  dialogTitle.value = '编辑订单计划'
  editId.value = row.id
  Object.assign(form, {
    orderNo: row.orderNo,
    customerName: row.customerName,
    productCode: row.productCode,
    productName: row.productName,
    planQty: row.planQty,
    qtyUnit: row.qtyUnit,
    deliveryDate: row.deliveryDate,
    priority: row.priority ?? 0,
    remark: row.remark,
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
    if (editId.value) {
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
  ElMessageBox.confirm('确定要删除该订单计划吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await orderPlanApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

function handleRelease(row: OrderPlanVO) {
  ElMessageBox.confirm('确定要下达该订单计划吗？', '提示', {
    type: 'info',
  }).then(async () => {
    await orderPlanApi.release(row.id)
    ElMessage.success('下达成功')
    loadList()
  }).catch(() => {})
}

function handleComplete(row: OrderPlanVO) {
  ElMessageBox.confirm('确定要完成该订单计划吗？', '提示', {
    type: 'success',
  }).then(async () => {
    await orderPlanApi.complete(row.id)
    ElMessage.success('完成成功')
    loadList()
  }).catch(() => {})
}

function handleTerminate(row: OrderPlanVO) {
  ElMessageBox.confirm('确定要终止该订单计划吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await orderPlanApi.terminate(row.id)
    ElMessage.success('终止成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.order-plan-list {
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
