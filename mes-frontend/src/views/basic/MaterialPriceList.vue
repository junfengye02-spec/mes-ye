<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="物料编码">
        <el-input v-model="query.materialCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="物料名称">
        <el-input v-model="query.materialName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="价格类型">
        <el-input v-model="query.priceType" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="query.supplier" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
    </SearchForm>

    <DataTable
      :data="(list as any)"
      :loading="loading"
      :total="total"
      :page-num="Number(query.pageNum) || 1"
      :page-size="Number(query.pageSize) || 20"
      @page-change="handlePageChange"
    >
      <template #title>物料价格</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="materialCode" label="物料编码" min-width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="priceType" label="价格类型" min-width="100" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="currency" label="币种" width="80" />
      <el-table-column prop="effectiveDate" label="生效日期" width="110" />
      <el-table-column prop="expirationDate" label="失效日期" width="110" />
      <el-table-column prop="supplier" label="供应商" min-width="120" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑物料价格' : '新增物料价格'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="物料" prop="materialId">
          <el-select
            v-model="form.materialId"
            placeholder="请选择物料"
            filterable
            style="width: 100%"
            :disabled="isEdit"
          >
            <el-option
              v-for="m in materialOptions"
              :key="m.id"
              :label="`${m.materialCode} - ${m.materialName}`"
              :value="Number(m.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格类型" prop="priceType">
          <el-input v-model="form.priceType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number
            :model-value="form.price ?? undefined"
            @update:model-value="(v: number | undefined) => { form.price = v }"
            :precision="4"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="币种" prop="currency">
          <el-input v-model="form.currency" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效日期" prop="expirationDate">
          <el-date-picker v-model="form.expirationDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplier">
          <el-input v-model="form.supplier" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" rows="3" placeholder="请输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - Element Plus type inference for form/table
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import { materialPriceApi } from '@/api/basic/materialPrice'
import { materialApi } from '@/api/basic/material'
import type { MaterialPriceVO, MaterialPriceDTO, MaterialPriceQuery } from '@/types/basic'
import type { MaterialVO } from '@/types/basic'

const query = reactive<MaterialPriceQuery>({
  materialCode: undefined,
  materialName: undefined,
  priceType: undefined,
  supplier: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<MaterialPriceVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)
const materialOptions = ref<MaterialVO[]>([])

const form = reactive({
  materialId: undefined as number | undefined,
  priceType: '',
  price: undefined as number | undefined,
  currency: '',
  effectiveDate: '',
  expirationDate: '',
  supplier: '',
  remark: '',
} as MaterialPriceDTO & { materialId?: number })

const formRules = {
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
}

async function loadMaterials() {
  const res = await materialApi.page({ pageNum: 1, pageSize: 500 })
  materialOptions.value = res.list || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await materialPriceApi.page(query)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

watch(dialogVisible, val => {
  if (val) loadMaterials()
})

function handlePageChange({ pageNum, pageSize }: { pageNum: number; pageSize: number }) {
  query.pageNum = pageNum
  query.pageSize = pageSize
  loadData()
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: MaterialPriceVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    materialId: row.materialId,
    priceType: row.priceType,
    price: row.price,
    currency: row.currency,
    effectiveDate: row.effectiveDate,
    expirationDate: row.expirationDate,
    supplier: row.supplier,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    materialId: undefined,
    priceType: '',
    price: undefined,
    currency: '',
    effectiveDate: '',
    expirationDate: '',
    supplier: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto: MaterialPriceDTO = {
      materialId: form.materialId!,
      priceType: form.priceType,
      price: form.price,
      currency: form.currency,
      effectiveDate: form.effectiveDate,
      expirationDate: form.expirationDate,
      supplier: form.supplier,
      remark: form.remark,
    }
    if (isEdit.value && editId.value) {
      await materialPriceApi.update(editId.value, dto)
      ElMessage.success('修改成功')
    } else {
      await materialPriceApi.create(dto)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: MaterialPriceVO) {
  await ElMessageBox.confirm('确定要删除该物料价格吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await materialPriceApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
