<template>
  <div class="user-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="姓名" clearable style="width: 120px" />
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
          <span class="table-title">{{ t('system.user.title') }}</span>
          <el-button v-auth="['system:user:create']" type="primary" @click="openDialog(null)"><el-icon><Plus /></el-icon> {{ t('buttons.add') }}</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="factoryCode" label="所属工厂" width="110" />
        <el-table-column prop="accountType" label="账号类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.accountType === 'ADMIN' ? 'warning' : 'info'" size="small">
              {{ row.accountType === 'ADMIN' ? '管理端' : '现场端' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in (row.roles || [])" :key="r.id" size="small" style="margin-right: 4px">
              {{ r.roleName }}
            </el-tag>
            <span v-if="!row.roles?.length">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="['system:user:update']" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-auth="['system:user:resetPwd']" link type="warning" @click="handleResetPassword(row.id)">重置密码</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button v-auth="['system:user:delete']" link type="danger">删除</el-button></template>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="form.password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="所属工厂"><el-input v-model="form.factoryCode" /></el-form-item>
        <el-form-item label="账号类型">
          <el-select v-model="form.accountType" placeholder="默认现场端" style="width: 100%">
            <el-option label="管理端+现场端 (ADMIN)" value="ADMIN" />
            <el-option label="仅现场端 (STAFF)" value="STAFF" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { sysUserApi } from '@/api/system/user'
import type { SysUserVO, SysUserDTO, SysUserQuery } from '@/api/system/user'
import { sysRoleApi } from '@/api/system/role'
import type { SysRoleVO } from '@/api/system/role'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<SysUserVO[]>([])
const total = ref(0)
const query = reactive<SysUserQuery>({ username: '', realName: '', pageNum: 1, pageSize: 20 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const roleOptions = ref<SysRoleVO[]>([])

const form = reactive<SysUserDTO>({
  username: '', password: '', realName: '', phone: '', email: '',
  enabled: true, factoryCode: '', accountType: 'STAFF', roleIds: [],
})
const rules: FormRules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const res = await sysUserApi.page(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally { loading.value = false }
}

async function fetchRoles() {
  roleOptions.value = await sysRoleApi.list() || []
}

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() { query.username = ''; query.realName = ''; query.pageNum = 1; fetchList() }

function openDialog(row: SysUserVO | null) {
  editingId.value = row?.id || null
  form.username = row?.username || ''
  form.password = ''
  form.realName = row?.realName || ''
  form.phone = row?.phone || ''
  form.email = row?.email || ''
  form.enabled = row?.enabled ?? true
  form.factoryCode = row?.factoryCode || ''
  form.accountType = (row?.accountType as 'ADMIN' | 'STAFF') || 'STAFF'
  form.roleIds = row?.roles?.map(r => r.id) || []
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingId.value) {
    await sysUserApi.update(editingId.value, form)
    ElMessage.success('修改成功')
  } else {
    await sysUserApi.create(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

async function handleDelete(id: number) {
  await sysUserApi.delete(id)
  ElMessage.success('删除成功')
  fetchList()
}

async function handleResetPassword(id: number) {
  await sysUserApi.resetPassword(id)
  ElMessage.success('密码已重置为 123456')
}

onMounted(() => { fetchList(); fetchRoles() })
</script>

<style scoped>
.user-list { display: flex; flex-direction: column; gap: 16px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-title { font-weight: 600; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
