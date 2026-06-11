<template>
  <div class="return-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="退料单号">
          <el-input v-model="query.returnNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="query.workOrderNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('materialReturnStatus')"
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
          <span>生产退料列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="returnNo" label="退料单号" min-width="140" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="orderNo" label="订单号" min-width="120" />
        <el-table-column prop="productCode" label="产品编码" min-width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="projectName" label="项目" min-width="120" />
        <el-table-column prop="businessType" label="业务类型" width="110" />
        <el-table-column prop="flowCode" label="流程编码" width="110" />
        <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
        <el-table-column prop="completedQty" label="完工数量" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('materialReturnStatus', row.status || '') as any">
              {{ getDictLabel('materialReturnStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
      :title="dialogTitle"
      width="520px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="工单ID" prop="workOrderId">
          <el-input-number v-model="form.workOrderId" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工单号" prop="workOrderNo">
          <el-input v-model="form.workOrderNo" placeholder="请输入工单号" />
        </el-form-item>
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
          <el-input v-model="form.projectName" placeholder="请输入项目" />
        </el-form-item>
        <el-form-item label="WBS元素" prop="wbsElement">
          <el-input v-model="form.wbsElement" placeholder="请输入WBS元素" />
        </el-form-item>
        <el-form-item label="新制维修类型" prop="newOrRepairType">
          <el-input v-model="form.newOrRepairType" placeholder="请输入新制维修类型" />
        </el-form-item>
        <el-form-item label="业务类型" prop="businessType">
          <el-input v-model="form.businessType" placeholder="请输入业务类型" />
        </el-form-item>
        <el-form-item label="流程编码" prop="flowCode">
          <el-input v-model="form.flowCode" placeholder="请输入流程编码" />
        </el-form-item>
        <el-form-item label="计划数量" prop="planQty">
          <el-input-number v-model="form.planQty" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="完工数量" prop="completedQty">
          <el-input-number v-model="form.completedQty" :min="0" :precision="4" style="width: 100%" />
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
import type { MaterialReturnVO, MaterialReturnDTO, MaterialReturnQuery } from '@/types/material-mgmt'
import { materialReturnApi } from '@/api/material-mgmt/materialReturn'

const loading = ref(false)
const list = ref<MaterialReturnVO[]>([])
const total = ref(0)
const query = reactive<MaterialReturnQuery>({
  returnNo: '',
  workOrderNo: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增退料')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const form = reactive<MaterialReturnDTO>({
  workOrderId: undefined,
  workOrderNo: '',
  orderNo: '',
  productCode: '',
  productName: '',
  projectName: '',
  wbsElement: '',
  newOrRepairType: '',
  businessType: '',
  flowCode: '',
  planQty: undefined,
  completedQty: undefined,
})

const rules: FormRules = {
  workOrderId: [{ required: true, message: '请输入工单ID', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await materialReturnApi.page(query)
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
  query.returnNo = ''
  query.workOrderNo = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增退料'
  editId.value = null
  Object.assign(form, {
    workOrderId: undefined,
    workOrderNo: '',
    orderNo: '',
    productCode: '',
    productName: '',
    projectName: '',
    wbsElement: '',
    newOrRepairType: '',
    businessType: '',
    flowCode: '',
    planQty: undefined,
    completedQty: undefined,
  })
  dialogVisible.value = true
}

function handleEdit(row: MaterialReturnVO) {
  dialogTitle.value = '编辑退料'
  editId.value = row.id
  Object.assign(form, {
    workOrderId: row.workOrderId,
    workOrderNo: row.workOrderNo ?? '',
    orderNo: row.orderNo ?? '',
    productCode: row.productCode ?? '',
    productName: row.productName ?? '',
    projectName: row.projectName ?? '',
    newOrRepairType: row.newOrRepairType ?? '',
    businessType: row.businessType ?? '',
    flowCode: row.flowCode ?? '',
    planQty: row.planQty,
    completedQty: row.completedQty,
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
      await materialReturnApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await materialReturnApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: MaterialReturnVO) {
  ElMessageBox.confirm('确定要删除该退料单吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await materialReturnApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.return-list {
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
