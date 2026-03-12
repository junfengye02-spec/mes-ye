<template>
  <el-card shadow="never" class="search-form-card">
    <el-form :model="modelValue" inline @submit.prevent="$emit('search')">
      <slot />
      <el-form-item>
        <el-button type="primary" @click="$emit('search')">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
const props = defineProps<{
  modelValue: Record<string, any>
}>()
const emit = defineEmits<{
  search: []
  reset: []
}>()

const PRESERVED_KEYS = new Set(['pageNum', 'pageSize'])

function handleReset() {
  for (const key of Object.keys(props.modelValue)) {
    if (!PRESERVED_KEYS.has(key)) {
      props.modelValue[key] = undefined
    }
  }
  if (PRESERVED_KEYS.has('pageNum')) {
    props.modelValue.pageNum = 1
  }
  emit('reset')
  emit('search')
}
</script>

<style scoped>
.search-form-card {
  margin-bottom: 16px;
}
.search-form-card :deep(.el-card__body) {
  padding-bottom: 0;
}
</style>
