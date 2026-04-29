<template>
  <div class="login-container">
    <!-- 右上角语言切换 -->
    <div class="login-lang">
      <el-dropdown trigger="click" @command="handleLocaleChange">
        <span
          class="lang-trigger"
          role="button"
          tabindex="0"
          :aria-label="t('common.a11y.languageMenu')"
        >
          <span aria-hidden="true">🌐</span>
          {{ currentLocaleShort }}
          <el-icon class="el-icon--right" aria-hidden="true"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="opt in SUPPORTED_LOCALES"
              :key="opt.value"
              :command="opt.value"
              :disabled="opt.value === localeStore.current"
            >
              {{ opt.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <main id="login-main" class="login-card" tabindex="-1" aria-labelledby="login-title">
      <div class="login-header">
        <h1 id="login-title">{{ t('login.title') }}</h1>
        <p id="login-desc">{{ t('login.subtitle') }}</p>
      </div>
      <div v-if="tenantBadge" class="tenant-badge">
        <el-tag type="info" effect="plain">{{ t('login.tenantBadge', { code: tenantBadge }) }}</el-tag>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        aria-labelledby="login-title"
        aria-describedby="login-desc"
        @keyup.enter="handleLogin"
      >
        <el-form-item v-if="!domainLockedTenant" prop="tenantCode">
          <el-input
            v-model="form.tenantCode"
            :placeholder="t('login.tenantCode')"
            :aria-label="t('login.tenantCode')"
            size="large"
            :prefix-icon="TenantIcon"
            clearable
          />
        </el-form-item>
        <el-form-item prop="username">
          <el-input
            ref="usernameRef"
            v-model="form.username"
            :placeholder="t('login.username')"
            :aria-label="t('login.username')"
            autocomplete="username"
            size="large"
            :prefix-icon="UserIcon"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.password')"
            :aria-label="t('login.password')"
            autocomplete="current-password"
            size="large"
            :prefix-icon="LockIcon"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            :aria-label="loading ? t('buttons.loggingIn') : t('buttons.login')"
            @click="handleLogin"
          >
            {{ loading ? t('buttons.loggingIn') : t('buttons.login') }}
          </el-button>
        </el-form-item>
      </el-form>
      <!-- 登录状态动态提示：aria-live 让屏幕阅读器自动朗读登录成功/失败 -->
      <div class="login-live" role="status" aria-live="polite">{{ liveMessage }}</div>
      <div class="login-footer">
        <span>{{ t('common.appVersion') }}</span>
        <div class="portal-switch">
          <router-link to="/app/login">{{ t('login.portalSwitch') }}</router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, shallowRef, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, InputInstance } from 'element-plus'
import { User, Lock, OfficeBuilding, ArrowDown } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useLocaleStore } from '@/stores/locale'
import { SUPPORTED_LOCALES } from '@/locales'
import type { LocaleKey } from '@/locales'
import { resolveTenantCodeFromHost, setStoredTenantCode } from '@/utils/tenant'

const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)
const TenantIcon = shallowRef(OfficeBuilding)

const router = useRouter()
const authStore = useAuthStore()
const localeStore = useLocaleStore()
const { t } = useI18n()

const formRef = ref<FormInstance>()
const usernameRef = ref<InputInstance>()
const loading = ref(false)
const liveMessage = ref('')
const form = reactive({ username: '', password: '', tenantCode: '' })

const domainLockedTenant = ref<string | null>(null)
const tenantBadge = computed(() => domainLockedTenant.value || form.tenantCode || '')

const currentLocaleShort = computed(() => {
  const item = SUPPORTED_LOCALES.find(l => l.value === localeStore.current)
  return item ? item.short : ''
})

onMounted(() => {
  const fromDomain = resolveTenantCodeFromHost()
  if (fromDomain) {
    domainLockedTenant.value = fromDomain
    form.tenantCode = fromDomain
  }
  // 无障碍：进入登录页后自动聚焦到第一个输入项，减少键盘用户 Tab 次数
  nextTick(() => {
    usernameRef.value?.focus?.()
  })
})

// rules 作为 computed 确保切换语言后错误消息实时更新
const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('login.rules.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.rules.passwordRequired'), trigger: 'blur' }],
}))

async function handleLocaleChange(locale: LocaleKey) {
  await localeStore.setLocale(locale)
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: { username: string; password: string; loginClient: 'ADMIN'; tenantCode?: string } = {
    username: form.username,
    password: form.password,
    loginClient: 'ADMIN',
  }
  const tenant = (domainLockedTenant.value || form.tenantCode || '').trim()
  if (tenant) {
    payload.tenantCode = tenant
  }

  loading.value = true
  liveMessage.value = t('buttons.loggingIn')
  try {
    await authStore.login(payload)
    setStoredTenantCode(tenant || null)
    ElMessage.success(t('messages.loginSuccess'))
    liveMessage.value = t('messages.loginSuccess')
    const redirect = (router.currentRoute.value.query.redirect as string) || '/'
    const safeRedirect = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
    router.push(safeRedirect)
  } catch (e: any) {
    const msg = e?.message || t('messages.loginFail')
    ElMessage.error(msg)
    liveMessage.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-lang {
  position: absolute;
  top: 20px;
  right: 24px;
  color: #fff;
}
.lang-trigger {
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.15);
  user-select: none;
}
.lang-trigger:hover {
  background: rgba(255, 255, 255, 0.25);
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
  margin-bottom: 24px;
}

.login-header h1 {
  font-size: 22px;
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
  font-size: 12px;
  color: #c0c4cc;
}

.portal-switch {
  margin-top: 10px;
}

.portal-switch a {
  color: #409eff;
  font-size: 13px;
}
</style>
