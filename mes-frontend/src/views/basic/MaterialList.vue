<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="物料编码">
        <el-input v-model="query.materialCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="物料名称">
        <el-input v-model="query.materialName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="物料类型">
        <el-input v-model="query.materialType" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="工厂">
        <el-input v-model="query.factory" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
    </SearchForm>

    <DataTable
      :data="list"
      :loading="loading"
      :total="total"
      :page-num="query.pageNum"
      :page-size="query.pageSize"
      @page-change="handlePageChange"
    >
      <template #title>物料档案</template>
      <template #toolbar>
        <el-button v-auth="['basic:material:create']" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="materialCode" label="物料编码" min-width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="materialType" label="物料类型" min-width="100" />
      <el-table-column prop="categoryLevel1" label="一级分类" min-width="100" />
      <el-table-column prop="baseUnit" label="基本单位" width="90" />
      <el-table-column prop="factory" label="工厂" width="100" />
      <el-table-column prop="traceMode" label="追溯模式" width="100" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button v-auth="['basic:material:detail']" link type="info" @click.stop="handleView(row)">查看</el-button>
          <el-button v-auth="['basic:material:update']" link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button v-auth="['basic:material:delete']" link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑物料' : '新增物料'" width="600px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="物料编码" prop="materialCode">
          <el-input v-model="form.materialCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="物料名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="物料类型" prop="materialType">
          <el-input v-model="form.materialType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="一级分类" prop="categoryLevel1">
          <el-input v-model="form.categoryLevel1" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="二级分类" prop="categoryLevel2">
          <el-input v-model="form.categoryLevel2" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="G代码" prop="gCode">
          <el-input v-model="form.gCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品类型" prop="productType">
          <el-input v-model="form.productType" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="工厂" prop="factory">
          <el-input v-model="form.factory" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="基本单位" prop="baseUnit">
          <el-input v-model="form.baseUnit" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="追溯模式" prop="traceMode">
          <el-input v-model="form.traceMode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="图号" prop="drawingNo">
          <el-input v-model="form.drawingNo" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="物料品牌" prop="materialBrand">
          <el-input v-model="form.materialBrand" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="需检验" prop="needInspection">
          <el-switch v-model="form.needInspection" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          v-auth="['basic:material:create', 'basic:material:update']"
          type="primary"
          :loading="saving"
          @click="handleSave"
        >确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import { materialApi } from '@/api/basic/material'
import type { MaterialVO, MaterialDTO, MaterialQuery } from '@/types/basic'

const query = reactive<MaterialQuery>({
  materialCode: undefined,
  materialName: undefined,
  materialType: undefined,
  factory: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<MaterialVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<MaterialDTO>({
  materialCode: '',
  materialName: '',
  materialType: '',
  categoryLevel1: '',
  categoryLevel2: '',
  gCode: '',
  productType: '',
  factory: '',
  baseUnit: '',
  traceMode: '',
  drawingNo: '',
  materialBrand: '',
  needInspection: 0,
})

const formRules = {
  materialCode: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await materialApi.page(query)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

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

function handleEdit(row: MaterialVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    materialCode: row.materialCode,
    materialName: row.materialName,
    materialType: row.materialType,
    categoryLevel1: row.categoryLevel1,
    categoryLevel2: row.categoryLevel2,
    gCode: row.gCode,
    productType: row.productType,
    factory: row.factory,
    baseUnit: row.baseUnit,
    traceMode: row.traceMode,
    drawingNo: row.drawingNo,
    materialBrand: row.materialBrand,
    needInspection: row.needInspection ?? 0,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    materialCode: '',
    materialName: '',
    materialType: '',
    categoryLevel1: '',
    categoryLevel2: '',
    gCode: '',
    productType: '',
    factory: '',
    baseUnit: '',
    traceMode: '',
    drawingNo: '',
    materialBrand: '',
    needInspection: 0,
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await materialApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await materialApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

function handleView(row: MaterialVO) {
  const lines = [
    `物料编码：${row.materialCode ?? '-'}`,
    `物料名称：${row.materialName ?? '-'}`,
    `物料类型：${row.materialType ?? '-'}`,
    `一级分类：${row.categoryLevel1 ?? '-'}`,
    `基本单位：${row.baseUnit ?? '-'}`,
    `工厂：${row.factory ?? '-'}`,
    `追溯模式：${row.traceMode ?? '-'}`,
  ].join('\n')
  ElMessageBox.alert(lines, '物料详情', { confirmButtonText: '关闭' }).catch(() => {})
}

async function handleDelete(row: MaterialVO) {
  await ElMessageBox.confirm('确定要删除该物料吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await materialApi.delete(row.id)
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
