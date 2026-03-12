<template>
  <div class="delivery-sign-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="发货单号">
          <el-input v-model="query.deliveryNo" placeholder="请输入" clearable style="width: 160px" />
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
              v-for="item in getDictList('deliverySignStatus')"
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
          <span>发货签收列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="deliveryNo" label="发货单号" min-width="140" />
        <el-table-column prop="orderNo" label="订单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="deliveryQty" label="发货数量" width="100" align="right" />
        <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
        <el-table-column prop="deliveryDate" label="发货日期" width="120" />
        <el-table-column prop="signDate" label="签收日期" width="120" />
        <el-table-column prop="signPerson" label="签收人" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('deliverySignStatus', row.status || '') as any">
              {{ getDictLabel('deliverySignStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING' || !row.status"
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="发货数量" prop="deliveryQty">
          <el-input-number v-model="form.deliveryQty" :min="0.0001" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="qtyUnit">
          <el-input v-model="form.qtyUnit" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="发货日期" prop="deliveryDate">
          <el-date-picker
            v-model="form.deliveryDate"
            type="date"
            placeholder="选择日期"
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
import type { DeliverySignVO, DeliverySignDTO, DeliverySignQuery } from '@/types/material-mgmt'
import { deliverySignApi } from '@/api/material-mgmt/deliverySign'

const loading = ref(false)
const list = ref<DeliverySignVO[]>([])
const total = ref(0)
const query = reactive<DeliverySignQuery>({
  deliveryNo: '',
  orderNo: '',
  productCode: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive<DeliverySignDTO>({
  orderNo: '',
  productCode: '',
  productName: '',
  deliveryQty: undefined,
  qtyUnit: '',
  deliveryDate: '',
  remark: '',
})

const rules: FormRules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  deliveryQty: [{ required: true, message: '请输入发货数量', trigger: 'blur' }],
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
  query.deliveryNo = ''
  query.orderNo = ''
  query.productCode = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  Object.assign(form, {
    orderNo: '',
    productCode: '',
    productName: '',
    deliveryQty: undefined,
    qtyUnit: '',
    deliveryDate: '',
    remark: '',
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
