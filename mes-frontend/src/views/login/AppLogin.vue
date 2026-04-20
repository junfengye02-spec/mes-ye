<template>
  <div class="login-container app-login">
    <div class="login-card">
      <div class="login-header">
        <h1>MES 现场端</h1>
        <p>操作员 / 班组长登录</p>
      </div>
      <div v-if="tenantBadge" class="tenant-badge">
        <el-tag type="success" effect="plain">租户：{{ tenantBadge }}</el-tag>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item v-if="!domainLockedTenant" prop="tenantCode">
          <el-input
            v-model="form.tenantCode"
            placeholder="租户编码（多租户同名账号时必填）"
            size="large"
            :prefix-icon="TenantIcon"
            clearable
          />
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="UserIcon" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="LockIcon"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <router-link to="/login">返回管理端登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, shallowRef, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { resolveTenantCodeFromHost, setStoredTenantCode } from '@/utils/tenant'

const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)
const TenantIcon = shallowRef(OfficeBuilding)

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '', tenantCode: '' })

const domainLockedTenant = ref<string | null>(null)
const tenantBadge = computed(() => domainLockedTenant.value || form.tenantCode || '')

onMounted(() => {
  const fromDomain = resolveTenantCodeFromHost()
  if (fromDomain) {
    domainLockedTenant.value = fromDomain
    form.tenantCode = fromDomain
  }
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: { username: string; password: string; loginClient: 'USER'; tenantCode?: string } = {
    username: form.username,
    password: form.password,
    loginClient: 'USER',
  }
  const tenant = (domainLockedTenant.value || form.tenantCode || '').trim()
  if (tenant) payload.tenantCode = tenant

  loading.value = true
  try {
    await authStore.login(payload)
    setStoredTenantCode(tenant || null)
    ElMessage.success('登录成功')
    const redirect = (router.currentRoute.value.query.redirect as string) || '/app/workorder/list'
    const safeRedirect =
      redirect.startsWith('/app') && !redirect.startsWith('//') ? redirect : '/app/workorder/list'
    router.push(safeRedirect)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败，请检查用户名和密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px 0;
}

.login-header p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.tenant-badge {
  text-align: center;
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
}

.login-footer {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
}

.login-footer a {
  color: #409eff;
}
</style>
