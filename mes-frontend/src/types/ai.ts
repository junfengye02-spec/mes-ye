export interface AiChatRequest {
  question: string
  pageContext?: string
  conversationId?: string
}

export interface AiChatResponse {
  answer: string
  intent: string
  relatedModules: string[]
  evidenceSummary: string[]
  suggestedNavigation: string[]
  refusalReason?: string | null
  modelConfigured?: boolean
}

export interface AiAssistantMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  response?: AiChatResponse
}
