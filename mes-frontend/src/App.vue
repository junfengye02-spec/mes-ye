<template>
  <!-- 使用 el-config-provider 让 Element Plus 内置文案（分页、日期选择器等）跟随 locale 切换 -->
  <el-config-provider :locale="localeStore.elLocale as any">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElConfigProvider } from 'element-plus'
import { useLocaleStore } from '@/stores/locale'
import { useThemeStore } from '@/stores/theme'

const localeStore = useLocaleStore()
const themeStore = useThemeStore()

// main.ts 已在挂载前先初始化一次主题避免首屏闪烁；
// 这里再兜底一次，保证热更新 / SSR / 异常刷新场景下主题监听仍然注册
onMounted(() => {
  themeStore.initTheme()
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
