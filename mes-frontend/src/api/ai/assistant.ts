import request from '@/utils/request'
import { API_BASE_URL } from '@/utils/apiBase'
import type { AiChatRequest, AiChatResponse } from '@/types/ai'

interface AiStreamHandlers {
  onDelta?: (content: string) => void
  onDone?: (response: AiChatResponse) => void
}

export const aiAssistantApi = {
  chat: (data: AiChatRequest) => request.post<AiChatResponse>('/ai/assistant/chat', data, { skipErrorMessage: true }),
  chatStream: (data: AiChatRequest, handlers: AiStreamHandlers = {}) => chatStream(data, handlers),
}

async function chatStream(data: AiChatRequest, handlers: AiStreamHandlers): Promise<AiChatResponse> {
  const token = localStorage.getItem('token')
  const response = await fetch(`${API_BASE_URL.replace(/\/$/, '')}/ai/assistant/chat/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    throw await toStreamError(response)
  }
  if (!response.body) {
    throw new Error('AI助手暂时不可用')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let finalResponse: AiChatResponse | null = null

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    buffer = buffer.replace(/\r\n/g, '\n')

    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    for (const raw of events) {
      const parsed = parseSseEvent(raw)
      if (!parsed.data) continue
      if (parsed.event === 'delta') {
        const payload = JSON.parse(parsed.data) as { content?: string }
        if (payload.content) handlers.onDelta?.(payload.content)
      } else if (parsed.event === 'done') {
        finalResponse = JSON.parse(parsed.data) as AiChatResponse
        handlers.onDone?.(finalResponse)
        await reader.cancel()
        return finalResponse
      } else if (parsed.event === 'error') {
        const payload = JSON.parse(parsed.data) as { message?: string }
        throw new Error(payload.message || 'AI助手暂时不可用')
      }
    }
    if (done) break
  }

  if (!finalResponse) {
    throw new Error('AI助手暂时不可用')
  }
  return finalResponse
}

function parseSseEvent(raw: string): { event: string, data: string } {
  let event = 'message'
  const data: string[] = []
  raw.split('\n').forEach((line) => {
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim()
    } else if (line.startsWith('data:')) {
      data.push(line.slice('data:'.length).trim())
    }
  })
  return { event, data: data.join('\n') }
}

async function toStreamError(response: Response): Promise<Error> {
  try {
    const body = await response.json()
    return new Error(body?.message || 'AI助手暂时不可用')
  } catch {
    return new Error('AI助手暂时不可用')
  }
}
