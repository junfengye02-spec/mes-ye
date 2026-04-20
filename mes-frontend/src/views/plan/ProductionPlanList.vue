<template>
  <div class="production-plan-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('productionPlanStatus')"
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
          <span>生产计划列表</span>
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
        <el-table-column prop="workType" label="类型" width="80" />
        <el-table-column prop="planQty" label="计划数量" width="90" align="right" />
        <el-table-column prop="planStartTime" label="计划开始" width="160" />
        <el-table-column prop="planEndTime" label="计划结束" width="160" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('productionPlanStatus', row.status || '') as any">
              {{ getDictLabel('productionPlanStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'CREATED'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            <el-button v-if="row.status === 'CREATED'" type="primary" link size="small" @click="handleRelease(row)">下达</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="订单计划ID" prop="orderPlanId">
          <el-input-number v-model="form.orderPlanId" :min="1" placeholder="关联的订单计划" style="width: 100%" />
        </el-form-item>
        <el-form-item label="订单编号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单编号" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="计划数量" prop="planQty">
          <el-input-number v-model="form.planQty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="qtyUnit">
          <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker
            v-model="planDateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
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
import type { ProductionPlanVO, ProductionPlanDTO, ProductionPlanQuery } from '@/types/plan'
import { productionPlanApi } from '@/api/plan/productionPlan'

const loading = ref(false)
const list = ref<ProductionPlanVO[]>([])
const total = ref(0)
const query = reactive<ProductionPlanQuery>({
  orderNo: '',
  productCode: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增生产计划')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const planDateRange = ref<[string, string] | undefined>()
const form = reactive<ProductionPlanDTO>({
  orderPlanId: 0,
  orderNo: '',
  productCode: '',
  productName: '',
  planQty: 0,
  qtyUnit: '',
  planStartTime: '',
  planEndTime: '',
})

const rules: FormRules = {
  orderPlanId: [{ required: true, message: '请输入订单计划ID', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await productionPlanApi.page(query)
    list.value = res?.list ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.pageNum = 1; loadList() }

function handleReset() {
  query.orderNo = ''
  query.productCode = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增生产计划'
  editId.value = null
  Object.assign(form, { orderPlanId: 0, orderNo: '', productCode: '', productName: '', planQty: 0, qtyUnit: '', planStartTime: '', planEndTime: '' })
  planDateRange.value = undefined
  dialogVisible.value = true
}

function handleEdit(row: ProductionPlanVO) {
  dialogTitle.value = '编辑生产计划'
  editId.value = row.id
  Object.assign(form, {
    orderPlanId: row.orderPlanId ?? 0,
    orderNo: row.orderNo ?? '',
    productCode: row.productCode ?? '',
    productName: row.productName ?? '',
    planQty: row.planQty ?? 0,
    qtyUnit: row.qtyUnit ?? '',
    planStartTime: row.planStartTime ?? '',
    planEndTime: row.planEndTime ?? '',
  })
  planDateRange.value = row.planStartTime && row.planEndTime ? [row.planStartTime, row.planEndTime] : undefined
  dialogVisible.value = true
}

function handleDialogClose() { formRef.value?.resetFields() }

async function handleSubmit() {
  if (planDateRange.value && planDateRange.value.length === 2) {
    form.planStartTime = planDateRange.value[0]
    form.planEndTime = planDateRange.value[1]
  } else {
    form.planStartTime = ''
    form.planEndTime = ''
  }
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (editId.value !== null) {
      await productionPlanApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await productionPlanApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ProductionPlanVO) {
  ElMessageBox.confirm('确定要删除该生产计划吗？', '提示', { type: 'warning' }).then(async () => {
    await productionPlanApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

function handleRelease(row: ProductionPlanVO) {
  ElMessageBox.confirm('确定要下达该生产计划吗？', '提示', { type: 'info' }).then(async () => {
    await productionPlanApi.release(row.id)
    ElMessage.success('下达成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => { loadList() })
</script>

<style scoped>
.production-plan-list { padding: 16px; }
.search-card { margin-bottom: 16px; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
