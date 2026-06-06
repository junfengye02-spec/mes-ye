<template>
  <el-container class="main-layout">
    <!-- 跳过链接：键盘用户按 Tab 第一下就能跳到主内容区 -->
    <a href="#main-content" class="skip-link">{{ t('common.a11y.skipToMain') }}</a>

    <el-aside
      id="main-sidebar"
      :width="isCollapse ? '64px' : '220px'"
      class="sidebar"
      role="navigation"
      :aria-label="t('common.a11y.mainNavigation')"
    >
      <div
        class="logo"
        role="button"
        tabindex="0"
        :aria-label="t('common.home')"
        @click="router.push('/')"
        @keydown.enter.prevent="router.push('/')"
        @keydown.space.prevent="router.push('/')"
      >
        <span v-if="!isCollapse" class="logo-text">{{ t('common.appName') }}</span>
        <span v-else class="logo-text-mini">M</span>
      </div>
      <el-scrollbar>
        <SidebarMenu :collapse="isCollapse" />
      </el-scrollbar>
    </el-aside>
    <el-container>
      <el-header
        class="header"
        role="banner"
        :aria-label="t('common.a11y.topToolbar')"
      >
        <div class="header-left">
          <!-- 使用真正的 button 元素替代纯 el-icon 点击：
               1) 原生 tab 可聚焦
               2) aria-expanded 反映侧边栏当前状态
               3) aria-label 提供屏幕阅读器可读名 -->
          <button
            type="button"
            class="collapse-btn-button"
            :aria-label="isCollapse ? t('common.a11y.expandSidebar') : t('common.a11y.collapseSidebar')"
            :aria-expanded="!isCollapse"
            aria-controls="main-sidebar"
            @click="isCollapse = !isCollapse"
          >
            <el-icon class="collapse-btn-icon">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </button>
          <BreadcrumbNav />
        </div>
        <div class="header-right">
          <button
            v-if="canUseAiAssistant"
            type="button"
            class="assistant-trigger"
            aria-label="AI助手"
            title="AI助手"
            @click="aiAssistantVisible = true"
          >
            <el-icon aria-hidden="true"><ChatDotRound /></el-icon>
          </button>

          <!-- 主题切换下拉：不引入新的 i18n 文案（P2-23 文案冻结），用图标 + aria-label 表达语义 -->
          <el-dropdown trigger="click" @command="handleThemeCommand">
            <span
              class="theme-dropdown"
              role="button"
              tabindex="0"
              :aria-label="themeTitle"
              :title="themeTitle"
            >
              <el-icon aria-hidden="true">
                <Sunny v-if="themeStore.mode === 'light'" />
                <Moon v-else-if="themeStore.mode === 'dark'" />
                <Monitor v-else />
              </el-icon>
              <el-icon class="el-icon--right" aria-hidden="true"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  command="light"
                  :disabled="themeStore.mode === 'light'"
                >
                  <el-icon><Sunny /></el-icon>
                  <span class="theme-item-text">Light</span>
                </el-dropdown-item>
                <el-dropdown-item
                  command="dark"
                  :disabled="themeStore.mode === 'dark'"
                >
                  <el-icon><Moon /></el-icon>
                  <span class="theme-item-text">Dark</span>
                </el-dropdown-item>
                <el-dropdown-item
                  command="auto"
                  :disabled="themeStore.mode === 'auto'"
                >
                  <el-icon><Monitor /></el-icon>
                  <span class="theme-item-text">Auto</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 语言切换下拉：aria-label 补上菜单用途 -->
          <el-dropdown trigger="click" @command="handleLocaleChange">
            <span
              class="lang-dropdown"
              role="button"
              tabindex="0"
              :aria-label="t('common.a11y.languageMenu')"
              :title="t('common.language')"
            >
              <span class="lang-flag" aria-hidden="true">🌐</span>
              <span class="lang-label">{{ currentLocaleLabel }}</span>
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

          <!-- 用户菜单 -->
          <el-dropdown @command="handleCommand">
            <span
              class="user-dropdown"
              role="button"
              tabindex="0"
              :aria-label="t('common.a11y.userMenu')"
            >
              <el-icon aria-hidden="true"><UserFilled /></el-icon>
              <span class="user-name">{{ authStore.username || t('common.tenant') }}</span>
              <el-icon class="el-icon--right" aria-hidden="true"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">{{ t('buttons.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main
        id="main-content"
        class="main-content"
        tabindex="-1"
        role="main"
        :aria-label="t('common.a11y.contentRegion')"
      >
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :max="15">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <AiAssistantDrawer v-model="aiAssistantVisible" />
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChatDotRound, Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import SidebarMenu from './SidebarMenu.vue'
import BreadcrumbNav from './BreadcrumbNav.vue'
import AiAssistantDrawer from '@/components/AiAssistant/AiAssistantDrawer.vue'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { useLocaleStore } from '@/stores/locale'
import { useThemeStore } from '@/stores/theme'
import { hasAnyPermission } from '@/directives/auth'
import type { ThemeMode } from '@/stores/theme'
import { SUPPORTED_LOCALES } from '@/locales'
import type { LocaleKey } from '@/locales'

const router = useRouter()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()
const localeStore = useLocaleStore()
const themeStore = useThemeStore()
const { t } = useI18n()
const isCollapse = ref(false)
const aiAssistantVisible = ref(false)

const currentLocaleLabel = computed(() => {
  const item = SUPPORTED_LOCALES.find(l => l.value === localeStore.current)
  return item ? item.short : ''
})

const canUseAiAssistant = computed(() => hasAnyPermission(['ai:assistant:chat']))

// 主题切换按钮的 title / aria-label：不走 vue-i18n 字典（P2-23 文案冻结），使用静态英文
const themeTitle = computed(() => {
  if (themeStore.mode === 'light') return 'Theme: Light'
  if (themeStore.mode === 'dark') return 'Theme: Dark'
  return 'Theme: Auto'
})

onMounted(async () => {
  if (authStore.isLoggedIn) {
    try {
      if (!authStore.userInfo) {
        await authStore.fetchUserInfo()
      }
      if (!permissionStore.loaded) {
        await permissionStore.loadUserMenus()
      }
    } catch {
      router.push('/login')
    }
  }
})

async function handleLocaleChange(locale: LocaleKey) {
  await localeStore.setLocale(locale)
}

/**
 * 主题切换：light / dark / auto
 *
 * @param mode 下拉项的 command 值
 */
function handleThemeCommand(mode: ThemeMode) {
  themeStore.setMode(mode)
}

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  background-color: var(--el-bg-color-page);
}
.sidebar {
  background: var(--mes-sidebar-bg);
  transition: width 0.3s, background-color 0.2s ease;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  border-bottom: 1px solid var(--mes-sidebar-border);
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}
.logo-text-mini {
  font-size: 22px;
  font-weight: 700;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--mes-header-border);
  background: var(--mes-header-bg);
  padding: 0 16px;
  height: 56px;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
/* 折叠按钮：原生 button，去掉默认样式但保留完整可访问性 */
.collapse-btn-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--mes-header-text);
  cursor: pointer;
  border-radius: 4px;
}
.collapse-btn-button:hover {
  color: var(--mes-primary);
  background: rgba(64, 158, 255, 0.08);
}
.collapse-btn-icon {
  font-size: 20px;
  pointer-events: none;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.assistant-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--mes-header-border);
  border-radius: 4px;
  background: var(--mes-header-bg);
  color: var(--mes-header-text);
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}
.assistant-trigger:hover,
.assistant-trigger:focus-visible {
  color: var(--mes-primary);
  border-color: var(--mes-primary);
  background: rgba(64, 158, 255, 0.08);
  outline: none;
}
.theme-dropdown,
.lang-dropdown {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--mes-header-text);
  font-size: 13px;
  user-select: none;
}
.theme-dropdown:hover,
.lang-dropdown:hover {
  color: var(--mes-primary);
}
.theme-item-text {
  margin-left: 6px;
}
.lang-flag {
  font-size: 16px;
  line-height: 1;
}
.lang-label {
  font-weight: 600;
}
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--mes-header-text);
  font-size: 14px;
}
.user-dropdown:hover {
  color: var(--mes-primary);
}
.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.main-content {
  background: var(--mes-bg-page);
  color: var(--el-text-color-primary);
  padding: 16px;
  overflow-y: auto;
  transition: background-color 0.2s ease, color 0.2s ease;
}
/* main tabindex="-1" 激活时不要显示难看的原生 outline，
 * 页面内部可聚焦元素自己会显示 :focus-visible 轮廓 */
.main-content:focus {
  outline: none;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
