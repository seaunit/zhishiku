<template>
  <section class="page chat-page">
    <header class="page-heading chat-page-heading">
      <div>
        <h1>{{ t('chat.title') }}</h1>
        <p>{{ t('chat.subtitle') }}</p>
      </div>
      <div class="chat-heading-actions">
        <span class="chat-ai-state">
          <i aria-hidden="true" />
          {{ t('chat.available') }}
        </span>
        <button
          type="button"
          class="button secondary chat-new-action"
          :disabled="chatBusy"
          @click="startNewConversation"
        >
          <Plus :size="16" />
          <span>{{ t('chat.newConversation') }}</span>
        </button>
      </div>
    </header>

    <div class="chat-page-layout">
      <aside class="panel chat-history-panel" :aria-label="t('chat.historyTitle')">
        <header class="chat-history-head">
          <div>
            <h2>{{ t('chat.historyTitle') }}</h2>
            <span>{{ t('chat.historyCount', { count: conversations.length }) }}</span>
          </div>
          <button
            type="button"
            class="icon-button"
            :aria-label="t('chat.newConversation')"
              :disabled="chatBusy"
            @click="startNewConversation"
          >
            <Plus :size="17" />
          </button>
        </header>
        <div v-if="historyLoading" class="chat-history-state">{{ t('chat.historyLoading') }}</div>
        <div v-else-if="historyError" class="chat-history-state is-error">
          {{ historyError }}
        </div>
        <div v-else-if="conversations.length === 0" class="chat-history-state">
          {{ t('chat.historyEmpty') }}
        </div>
        <ul v-else class="chat-history-list">
          <li v-for="conversation in conversations" :key="conversation.id">
            <button
              type="button"
              class="chat-history-open"
              :class="{ 'is-active': conversation.id === activeConversationId }"
              :disabled="chatBusy"
              @click="openConversation(conversation.id)"
            >
              <strong>{{ conversation.title }}</strong>
              <small>{{ conversation.lastMessage || t('chat.historyNoMessage') }}</small>
              <time>{{ formatConversationTime(conversation.updatedAt) }}</time>
            </button>
            <button
              type="button"
              class="chat-history-delete"
              :aria-label="t('chat.deleteConversationLabel', { title: conversation.title })"
              :disabled="chatBusy"
              @click="requestDeleteConversation(conversation)"
            >
              <Trash2 :size="14" />
            </button>
          </li>
        </ul>
      </aside>

      <div class="chat-main-column">
        <div class="chat-mobile-history">
          <label>
            <span>{{ t('chat.historyTitle') }}</span>
            <select
              :value="activeConversationId"
              :disabled="chatBusy"
              @change="selectConversation($event)"
            >
              <option value="">{{ t('chat.newConversation') }}</option>
              <option v-for="conversation in conversations" :key="conversation.id" :value="conversation.id">
                {{ conversation.title }}
              </option>
            </select>
          </label>
          <button
            type="button"
            class="icon-button"
            :aria-label="t('chat.newConversation')"
            :disabled="chatBusy"
            @click="startNewConversation"
          >
            <Plus :size="17" />
          </button>
        </div>

        <section class="panel chat-workspace">
      <header class="chat-workspace-head">
        <span class="chat-assistant-mark" aria-hidden="true">
          <Waves :size="19" />
        </span>
        <span>
          <strong>{{ t('chat.assistantName') }}</strong>
          <small>{{ t('chat.assistantScope') }}</small>
        </span>
      </header>

      <div ref="streamElement" class="chat-stream" aria-live="polite">
        <section v-if="conversationLoading" class="chat-empty chat-loading-state">
          <span>{{ t('chat.conversationLoading') }}</span>
        </section>
        <section v-else-if="messages.length === 0 && !sending" class="chat-empty">
          <span class="chat-empty-mark" aria-hidden="true"><MessagesSquare :size="22" /></span>
          <h2>{{ t('chat.emptyTitle') }}</h2>
          <p>{{ t('chat.emptyBody') }}</p>
          <div class="chat-prompts">
            <button
              v-for="prompt in samplePrompts"
              :key="prompt"
              type="button"
              @click="usePrompt(prompt)"
            >
              {{ prompt }}
              <ArrowUpRight :size="15" />
            </button>
          </div>
        </section>

        <article
          v-for="message in messages"
          :key="message.id"
          class="chat-message"
          :class="`is-${message.role}`"
        >
          <header class="chat-message-head">
            <time v-if="message.role === 'user'" class="chat-message-time">
              {{ message.time }}
            </time>
            <span class="chat-avatar" :class="`is-${message.role}`" aria-hidden="true">
              <Waves v-if="message.role === 'assistant'" :size="16" />
              <template v-else>{{ userInitial }}</template>
            </span>
            <span class="chat-message-meta">
              <strong>{{ message.role === 'user' ? t('chat.you') : t('chat.assistantName') }}</strong>
              <time v-if="message.role === 'assistant'">{{ message.time }}</time>
            </span>
          </header>
          <p v-if="message.pending" class="chat-typing">
            <span />
            <span />
            <span />
            <b>{{ message.content }}</b>
          </p>
          <template v-else>
            <p class="chat-message-body" :class="{ 'is-streaming': message.streaming }">
              {{ message.content }}
            </p>
            <button
              v-if="message.needsInternet && !message.internetUsed"
              type="button"
              class="chat-internet-action"
              :disabled="chatBusy"
              @click="continueWithInternet(message)"
            >
              <Globe2 :size="17" />
              <span>
                <strong>{{ t('chat.internetAction') }}</strong>
                <small>{{ t('chat.internetActionHint') }}</small>
              </span>
              <ArrowRight :size="17" />
            </button>
            <ul v-if="message.sources.length" class="chat-sources">
              <li v-for="(source, index) in message.sources" :key="sourceKey(source, index)">
                <RouterLink
                  v-if="source.kind === 'knowledge' && source.knowledgeId"
                  :to="`/library/${source.knowledgeId}`"
                >
                  <span class="source-index num">{{ index + 1 }}</span>
                  <span>
                    <strong>{{ source.title }}</strong>
                    <small>{{ source.excerpt }}</small>
                  </span>
                  <BookOpen :size="15" />
                </RouterLink>
                <a
                  v-else-if="isSafeExternalUrl(source.url)"
                  :href="source.url || undefined"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span class="source-index num">{{ index + 1 }}</span>
                  <span>
                    <strong>{{ source.title }}</strong>
                    <small>{{ source.provider || source.excerpt }}</small>
                  </span>
                  <ExternalLink :size="15" />
                </a>
              </li>
            </ul>
          </template>
        </article>
      </div>

      <form class="chat-composer" @submit.prevent="sendQuestion">
        <textarea
          ref="inputElement"
          v-model="draft"
          rows="1"
          maxlength="300"
          :aria-label="t('chat.inputLabel')"
          :placeholder="t('chat.placeholder')"
          :disabled="chatBusy"
          @keydown.enter.exact.prevent="sendQuestion"
        />
        <div class="chat-composer-foot">
          <span>{{ t('chat.localRetrievalFree') }}</span>
          <button type="submit" class="button primary" :disabled="chatBusy || !draft.trim()">
            <LoaderCircle v-if="sending" class="spin" :size="16" />
            <SendHorizontal v-else :size="16" />
            <span>{{ sending ? t('chat.sending') : t('chat.sendWithPoints') }}</span>
          </button>
        </div>
      </form>
        </section>
      </div>
    </div>

    <ConfirmDialog
      v-if="deleteTarget"
      :title="t('chat.deleteConversationTitle')"
      :body="t('chat.deleteConversationBody', { title: deleteTarget.title })"
      :cancel-label="t('common.cancel')"
      :confirm-label="deletingConversation ? t('chat.deletingConversation') : t('chat.deleteConversation')"
      :busy="deletingConversation"
      :error="deleteError"
      @cancel="cancelDeleteConversation"
      @confirm="confirmDeleteConversation"
    />
  </section>
</template>

<script setup lang="ts">
import {
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  ExternalLink,
  Globe2,
  LoaderCircle,
  MessagesSquare,
  Plus,
  SendHorizontal,
  Trash2,
  Waves,
} from 'lucide-vue-next'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ApiError,
  appendChatConversationMessage,
  confirmBillingQuote,
  createChatConversation,
  createBillingQuote,
  deleteChatConversation,
  getChatConversation,
  listChatConversations,
  streamKnowledgeChat,
} from '@/api/client'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { createIdempotencyKey } from '@/lib/id'
import { useSessionStore } from '@/stores/session'
import type {
  AppendChatMessageRequest,
  ChatConversation,
  KnowledgeChatHistoryMessage,
  KnowledgeChatSource,
} from '@/types/api'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  query: string
  time: string
  sources: KnowledgeChatSource[]
  needsInternet: boolean
  internetUsed: boolean
  pending: boolean
  streaming: boolean
}

const { t, locale } = useI18n()
const session = useSessionStore()
const messages = ref<ChatMessage[]>([])
const conversations = ref<ChatConversation[]>([])
const activeConversationId = ref('')
const draft = ref('')
const sending = ref(false)
const historyLoading = ref(true)
const historyError = ref('')
const conversationLoading = ref(false)
const deleteTarget = ref<ChatConversation | null>(null)
const deletingConversation = ref(false)
const deleteError = ref('')
const streamElement = ref<HTMLElement | null>(null)
const inputElement = ref<HTMLTextAreaElement | null>(null)
let sequence = 0

const userDisplayName = computed(
  () =>
    session.session?.displayName ||
    session.session?.email.split('@')[0] ||
    t('chat.you'),
)
const userInitial = computed(() => {
  const name = userDisplayName.value
  return Array.from(name.trim())[0]?.toUpperCase() || '你'
})
const chatBusy = computed(() => sending.value || historyLoading.value || conversationLoading.value)

const samplePrompts = computed(() => [
  t('chat.sampleOne'),
  t('chat.sampleTwo'),
  t('chat.sampleThree'),
])

function formatMessageTime(value: string | Date) {
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(value instanceof Date ? value : new Date(value))
}

function formatConversationTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return formatMessageTime(date)
  }
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    month: 'short',
    day: 'numeric',
  }).format(date)
}

async function loadHistory() {
  historyLoading.value = true
  historyError.value = ''
  try {
    await session.restore()
    const page = await listChatConversations()
    conversations.value = page.items
    if (page.items[0]) {
      await openConversation(page.items[0].id, false)
    }
  } catch {
    historyError.value = t('chat.historyLoadFailed')
  } finally {
    historyLoading.value = false
  }
}

async function openConversation(id: string, focusInput = true) {
  if (sending.value || conversationLoading.value || !id || id === activeConversationId.value) {
    return
  }
  conversationLoading.value = true
  historyError.value = ''
  try {
    const detail = await getChatConversation(id)
    activeConversationId.value = detail.conversation.id
    replaceConversation(detail.conversation)
    messages.value = detail.messages.map((message) => ({
      id: ++sequence,
      role: message.role,
      content: message.content,
      query: '',
      time: formatMessageTime(message.createdAt),
      sources: message.sources,
      needsInternet: false,
      internetUsed: message.needsInternet,
      pending: false,
      streaming: false,
    }))
    await scrollToLatest()
    if (focusInput) {
      inputElement.value?.focus()
    }
  } catch {
    historyError.value = t('chat.historyLoadFailed')
  } finally {
    conversationLoading.value = false
  }
}

function startNewConversation() {
  if (chatBusy.value) {
    return
  }
  activeConversationId.value = ''
  messages.value = []
  draft.value = ''
  historyError.value = ''
  inputElement.value?.focus()
}

function selectConversation(event: Event) {
  const id = (event.target as HTMLSelectElement).value
  if (id) {
    void openConversation(id)
  } else {
    startNewConversation()
  }
}

async function ensureConversation(query: string) {
  if (activeConversationId.value) {
    return activeConversationId.value
  }
  const created = await createChatConversation(
    query.slice(0, 100),
    createIdempotencyKey('chat-conversation'),
  )
  activeConversationId.value = created.id
  replaceConversation(created)
  return created.id
}

async function persistMessage(
  conversationId: string,
  payload: AppendChatMessageRequest,
) {
  const persisted = await appendChatConversationMessage(
    conversationId,
    payload,
    createIdempotencyKey('chat-message'),
  )
  const current = conversations.value.find((conversation) => conversation.id === conversationId)
  if (current) {
    replaceConversation({
      ...current,
      lastMessage: payload.content.replace(/\s+/g, ' ').slice(0, 160),
      messageCount: current.messageCount + 1,
      updatedAt: persisted.createdAt,
    })
  }
  return persisted
}

async function persistMessageBestEffort(
  conversationId: string,
  payload: AppendChatMessageRequest,
) {
  if (!conversationId) {
    return
  }
  try {
    await persistMessage(conversationId, payload)
  } catch {
    historyError.value = t('chat.historySaveFailed')
  }
}

function replaceConversation(conversation: ChatConversation) {
  conversations.value = [
    conversation,
    ...conversations.value.filter((item) => item.id !== conversation.id),
  ]
}

function requestDeleteConversation(conversation: ChatConversation) {
  if (chatBusy.value) {
    return
  }
  deleteError.value = ''
  deleteTarget.value = conversation
}

function cancelDeleteConversation() {
  if (deletingConversation.value) {
    return
  }
  deleteTarget.value = null
  deleteError.value = ''
}

async function confirmDeleteConversation() {
  if (!deleteTarget.value || deletingConversation.value) {
    return
  }
  deletingConversation.value = true
  deleteError.value = ''
  const target = deleteTarget.value
  try {
    await deleteChatConversation(target.id, createIdempotencyKey('chat-delete'))
    conversations.value = conversations.value.filter((item) => item.id !== target.id)
    deleteTarget.value = null
    if (activeConversationId.value === target.id) {
      if (conversations.value[0]) {
        activeConversationId.value = ''
        await openConversation(conversations.value[0].id, false)
      } else {
        startNewConversation()
      }
    }
  } catch (caught) {
    deleteError.value = caught instanceof Error ? caught.message : t('chat.deleteConversationFailed')
  } finally {
    deletingConversation.value = false
  }
}

function addMessage(
  role: ChatMessage['role'],
  content: string,
  query: string,
  options: Partial<Pick<ChatMessage, 'sources' | 'needsInternet' | 'pending'>> = {},
) {
  const message: ChatMessage = {
    id: ++sequence,
    role,
    content,
    query,
    time: formatMessageTime(new Date()),
    sources: options.sources || [],
    needsInternet: options.needsInternet || false,
    internetUsed: false,
    pending: options.pending || false,
    streaming: false,
  }
  messages.value = [...messages.value, message]
  void scrollToLatest()
  return message
}

onMounted(() => {
  void loadHistory()
})

function updateMessage(id: number, patch: Partial<ChatMessage>) {
  messages.value = messages.value.map((message) =>
    message.id === id ? { ...message, ...patch } : message,
  )
  void scrollToLatest()
}

function historyBeforeCurrent(currentQuery = ''): KnowledgeChatHistoryMessage[] {
  const prior = messages.value
    .filter((message) => !message.pending)
    .map((message) => ({ role: message.role, content: message.content }))
  if (currentQuery) {
    for (let index = prior.length - 1; index >= 0; index -= 1) {
      if (prior[index]?.role === 'user' && prior[index]?.content === currentQuery) {
        prior.splice(index, 1)
        break
      }
    }
  }
  return prior.slice(-6)
}

async function scrollToLatest() {
  await nextTick()
  if (streamElement.value) {
    streamElement.value.scrollTop = streamElement.value.scrollHeight
  }
}

function usePrompt(prompt: string) {
  draft.value = prompt
  inputElement.value?.focus()
}

async function sendQuestion() {
  const query = draft.value.trim()
  if (!query || chatBusy.value) {
    return
  }
  const history = historyBeforeCurrent()
  draft.value = ''
  addMessage('user', query, query)
  sending.value = true
  let conversationId = ''
  let pending: ChatMessage | undefined
  try {
    conversationId = await ensureConversation(query)
    await persistMessage(conversationId, {
      role: 'user',
      content: query,
      sources: [],
      needsInternet: false,
    })
    const pendingMessage = addMessage('assistant', t('chat.retrieving'), query, { pending: true })
    pending = pendingMessage
    updateMessage(pendingMessage.id, { content: t('chat.thinking') })
    const quote = await createBillingQuote({
      operation: 'knowledge_chat',
      lines: [{ feature: 'ai_answer', quantity: 1 }],
    })
    const task = await confirmBillingQuote(quote.quoteId, quote.maxPoints)
    let streamed = ''
    const answer = await streamKnowledgeChat(
      {
        query,
        history,
        externalAiConsent: true,
        billingTaskId: task.id,
      },
      {
        idempotencyKey: createIdempotencyKey('knowledge-chat-stream'),
        onToken: (text) => {
          streamed += text
          updateMessage(pendingMessage.id, {
            content: streamed,
            pending: false,
            streaming: true,
          })
        },
      },
    )
    if (answer.needsInternet) {
      const content = t('chat.knowledgeInsufficient')
      updateMessage(pendingMessage.id, {
        content,
        needsInternet: true,
        pending: false,
        streaming: false,
        sources: answer.sources,
      })
      await persistMessageBestEffort(conversationId, {
        role: 'assistant',
        content,
        sources: answer.sources,
        needsInternet: true,
        reason: answer.reason || 'knowledge_insufficient',
      })
      return
    }
    updateMessage(pendingMessage.id, {
      content: answer.answer,
      sources: answer.sources,
      pending: false,
      streaming: false,
    })
    await persistMessageBestEffort(conversationId, {
      role: 'assistant',
      content: answer.answer,
      sources: answer.sources,
      needsInternet: false,
      reason: null,
    })
  } catch (caught) {
    if (!pending) {
      pending = addMessage('assistant', t('chat.retrieving'), query, { pending: true })
    }
    const content = chatErrorMessage(caught)
    updateMessage(pending.id, {
      content,
      pending: false,
      streaming: false,
    })
    await persistMessageBestEffort(conversationId, {
      role: 'assistant',
      content,
      sources: [],
      needsInternet: false,
      reason: 'chat_failed',
    })
  } finally {
    sending.value = false
    inputElement.value?.focus()
  }
}

async function continueWithInternet(message: ChatMessage) {
  if (chatBusy.value || message.internetUsed) {
    return
  }
  updateMessage(message.id, { internetUsed: true })
  sending.value = true
  const history = historyBeforeCurrent(message.query)
  const conversationId = activeConversationId.value
  const pending = addMessage('assistant', t('chat.internetSearching'), message.query, {
    pending: true,
  })
  try {
    const quote = await createBillingQuote({
      operation: 'internet_search_with_answer',
      lines: [
        { feature: 'internet_search', quantity: 1 },
        { feature: 'ai_answer', quantity: 1 },
      ],
    })
    const task = await confirmBillingQuote(quote.quoteId, quote.maxPoints)
    let streamed = ''
    const answer = await streamKnowledgeChat(
      {
        query: message.query,
        history,
        externalAiConsent: true,
        billingTaskId: task.id,
      },
      {
        internet: true,
        idempotencyKey: createIdempotencyKey('knowledge-chat-internet-stream'),
        onToken: (text) => {
          streamed += text
          updateMessage(pending.id, {
            content: streamed,
            pending: false,
            streaming: true,
          })
        },
      },
    )
    if (answer.answer) {
      updateMessage(pending.id, {
        content: answer.answer,
        sources: answer.sources,
        pending: false,
        streaming: false,
      })
      await persistMessageBestEffort(conversationId, {
        role: 'assistant',
        content: answer.answer,
        sources: answer.sources,
        needsInternet: false,
        reason: null,
      })
      return
    }
    const content =
      answer.reason === 'internet_no_results'
        ? t('chat.internetNoResults')
        : t('chat.internetInsufficient')
    updateMessage(pending.id, {
      content,
      sources: answer.sources,
      pending: false,
      streaming: false,
    })
    await persistMessageBestEffort(conversationId, {
      role: 'assistant',
      content,
      sources: answer.sources,
      needsInternet: false,
      reason: answer.reason || 'internet_insufficient_evidence',
    })
  } catch (caught) {
    updateMessage(message.id, { internetUsed: false })
    const content = chatErrorMessage(caught)
    updateMessage(pending.id, {
      content,
      pending: false,
      streaming: false,
    })
    await persistMessageBestEffort(conversationId, {
      role: 'assistant',
      content,
      sources: [],
      needsInternet: false,
      reason: 'chat_failed',
    })
  } finally {
    sending.value = false
    inputElement.value?.focus()
  }
}

function sourceKey(source: KnowledgeChatSource, index: number) {
  return source.knowledgeId || source.url || `${source.kind}-${index}`
}

function isSafeExternalUrl(value?: string | null) {
  if (!value) {
    return false
  }
  try {
    const url = new URL(value)
    return (url.protocol === 'https:' || url.protocol === 'http:') && !url.username && !url.password
  } catch {
    return false
  }
}

function chatErrorMessage(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'INSUFFICIENT_POINTS') {
      return t('chat.insufficientPoints')
    }
    if (
      caught.code === 'BILLING_DISABLED' ||
      caught.code === 'BILLING_BRIDGE_UNAVAILABLE' ||
      caught.code === 'AI_BILLING_REQUIRED'
    ) {
      return t('chat.billingUnavailable')
    }
    if (caught.code === 'AI_EXTERNAL_CONSENT_REQUIRED') {
      return t('chat.consentRequired')
    }
    if (caught.code === 'EXTERNAL_SEARCH_UNAVAILABLE') {
      return t('chat.internetUnavailable')
    }
    if (caught.code === 'WEB_IMPORT_UNAVAILABLE') {
      return t('chat.aiUnavailable')
    }
  }
  return caught instanceof Error ? caught.message : t('errors.generic')
}
</script>
