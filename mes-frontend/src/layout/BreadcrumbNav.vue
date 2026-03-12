<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useActiveMenuList } from './useActiveMenuList'

const route = useRoute()
const activeMenuList = useActiveMenuList()

function isPathUnder(currentPath: string, menuPath: string): boolean {
  return currentPath === menuPath || currentPath.startsWith(menuPath + '/')
}

const breadcrumbs = computed(() => {
  const path = route.path
  const result: { path: string; title: string }[] = []
  for (const menu of activeMenuList.value) {
    if (isPathUnder(path, menu.path)) {
      result.push({ path: menu.path, title: menu.title })
      if (menu.children) {
        const child = menu.children.find(c => c.path === path)
        if (child) {
          result.push({ path: child.path, title: child.title })
        }
      }
      break
    }
  }
  return result
})
</script>
