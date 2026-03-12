<template>
  <div class="page-container">
    <SearchForm v-model="query" @search="loadData">
      <el-form-item label="BOM编码">
        <el-input v-model="query.bomCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="BOM名称">
        <el-input v-model="query.bomName" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="产品编码">
        <el-input v-model="query.productCode" placeholder="请输入" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option
            v-for="item in getDictList('bomStatus')"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </SearchForm>

    <DataTable
      :data="(list as any)"
      :loading="loading"
      :total="total"
      :page-num="Number(query.pageNum) || 1"
      :page-size="Number(query.pageSize) || 20"
      @page-change="handlePageChange"
      @row-click="handleRowClick"
    >
      <template #title>制造BOM</template>
      <template #toolbar>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增
        </el-button>
      </template>
      <el-table-column prop="bomCode" label="BOM编码" min-width="120" />
      <el-table-column prop="bomName" label="BOM名称" min-width="140" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="productCode" label="产品编码" min-width="120" />
      <el-table-column prop="productName" label="产品名称" min-width="140" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(getDictType('bomStatus', row.status) || undefined) as any">
            {{ getDictLabel('bomStatus', row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" @click.stop="handleUpgrade(row)">升级版本</el-button>
          <el-button link type="primary" @click.stop="handlePublish(row)">发布</el-button>
          <el-button link type="warning" @click.stop="handleDisable(row)">停用</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑BOM' : '新增BOM'" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" :label-width="100">
        <el-form-item label="BOM编码" prop="bomCode">
          <el-input v-model="form.bomCode" placeholder="请输入" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="BOM名称" prop="bomName">
          <el-input v-model="form.bomName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="form.version" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入" />
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

    <el-dialog v-model="itemsDialogVisible" title="BOM明细" width="700px">
      <el-table
        :data="bomItems"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
        stripe
        v-loading="itemsLoading"
      >
        <template #empty>
          <el-empty description="暂无明细" />
        </template>
        <el-table-column prop="materialCode" label="物料编码" min-width="120" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unit" label="单位" width="80" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck - Element Plus type inference for form/table
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchForm from '@/components/SearchForm/index.vue'
import DataTable from '@/components/DataTable/index.vue'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import { manufacturingBomApi } from '@/api/process/manufacturingBom'
import type { ManufacturingBomVO, ManufacturingBomDTO, ManufacturingBomQuery, ManufacturingBomItemVO } from '@/types/process'

const query = reactive<ManufacturingBomQuery>({
  bomCode: undefined,
  bomName: undefined,
  productCode: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

const list = ref<ManufacturingBomVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const itemsDialogVisible = ref(false)
const itemsLoading = ref(false)
const bomItems = ref<ManufacturingBomItemVO[]>([])
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const form = reactive<ManufacturingBomDTO>({
  bomCode: '',
  bomName: '',
  version: '',
  productCode: '',
  productName: '',
  remark: '',
})

const formRules = {
  bomCode: [{ required: true, message: '请输入BOM编码', trigger: 'blur' }],
  bomName: [{ required: true, message: '请输入BOM名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await manufacturingBomApi.page(query)
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

async function handleRowClick(row: ManufacturingBomVO) {
  itemsDialogVisible.value = true
  itemsLoading.value = true
  bomItems.value = []
  try {
    bomItems.value = await manufacturingBomApi.getItemsTree(row.id)
  } finally {
    itemsLoading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: ManufacturingBomVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    bomCode: row.bomCode,
    bomName: row.bomName,
    version: row.version,
    productCode: row.productCode,
    productName: row.productName,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    bomCode: '',
    bomName: '',
    version: '',
    productCode: '',
    productName: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await manufacturingBomApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await manufacturingBomApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleUpgrade(row: ManufacturingBomVO) {
  loading.value = true
  try {
    await manufacturingBomApi.upgrade(row.id)
    ElMessage.success('版本升级成功')
    loadData()
  } finally {
    loading.value = false
  }
}

async function handlePublish(row: ManufacturingBomVO) {
  loading.value = true
  try {
    await manufacturingBomApi.publish(row.id)
    ElMessage.success('发布成功')
    loadData()
  } finally {
    loading.value = false
  }
}

async function handleDisable(row: ManufacturingBomVO) {
  loading.value = true
  try {
    await manufacturingBomApi.disable(row.id)
    ElMessage.success('停用成功')
    loadData()
  } finally {
    loading.value = false
  }
}

async function handleDelete(row: ManufacturingBomVO) {
  await ElMessageBox.confirm('确定要删除该BOM吗？', '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await manufacturingBomApi.delete(row.id)
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
