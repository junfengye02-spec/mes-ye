<template>
  <div class="data-mapping-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="映射类型">
          <el-input v-model="query.mappingType" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="MES编码">
          <el-input v-model="query.mesCode" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="APS编码">
          <el-input v-model="query.apsCode" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="启用">
          <el-select v-model="query.enabled" placeholder="请选择" clearable style="width: 140px">
            <el-option
              v-for="item in getDictList('yesNo')"
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
          <span>APS 数据映射</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="mappingType" label="映射类型" min-width="120" />
        <el-table-column prop="mesCode" label="MES编码" min-width="120" />
        <el-table-column prop="mesName" label="MES名称" min-width="120" />
        <el-table-column prop="apsCode" label="APS编码" min-width="120" />
        <el-table-column prop="apsName" label="APS名称" min-width="120" />
        <el-table-column prop="enabled" label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @update:model-value="(val: string | number | boolean) => handleEnabledChange(row, !!val)"
            />
          </template>
        </el-table-column>
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
      width="560px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="映射类型" prop="mappingType">
          <el-input v-model="form.mappingType" placeholder="请输入映射类型" />
        </el-form-item>
        <el-form-item label="MES编码" prop="mesCode">
          <el-input v-model="form.mesCode" placeholder="请输入MES编码" />
        </el-form-item>
        <el-form-item label="MES名称" prop="mesName">
          <el-input v-model="form.mesName" placeholder="请输入MES名称" />
        </el-form-item>
        <el-form-item label="APS编码" prop="apsCode">
          <el-input v-model="form.apsCode" placeholder="请输入APS编码" />
        </el-form-item>
        <el-form-item label="APS名称" prop="apsName">
          <el-input v-model="form.apsName" placeholder="请输入APS名称" />
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="formEnabled" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getDictList } from '@/utils/dict'
import type { ApsDataMappingVO, ApsDataMappingDTO, ApsDataMappingQuery } from '@/types/aps'
import { apsDataMappingApi } from '@/api/aps/apsSync'

const loading = ref(false)
const list = ref<ApsDataMappingVO[]>([])
const total = ref(0)
const query = reactive<ApsDataMappingQuery>({
  mappingType: '',
  mesCode: '',
  apsCode: '',
  enabled: undefined,
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增映射')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const form = reactive<ApsDataMappingDTO>({
  mappingType: '',
  mesCode: '',
  mesName: '',
  apsCode: '',
  apsName: '',
  enabled: 1,
})

const formEnabled = computed({
  get: () => form.enabled === 1,
  set: (val: boolean) => { form.enabled = val ? 1 : 0 },
})

const rules: FormRules = {
  mappingType: [{ required: true, message: '请输入映射类型', trigger: 'blur' }],
  mesCode: [{ required: true, message: '请输入MES编码', trigger: 'blur' }],
  apsCode: [{ required: true, message: '请输入APS编码', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await apsDataMappingApi.page(query)
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
  query.mappingType = ''
  query.mesCode = ''
  query.apsCode = ''
  query.enabled = undefined
  query.pageNum = 1
  loadList()
}

function handleAdd() {
  dialogTitle.value = '新增映射'
  editId.value = null
  Object.assign(form, {
    mappingType: '',
    mesCode: '',
    mesName: '',
    apsCode: '',
    apsName: '',
    enabled: 1,
  })
  dialogVisible.value = true
}

function handleEdit(row: ApsDataMappingVO) {
  dialogTitle.value = '编辑映射'
  editId.value = row.id
  Object.assign(form, {
    mappingType: row.mappingType ?? '',
    mesCode: row.mesCode ?? '',
    mesName: row.mesName ?? '',
    apsCode: row.apsCode ?? '',
    apsName: row.apsName ?? '',
    enabled: row.enabled ?? 1,
  })
  dialogVisible.value = true
}

async function handleEnabledChange(row: ApsDataMappingVO, val: boolean) {
  try {
    await apsDataMappingApi.update(row.id, {
      mappingType: row.mappingType,
      mesCode: row.mesCode,
      mesName: row.mesName,
      apsCode: row.apsCode,
      apsName: row.apsName,
      enabled: val ? 1 : 0,
    })
    ElMessage.success('更新成功')
    loadList()
  } catch {
    // error handled by request interceptor
  }
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (editId.value) {
      await apsDataMappingApi.update(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await apsDataMappingApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: ApsDataMappingVO) {
  ElMessageBox.confirm('确定要删除该映射吗？', '提示', {
    type: 'warning',
  }).then(async () => {
    await apsDataMappingApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.data-mapping-list {
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
