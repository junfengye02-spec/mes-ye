<template>
  <div class="production-plan-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="计划号">
          <el-input v-model="query.planNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="产品编码">
          <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 160px" />
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
        <el-table-column prop="planNo" label="计划号" min-width="120" />
        <el-table-column prop="orderNo" label="订单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="100" />
        <el-table-column prop="productName" label="产品名称" min-width="120" />
        <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
        <el-table-column prop="planStartDate" label="计划开始日期" width="120" />
        <el-table-column prop="planEndDate" label="计划结束日期" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('productionPlanStatus', row.status || '') as any">
              {{ getDictLabel('productionPlanStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="计划号" prop="planNo">
          <el-input v-model="form.planNo" placeholder="请输入计划号" />
        </el-form-item>
        <el-form-item label="订单计划ID" prop="orderPlanId">
          <el-input-number v-model="form.orderPlanId" :min="0" placeholder="订单计划ID" style="width: 100%" />
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
        <el-form-item label="计划日期" prop="planDateRange">
          <el-date-picker
            v-model="form.planDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
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
import type { ProductionPlanVO, ProductionPlanDTO, ProductionPlanQuery } from '@/types/plan'
import { productionPlanApi } from '@/api/plan/productionPlan'

const loading = ref(false)
const list = ref<ProductionPlanVO[]>([])
const total = ref(0)
const query = reactive<ProductionPlanQuery>({
  planNo: '',
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
const form = reactive<ProductionPlanDTO & { planDateRange?: [string, string] }>({
  planNo: '',
  orderPlanId: undefined,
  productCode: '',
  productName: '',
  planQty: 1,
  qtyUnit: '',
  planStartDate: '',
  planEndDate: '',
  planDateRange: undefined,
  remark: '',
})

const rules: FormRules = {
  planNo: [{ required: true, message: '请输入计划号', trigger: 'blur' }],
  planQty: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
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

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.planNo = ''
  query.orderNo = ''
  query.productCode = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增生产计划'
  editId.value = null
  Object.assign(form, {
    planNo: '',
    orderPlanId: undefined,
    productCode: '',
    productName: '',
    planQty: 1,
    qtyUnit: '',
    planStartDate: '',
    planEndDate: '',
    planDateRange: undefined,
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: ProductionPlanVO) {
  dialogTitle.value = '编辑生产计划'
  editId.value = row.id
  const range: [string, string] | undefined =
    row.planStartDate && row.planEndDate ? [row.planStartDate, row.planEndDate] : undefined
  Object.assign(form, {
    planNo: row.planNo,
    orderPlanId: row.orderPlanId,
    productCode: row.productCode,
    productName: row.productName,
    planQty: row.planQty,
    qtyUnit: row.qtyUnit,
    planStartDate: row.planStartDate,
    planEndDate: row.planEndDate,
    planDateRange: range,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  await formRef.value?.validate()
  const { planDateRange, ...rest } = form
  const submitData: ProductionPlanDTO = {
    ...rest,
    planStartDate: planDateRange?.[0],
    planEndDate: planDateRange?.[1],
  }
  submitLoading.value = true
  try {
    if (editId.value) {
      await productionPlanApi.update(editId.value, submitData)
      ElMessage.success('修改成功')
    } else {
      await productionPlanApi.create(submitData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ProductionPlanVO) {
  ElMessageBox.confirm('确定要删除该生产计划吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await productionPlanApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

function handleRelease(row: ProductionPlanVO) {
  ElMessageBox.confirm('确定要下达该生产计划吗？', '提示', {
    type: 'info',
  }).then(async () => {
    await productionPlanApi.release(row.id)
    ElMessage.success('下达成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.production-plan-list {
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
