import request from '@/utils/request'
import type { AiChatRequest, AiChatResponse } from '@/types/ai'

export const aiAssistantApi = {
  chat: (data: AiChatRequest) => request.post<AiChatResponse>('/ai/assistant/chat', data, { skipErrorMessage: true }),
}
