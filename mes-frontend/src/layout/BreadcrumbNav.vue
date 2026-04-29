<template>
  <!--
    无障碍说明：
    1) 外层 <nav aria-label> 让屏幕阅读器把这一块识别为独立的"面包屑导航"地标
    2) el-breadcrumb 自带 role="navigation"，我们再补 aria-label 以区分主导航
  -->
  <nav :aria-label="t('common.a11y.breadcrumb')">
    <el-breadcrumb separator="/" :aria-label="t('common.a11y.breadcrumb')">
      <el-breadcrumb-item :to="{ path: '/' }">{{ t('common.home') }}</el-breadcrumb-item>
      <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
        {{ item.title }}
      </el-breadcrumb-item>
    </el-breadcrumb>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useActiveMenuList } from './useActiveMenuList'
import type { MenuItem } from './menuConfig'

const route = useRoute()
const { t, te } = useI18n()
const activeMenuList = useActiveMenuList()

function isPathUnder(currentPath: string, menuPath: string): boolean {
  return currentPath === menuPath || currentPath.startsWith(menuPath + '/')
}

function renderTitle(item: MenuItem): string {
  if (item.i18nKey && te(item.i18nKey)) return t(item.i18nKey)
  return item.title
}

const breadcrumbs = computed(() => {
  const path = route.path
  const result: { path: string; title: string }[] = []
  for (const menu of activeMenuList.value) {
    if (isPathUnder(path, menu.path)) {
      result.push({ path: menu.path, title: renderTitle(menu) })
      if (menu.children) {
        const child = menu.children.find(c => c.path === path)
        if (child) {
          result.push({ path: child.path, title: renderTitle(child) })
        }
      }
      break
    }
  }
  return result
})
</script>
