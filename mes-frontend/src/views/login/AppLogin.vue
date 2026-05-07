<template>
  <div class="login-container app-login">
    <main id="applogin-main" class="login-card" tabindex="-1" aria-labelledby="applogin-title">
      <div class="login-header">
        <h1 id="applogin-title">MES 现场端</h1>
        <p id="applogin-desc">操作员 / 班组长登录</p>
      </div>
      <div v-if="tenantBadge" class="tenant-badge">
        <el-tag type="success" effect="plain">租户：{{ tenantBadge }}</el-tag>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        aria-labelledby="applogin-title"
        aria-describedby="applogin-desc"
        @keyup.enter="handleLogin"
      >
        <el-form-item v-if="!domainLockedTenant" prop="tenantCode">
          <el-input
            v-model="form.tenantCode"
            placeholder="租户编码（多租户同名账号时必填）"
            aria-label="租户编码"
            size="large"
            :prefix-icon="TenantIcon"
            clearable
          />
        </el-form-item>
        <el-form-item prop="username">
          <el-input
            ref="usernameRef"
            v-model="form.username"
            placeholder="用户名"
            aria-label="用户名"
            autocomplete="username"
            size="large"
            :prefix-icon="UserIcon"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            aria-label="密码"
            autocomplete="current-password"
            size="large"
            :prefix-icon="LockIcon"
            show-password
          />
        </el-form-item>
        <el-form-item v-if="captchaVisible" prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="图形验证码"
              aria-label="图形验证码"
              autocomplete="off"
              size="large"
              maxlength="6"
            />
            <button
              type="button"
              class="captcha-image-btn"
              :disabled="captchaLoading"
              aria-label="刷新图形验证码"
              @click="loadCaptcha"
            >
              <img v-if="captchaImage" :src="captchaImage" alt="图形验证码" />
              <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            :aria-label="loading ? '登录中' : '登录'"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <!-- 登录状态动态提示：aria-live 让屏幕阅读器自动朗读登录成功/失败 -->
      <div class="login-live" role="status" aria-live="polite">{{ liveMessage }}</div>
      <div class="login-footer">
        <router-link to="/login">返回管理端登录</router-link>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, shallowRef, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, InputInstance } from 'element-plus'
import { User, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/system/auth'
import type { LoginParams } from '@/api/system/auth'
import { resolveTenantCodeFromHost, setStoredTenantCode } from '@/utils/tenant'

const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)
const TenantIcon = shallowRef(OfficeBuilding)

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const usernameRef = ref<InputInstance>()
const loading = ref(false)
const liveMessage = ref('')
const form = reactive({ username: '', password: '', tenantCode: '', captchaCode: '' })
const captchaVisible = ref(false)
const captchaLoading = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')

const domainLockedTenant = ref<string | null>(null)
const tenantBadge = computed(() => domainLockedTenant.value || form.tenantCode || '')

onMounted(() => {
  const fromDomain = resolveTenantCodeFromHost()
  if (fromDomain) {
    domainLockedTenant.value = fromDomain
    form.tenantCode = fromDomain
  }
  // 无障碍：自动聚焦第一个输入框，键盘用户省 Tab
  nextTick(() => {
    usernameRef.value?.focus?.()
  })
})

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: captchaVisible.value
    ? [{ required: true, message: '请输入图形验证码', trigger: 'blur' }]
    : [],
}))

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: LoginParams = {
    username: form.username,
    password: form.password,
    loginClient: 'USER',
  }
  const tenant = (domainLockedTenant.value || form.tenantCode || '').trim()
  if (tenant) payload.tenantCode = tenant
  if (captchaVisible.value) {
    payload.captchaKey = captchaKey.value
    payload.captchaCode = form.captchaCode.trim()
  }

  loading.value = true
  liveMessage.value = '登录中'
  try {
    await authStore.login(payload)
    setStoredTenantCode(tenant || null)
    ElMessage.success('登录成功')
    liveMessage.value = '登录成功'
    const redirect = (router.currentRoute.value.query.redirect as string) || '/app/workorder/list'
    const safeRedirect =
      redirect.startsWith('/app') && !redirect.startsWith('//') ? redirect : '/app/workorder/list'
    router.push(safeRedirect)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败，请检查用户名和密码'
    if (isCaptchaError(e)) {
      captchaVisible.value = true
    }
    if (captchaVisible.value) {
      form.captchaCode = ''
      await loadCaptcha().catch(() => undefined)
    }
    ElMessage.error(msg)
    liveMessage.value = msg
  } finally {
    loading.value = false
  }
}

function isCaptchaError(e: unknown): boolean {
  const code = Number((e as { code?: number })?.code)
  const message = e instanceof Error ? e.message : ''
  return code === 1101 || code === 1102 || message.includes('验证码')
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const captcha = await authApi.getCaptcha()
    captchaKey.value = captcha.captchaKey
    captchaImage.value = captcha.imageBase64
  } finally {
    captchaLoading.value = false
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
.login-card:focus {
  outline: none;
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

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
  width: 100%;
}

.captcha-image-btn {
  height: 40px;
  padding: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  overflow: hidden;
}

.captcha-image-btn:disabled {
  cursor: wait;
  opacity: 0.72;
}

.captcha-image-btn img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 可访问的隐藏状态提示（仅屏幕阅读器朗读） */
.login-live {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
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
