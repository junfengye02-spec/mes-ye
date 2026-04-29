<template>
  <!--
    无障碍说明：el-menu 渲染出的外层元素为 <ul role="menu">，
    我们通过 aria-label 让屏幕阅读器明确这是"主导航菜单"。
  -->
  <el-menu
    :default-active="currentPath"
    :collapse="collapse"
    :router="true"
    background-color="#1d1e1f"
    text-color="#bfcbd9"
    active-text-color="#409eff"
    :collapse-transition="false"
    :aria-label="t('common.a11y.mainNavigation')"
  >
    <template v-for="menu in menuList" :key="menu.path">
      <el-sub-menu v-if="menu.children?.length" :index="menu.path">
        <template #title>
          <el-icon v-if="menu.icon" aria-hidden="true"><component :is="menu.icon" /></el-icon>
          <span>{{ renderTitle(menu) }}</span>
        </template>
        <el-menu-item
          v-for="child in menu.children"
          :key="child.path"
          :index="child.path"
        >
          {{ renderTitle(child) }}
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="menu.path">
        <el-icon v-if="menu.icon" aria-hidden="true"><component :is="menu.icon" /></el-icon>
        <span>{{ renderTitle(menu) }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useActiveMenuList } from './useActiveMenuList'
import type { MenuItem } from './menuConfig'

defineProps<{ collapse: boolean }>()

const route = useRoute()
const { t, te } = useI18n()
const currentPath = computed(() => route.path)
const menuList = useActiveMenuList()

/**
 * 菜单标题翻译：优先 i18nKey，未命中或未配置时回退 title（原始中文）
 */
function renderTitle(item: MenuItem): string {
  if (item.i18nKey && te(item.i18nKey)) {
    return t(item.i18nKey)
  }
  return item.title
}
</script>

<style scoped>
:deep(.el-menu) {
  border-right: none;
}
</style>
