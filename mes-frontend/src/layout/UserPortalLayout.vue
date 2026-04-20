<template>
  <el-container class="user-portal-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">MES 现场端</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/app/workorder/list">生产工单</el-menu-item>
        <el-menu-item index="/app/dispatch/task">派工任务</el-menu-item>
        <el-menu-item index="/app/query/work-status">工作状态查询</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">现场作业</span>
        <el-dropdown @command="onCmd">
          <span class="user-line">
            <el-icon><UserFilled /></el-icon>
            {{ authStore.username || '用户' }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const active = computed(() => route.path)

function onCmd(cmd: string) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/app/login')
  }
}
</script>

<style scoped>
.user-portal-layout {
  min-height: 100vh;
}

.sidebar {
  background: #1d2b3a;
  color: #fff;
}

.logo {
  padding: 16px;
  font-weight: 600;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar :deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.sidebar :deep(.el-menu-item) {
  color: #c0c4cc;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: #409eff33;
  color: #fff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}

.title {
  font-weight: 600;
}

.user-line {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.main {
  background: #f5f7fa;
}
</style>
