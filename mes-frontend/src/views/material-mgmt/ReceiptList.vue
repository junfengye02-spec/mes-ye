<template>
  <div class="receipt-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="入库单号">
          <el-input v-model="query.receiptNo" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="入库类型">
          <el-select v-model="query.receiptType" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('receiptType')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('receiptStatus')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> {{ t('buttons.search') }}
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> {{ t('buttons.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span>{{ t('material.receiptTitle') }}</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> {{ t('buttons.add') }}
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="receiptNo" label="入库单号" min-width="140" />
        <el-table-column prop="receiptType" label="入库类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('receiptType', row.receiptType || '') as any">
              {{ getDictLabel('receiptType', row.receiptType || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouse" label="仓库" min-width="120" />
        <el-table-column prop="movementType" label="移动类型" width="110" />
        <el-table-column prop="planReceiptTime" label="计划收货时间" width="170" />
        <el-table-column prop="actualReceiptTime" label="实际收货时间" width="170" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDictType('receiptStatus', row.status || '') as any">
              {{ getDictLabel('receiptStatus', row.status || '') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看明细</el-button>
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

    <!-- 新增/编辑 入库单：主表 + 明细 items -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="880px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="入库单号" prop="receiptNo">
              <el-input v-model="form.receiptNo" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库类型" prop="receiptType">
              <el-select v-model="form.receiptType" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in getDictList('receiptType')"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouse">
              <el-input v-model="form.warehouse" placeholder="请输入仓库" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="移动类型" prop="movementType">
              <el-input v-model="form.movementType" placeholder="请输入移动类型（如 101-成品入库）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划收货时间" prop="planReceiptTime">
              <el-date-picker
                v-model="form.planReceiptTime"
                type="datetime"
                placeholder="选择计划收货时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">入库明细</el-divider>

        <el-table :data="form.items" border stripe size="small" empty-text="请点击下方按钮添加明细">
          <el-table-column label="工单号" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.workOrderNo" placeholder="工单号" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="物料编码" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.materialCode" placeholder="物料编码" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="物料名称" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.materialName" placeholder="物料名称" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="收货数量" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.receiptQty" :min="0.0001" :precision="4" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="90">
            <template #default="{ row }">
              <el-input v-model="row.unit" placeholder="单位" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="存储地点" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.storageLocation" placeholder="存储地点" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link size="small" @click="removeItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 8px">
          <el-button type="primary" link @click="addItem">
            <el-icon><Plus /></el-icon> 添加明细
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看明细 -->
    <el-dialog v-model="viewVisible" title="入库明细" width="880px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ viewData.receiptNo }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">
          {{ getDictLabel('receiptType', viewData.receiptType || '') }}
        </el-descriptions-item>
        <el-descriptions-item label="仓库">{{ viewData.warehouse }}</el-descriptions-item>
        <el-descriptions-item label="移动类型">{{ viewData.movementType }}</el-descriptions-item>
        <el-descriptions-item label="计划收货时间">{{ viewData.planReceiptTime }}</el-descriptions-item>
        <el-descriptions-item label="实际收货时间">{{ viewData.actualReceiptTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getDictLabel('receiptStatus', viewData.status || '') }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createdTime }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">明细（{{ viewData.items?.length || 0 }} 条）</el-divider>
      <el-table :data="viewData.items || []" border stripe size="small">
        <el-table-column prop="itemCode" label="编码" width="120" />
        <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="receiptQty" label="收货数量" width="110" align="right" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="storageLocation" label="存储地点" min-width="120" />
        <el-table-column prop="varianceQty" label="差异数量" width="110" align="right" />
        <el-table-column prop="varianceReason" label="差异原因" min-width="140" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { ReceiptVO, ReceiptDTO, ReceiptQuery, ReceiptItemDTO } from '@/types/material-mgmt'
import { receiptApi } from '@/api/material-mgmt/receipt'

const { t } = useI18n()
const loading = ref(false)
const list = ref<ReceiptVO[]>([])
const total = ref(0)
const query = reactive<ReceiptQuery>({
  receiptNo: '',
  receiptType: '',
  status: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增入库')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)

// 表单模型：对齐后端 FinishedGoodsReceiptDTO
const form = reactive<ReceiptDTO>({
  receiptNo: '',
  receiptType: '',
  warehouse: '',
  movementType: '',
  planReceiptTime: '',
  items: [],
})

const rules: FormRules = {
  receiptType: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
  warehouse: [{ required: true, message: '请输入仓库', trigger: 'blur' }],
}

const viewVisible = ref(false)
const viewData = reactive<ReceiptVO>({ id: 0 } as ReceiptVO)

async function loadList() {
  loading.value = true
  try {
    const res = await receiptApi.page(query)
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
  query.receiptNo = ''
  query.receiptType = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function resetForm() {
  Object.assign(form, {
    receiptNo: '',
    receiptType: '',
    warehouse: '',
    movementType: '',
    planReceiptTime: '',
    items: [] as ReceiptItemDTO[],
  })
}

function handleAdd() {
  dialogTitle.value = '新增入库'
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

async function handleEdit(row: ReceiptVO) {
  dialogTitle.value = '编辑入库'
  editId.value = row.id
  const detail = await receiptApi.getDetail(row.id)
  Object.assign(form, {
    receiptNo: detail?.receiptNo ?? '',
    receiptType: detail?.receiptType ?? '',
    warehouse: detail?.warehouse ?? '',
    movementType: detail?.movementType ?? '',
    planReceiptTime: detail?.planReceiptTime ?? '',
    items: (detail?.items ?? []).map((it) => ({
      itemCode: it.itemCode,
      workOrderId: it.workOrderId,
      workOrderNo: it.workOrderNo,
      materialCode: it.materialCode,
      materialName: it.materialName,
      receiptQty: it.receiptQty,
      unit: it.unit,
      storageLocation: it.storageLocation,
      varianceQty: it.varianceQty,
      varianceReason: it.varianceReason,
    })) as ReceiptItemDTO[],
  })
  dialogVisible.value = true
}

async function handleView(row: ReceiptVO) {
  const detail = await receiptApi.getDetail(row.id)
  Object.assign(viewData, detail)
  viewVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

function addItem() {
  if (!form.items) form.items = []
  form.items.push({
    workOrderNo: '',
    materialCode: '',
    materialName: '',
    receiptQty: undefined,
    unit: '',
    storageLocation: '',
  })
}

function removeItem(index: number) {
  form.items?.splice(index, 1)
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editId.value) {
      await receiptApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await receiptApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ReceiptVO) {
  ElMessageBox.confirm('确定要删除该入库单吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await receiptApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.receipt-list {
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
