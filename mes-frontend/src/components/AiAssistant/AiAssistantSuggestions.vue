<template>
  <div class="ai-suggestions" aria-label="AI助手推荐问题">
    <button
      v-for="suggestion in suggestions"
      :key="suggestion"
      type="button"
      class="suggestion-chip"
      @click="$emit('pick', suggestion)"
    >
      {{ suggestion }}
    </button>
  </div>
</template>

<script setup lang="ts">
defineEmits<{ pick: [question: string] }>()

const suggestions = [
  '查询未完成的生产作业',
  '派工质量不合格后系统怎么处理？',
  '今天有哪些检验工作需要关注？',
  'APS同步失败通常影响哪些流程？',
]
</script>

<style scoped>
.ai-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 0;
}
.suggestion-chip {
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 13px;
  line-height: 18px;
  cursor: pointer;
}
.suggestion-chip:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
</style>
