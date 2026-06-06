<template>
  <el-drawer
    :model-value="modelValue"
    title="AI助手"
    size="420px"
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="ai-assistant-drawer">
      <div class="message-list" aria-live="polite">
        <AiAssistantMessage
          v-for="message in messages"
          :key="message.id"
          :message="message"
          @navigate="handleNavigate"
        />
      </div>

      <AiAssistantSuggestions
        v-if="messages.length === 0"
        @pick="useSuggestion"
      />

      <div class="composer">
        <el-input
          v-model="question"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          maxlength="1000"
          show-word-limit
          placeholder="请输入生产相关问题"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="composer-actions">
          <el-button :disabled="loading || !question.trim()" type="primary" @click="handleSend">
            发送
          </el-button>
          <el-button :disabled="loading || messages.length === 0" @click="messages = []">
            清空
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { aiAssistantApi } from '@/api/ai/assistant'
import type { AiAssistantMessage } from '@/types/ai'
import AiAssistantMessageView from './AiAssistantMessage.vue'
import AiAssistantSuggestions from './AiAssistantSuggestions.vue'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const route = useRoute()
const router = useRouter()
const question = ref('')
const loading = ref(false)
const messages = ref<AiAssistantMessage[]>([])

const AiAssistantMessage = AiAssistantMessageView

async function handleSend() {
  const text = question.value.trim()
  if (!text || loading.value) return

  messages.value.push({ id: crypto.randomUUID(), role: 'user', content: text })
  messages.value.push({
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '',
    streaming: true,
  })
  const assistantMessage = messages.value[messages.value.length - 1]
  question.value = ''
  loading.value = true
  try {
    const response = await aiAssistantApi.chatStream(
      {
        question: text,
        pageContext: route.fullPath,
      },
      {
        onDelta: (content) => {
          assistantMessage.content += content
        },
        onDone: (response) => {
          assistantMessage.response = response
          if (!assistantMessage.content.trim()) {
            assistantMessage.content = response.answer
          }
        },
      },
    )
    assistantMessage.response = response
    if (!assistantMessage.content.trim()) {
      assistantMessage.content = response.answer
    }
  } catch (e: any) {
    const message = e?.message || 'AI助手暂时不可用'
    assistantMessage.content = message
    assistantMessage.response = {
      answer: message,
      intent: 'UNAVAILABLE',
      relatedModules: [],
      evidenceSummary: [],
      suggestedNavigation: [],
      refusalReason: message,
      modelConfigured: false,
    }
    ElMessage.error(message)
  } finally {
    assistantMessage.streaming = false
    loading.value = false
  }
}

function useSuggestion(value: string) {
  question.value = value
}

function handleNavigate(path: string) {
  if (!path.startsWith('/')) return
  router.push(path)
  emit('update:modelValue', false)
}
</script>

<style scoped>
.ai-assistant-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.message-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.composer {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
  margin-top: 12px;
}
.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
