<template>
  <el-menu
    :default-active="currentPath"
    :collapse="collapse"
    :router="true"
    background-color="#1d1e1f"
    text-color="#bfcbd9"
    active-text-color="#409eff"
    :collapse-transition="false"
  >
    <template v-for="menu in menuList" :key="menu.path">
      <el-sub-menu v-if="menu.children?.length" :index="menu.path">
        <template #title>
          <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </template>
        <el-menu-item
          v-for="child in menu.children"
          :key="child.path"
          :index="child.path"
        >
          {{ child.title }}
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="menu.path">
        <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
        <span>{{ menu.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useActiveMenuList } from './useActiveMenuList'

defineProps<{ collapse: boolean }>()

const route = useRoute()
const currentPath = computed(() => route.path)
const menuList = useActiveMenuList()
</script>

<style scoped>
:deep(.el-menu) {
  border-right: none;
}
</style>
