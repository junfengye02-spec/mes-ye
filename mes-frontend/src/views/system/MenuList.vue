<template>
  <div class="menu-list">
    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">菜单管理</span>
          <el-button type="primary" @click="openDialog(null)"><el-icon><Plus /></el-icon> 新增菜单</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="menuTree" row-key="id" border default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="80" align="center" />
        <el-table-column prop="path" label="路由路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="menuType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 'D'" type="warning">目录</el-tag>
            <el-tag v-else-if="row.menuType === 'M'">菜单</el-tag>
            <el-tag v-else-if="row.menuType === 'B'" type="danger">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="visible" label="可见" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visible ? 'success' : 'info'" size="small">{{ row.visible ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑菜单' : '新增菜单'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="menuTree" :props="{ label: 'menuName', children: 'children' }"
            node-key="id" value-key="id" check-strictly clearable placeholder="顶级菜单" style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="D">目录</el-radio>
            <el-radio value="M">菜单</el-radio>
            <el-radio value="B">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item v-if="form.menuType !== 'B'" label="路由路径"><el-input v-model="form.path" /></el-form-item>
        <el-form-item v-if="form.menuType === 'M'" label="组件路径"><el-input v-model="form.component" /></el-form-item>
        <el-form-item v-if="form.menuType !== 'B'" label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item v-if="form.menuType === 'B'" label="权限标识"><el-input v-model="form.permission" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="999" /></el-form-item>
        <el-form-item label="是否可见"><el-switch v-model="form.visible" /></el-form-item>
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
import { sysMenuApi } from '@/api/system/menu'
import type { SysMenuVO, SysMenuDTO } from '@/api/system/menu'

const loading = ref(false)
const menuTree = ref<SysMenuVO[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<SysMenuDTO>({
  parentId: undefined, menuName: '', path: '', component: '',
  menuType: 'M', permission: '', icon: '', sortOrder: 0, visible: true,
})
const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
}

async function fetchTree() {
  loading.value = true
  try { menuTree.value = await sysMenuApi.getTree() || [] }
  finally { loading.value = false }
}

function openDialog(row: SysMenuVO | null) {
  editingId.value = row?.id || null
  form.parentId = row?.parentId || undefined
  form.menuName = row?.menuName || ''
  form.path = row?.path || ''
  form.component = row?.component || ''
  form.menuType = row?.menuType || 'M'
  form.permission = row?.permission || ''
  form.icon = row?.icon || ''
  form.sortOrder = row?.sortOrder ?? 0
  form.visible = row?.visible ?? true
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingId.value) {
    await sysMenuApi.update(editingId.value, form)
    ElMessage.success('修改成功')
  } else {
    await sysMenuApi.create(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchTree()
}

async function handleDelete(id: number) {
  await sysMenuApi.delete(id)
  ElMessage.success('删除成功')
  fetchTree()
}

onMounted(() => fetchTree())
</script>

<style scoped>
.menu-list { display: flex; flex-direction: column; gap: 16px; }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-title { font-weight: 600; }
</style>
