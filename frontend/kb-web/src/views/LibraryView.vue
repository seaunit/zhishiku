<template>
  <section class="page library-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('library.title') }}</h1>
        <p>{{ t('library.subtitle') }}</p>
      </div>
      <RouterLink class="button primary desktop-action" to="/capture">
        <Plus :size="17" />
        <span>{{ t('nav.capture') }}</span>
      </RouterLink>
    </header>

    <div class="view-toolbar">
      <div class="filter-row" role="group" :aria-label="t('library.source')">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          class="filter-chip"
          :class="{ 'is-active': activeFilter === filter.value }"
          @click="activeFilter = filter.value"
        >
          {{ t(filter.label) }}
          <span class="num">{{ countFor(filter.value) }}</span>
        </button>
      </div>
      <span class="freshness">{{ t('library.freshness', { time: freshness }) }}</span>
    </div>

    <section class="panel library-panel" :aria-busy="loading">
      <div v-if="loading" class="library-skeleton" :aria-label="t('a11y.loading')">
        <span v-for="index in 6" :key="index" class="skeleton" />
      </div>

      <div v-else-if="error" class="inline-error" role="alert">
        <CircleAlert :size="20" />
        <div>
          <strong>{{ t('home.loadFailed') }}</strong>
          <p>{{ error }}</p>
        </div>
        <button type="button" class="text-button" @click="load">{{ t('common.retry') }}</button>
      </div>

      <div v-else-if="filteredItems.length === 0" class="empty-state">
        <BookOpen :size="22" />
        <h3>{{ t('library.emptyTitle') }}</h3>
        <p>{{ t('library.emptyBody') }}</p>
      </div>

      <div v-else class="knowledge-table">
        <div class="knowledge-table-head" aria-hidden="true">
          <span>{{ t('library.title') }}</span>
          <span>{{ t('library.category') }}</span>
          <span>{{ t('library.updated') }}</span>
          <span>{{ t('library.status') }}</span>
        </div>
        <RouterLink
          v-for="item in filteredItems"
          :key="item.id"
          class="knowledge-row"
          :to="`/library/${item.id}`"
        >
          <span class="knowledge-main">
            <span class="source-icon">
              <component :is="sourceIcon(item.source.kind)" :size="15" />
            </span>
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.summary || item.bodyPreview || '—' }}</small>
            </span>
          </span>
          <span class="table-label">{{ item.category?.name || t('common.uncategorized') }}</span>
          <span class="table-date">{{ formatDate(item.updatedAt) }}</span>
          <span class="table-source">
            <span class="status-pill" :class="statusClass(item.processingStatus)">
              {{ statusLabel(item.processingStatus) }}
            </span>
          </span>
        </RouterLink>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import {
  BookOpen,
  CircleAlert,
  FileText,
  Image,
  Link2,
  PenLine,
  Plus,
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listKnowledgeItems } from '@/api/client'
import { knowledgeStatusTone, knowledgeStatusTranslationKey } from '@/lib/knowledge-status'
import type { KnowledgeItemSummary } from '@/types/api'

const { t, locale } = useI18n()
const items = ref<KnowledgeItemSummary[]>([])
const loading = ref(true)
const error = ref('')
const activeFilter = ref('all')

const filters = [
  { value: 'all', label: 'library.all' },
  { value: 'webpage', label: 'library.webpage' },
  { value: 'document', label: 'library.document' },
  { value: 'screenshot', label: 'library.screenshot' },
  { value: 'text', label: 'library.text' },
  { value: 'manual', label: 'library.manual' },
]

const filteredItems = computed(() => {
  if (activeFilter.value === 'all') return items.value
  return items.value.filter((item) => item.source.kind === activeFilter.value)
})

const freshness = computed(() => {
  if (!items.value.length) return '—'
  const latest = items.value.reduce((current, item) =>
    new Date(item.updatedAt) > new Date(current.updatedAt) ? item : current,
  )
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(latest.updatedAt))
})

function countFor(filter: string) {
  return filter === 'all'
    ? items.value.length
    : items.value.filter((item) => item.source.kind === filter).length
}

function sourceIcon(kind: string) {
  if (kind === 'webpage') return Link2
  if (kind === 'document') return FileText
  if (kind === 'screenshot') return Image
  return PenLine
}

function statusClass(status: string) {
  return knowledgeStatusTone(status)
}

function statusLabel(status: string) {
  return t(knowledgeStatusTranslationKey(status))
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    month: 'short',
    day: 'numeric',
  }).format(date)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await listKnowledgeItems(100)
    items.value = response.items
  } catch (caught) {
    items.value = []
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
