<template>
  <div class="ai-message" :class="`is-${message.role}`">
    <div class="message-bubble">
      <div class="message-content">{{ safeContent }}</div>
      <div v-if="message.streaming" class="streaming-indicator">正在生成...</div>

      <div v-if="message.response?.refusalReason" class="refusal">
        {{ message.response.refusalReason }}
      </div>

      <div v-if="message.response?.relatedModules?.length" class="meta-row">
        <el-tag
          v-for="module in message.response.relatedModules"
          :key="module"
          size="small"
          effect="plain"
        >
          {{ module }}
        </el-tag>
      </div>

      <div v-if="message.response?.evidenceSummary?.length" class="evidence">
        <div
          v-for="item in message.response.evidenceSummary"
          :key="item"
          class="evidence-item"
        >
          {{ sanitize(item) }}
        </div>
      </div>

      <div v-if="message.response?.suggestedNavigation?.length" class="navigation-row">
        <el-button
          v-for="path in message.response.suggestedNavigation"
          :key="path"
          size="small"
          text
          @click="$emit('navigate', path)"
        >
          {{ navLabel(path) }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiAssistantMessage } from '@/types/ai'

const props = defineProps<{ message: AiAssistantMessage }>()
defineEmits<{ navigate: [path: string] }>()

const safeContent = computed(() => sanitize(props.message.content))

function sanitize(value: string): string {
  return (value || '')
    .split(/\r?\n/)
    .filter((line) => !/```|\/api\/|select\s+|insert\s+|update\s+|delete\s+|password\s*=|secret\s*=|token\s*=/i.test(line))
    .join('\n')
    .trim()
}

function navLabel(path: string): string {
  if (path.includes('production-work')) return '生产工作查询'
  if (path.includes('dispatch')) return '派工管理'
  if (path.includes('quality')) return '质量管理'
  if (path.includes('workorder')) return '工单管理'
  if (path.includes('material')) return '物料管理'
  if (path.includes('aps')) return 'APS'
  return '相关页面'
}
</script>

<style scoped>
.ai-message {
  display: flex;
  width: 100%;
}
.ai-message.is-user {
  justify-content: flex-end;
}
.ai-message.is-assistant {
  justify-content: flex-start;
}
.message-bubble {
  max-width: min(86%, 560px);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 10px 12px;
  background: var(--el-bg-color);
}
.is-user .message-bubble {
  color: #fff;
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
.message-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.6;
}
.streaming-indicator {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}
.refusal {
  margin-top: 8px;
  color: var(--el-color-danger);
  font-size: 13px;
  line-height: 1.5;
}
.meta-row,
.navigation-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.evidence {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}
.evidence-item + .evidence-item {
  margin-top: 4px;
}
</style>
