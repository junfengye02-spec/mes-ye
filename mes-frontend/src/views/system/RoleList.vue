<template>
  <div class="role-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> {{ t('buttons.search') }}</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> {{ t('buttons.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">{{ t('system.role.title') }}</span>
          <el-button v-auth="['system:role:create']" type="primary" @click="openDialog(null)"><el-icon><Plus /></el-icon> {{ t('buttons.add') }}</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="roleCode" label="角色编码" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="['system:role:update']" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-auth="['system:role:assignMenu']" link type="success" @click="openMenuAssign(row)">分配菜单</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button v-auth="['system:role:delete']" link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next"
          @size-change="fetchList" @current-change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="450px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        check-strictly
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { sysRoleApi } from '@/api/system/role'
import type { SysRoleVO, SysRoleDTO, SysRoleQuery } from '@/api/system/role'
import { sysMenuApi } from '@/api/system/menu'
import type { SysMenuVO } from '@/api/system/menu'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<SysRoleVO[]>([])
const total = ref(0)
const query = reactive<SysRoleQuery>({ roleName: '', pageNum: 1, pageSize: 20 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<SysRoleDTO>({ roleName: '', roleCode: '', description: '', enabled: true })
const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const menuDialogVisible = ref(false)
const menuTree = ref<SysMenuVO[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRoleId = ref<number | null>(null)
const menuTreeRef = ref<any>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await sysRoleApi.page(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() { query.roleName = ''; query.pageNum = 1; fetchList() }

function openDialog(row: SysRoleVO | null) {
  editingId.value = row?.id || null
  form.roleName = row?.roleName || ''
  form.roleCode = row?.roleCode || ''
  form.description = row?.description || ''
  form.enabled = row?.enabled ?? true
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingId.value) {
    await sysRoleApi.update(editingId.value, form)
    ElMessage.success('修改成功')
  } else {
    await sysRoleApi.create(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

async function handleDelete(id: number) {
  await sysRoleApi.delete(id)
  ElMessage.success('删除成功')
  fetchList()
}

async function openMenuAssign(row: SysRoleVO) {
  currentRoleId.value = row.id
  menuTree.value = await sysMenuApi.getTree() || []
  checkedMenuIds.value = await sysRoleApi.getRoleMenuIds(row.id) || []
  menuDialogVisible.value = true
}

async function handleSaveMenus() {
  if (!currentRoleId.value) return
  const checkedIds = menuTreeRef.value?.getCheckedKeys() || []
  const halfCheckedIds = menuTreeRef.value?.getHalfCheckedKeys() || []
  await sysRoleApi.assignMenus(currentRoleId.value, [...checkedIds, ...halfCheckedIds])
  ElMessage.success('菜单权限已保存')
  menuDialogVisible.value = false
}

onMounted(() => fetchList())
</script>

<style scoped>
.role-list { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-title { font-weight: 600; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
