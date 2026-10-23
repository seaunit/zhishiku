<template>
  <section class="page processing-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('processing.title') }}</h1>
        <p>{{ t('processing.subtitle') }}</p>
      </div>
    </header>

    <section class="panel processing-panel">
      <p v-if="reviewNotice && !loading" class="success-message" role="status">
        {{ reviewNotice }}
      </p>
      <div v-if="loading" class="library-skeleton" :aria-label="t('a11y.loading')">
        <span v-for="index in 4" :key="index" class="skeleton" />
      </div>
      <div v-else-if="loadError" class="inline-error" role="alert">
        <CircleAlert :size="20" />
        <p>{{ loadError }}</p>
        <button type="button" class="text-button" @click="load()">{{ t('common.retry') }}</button>
      </div>
      <div v-else-if="items.length === 0" class="empty-state">
        <Inbox :size="22" />
        <h3>{{ t('processing.emptyTitle') }}</h3>
        <p>{{ t('processing.emptyBody') }}</p>
      </div>
      <template v-else>
        <div v-if="reviewError" class="inline-error" role="alert">
          <CircleAlert :size="20" />
          <p>{{ reviewError }}</p>
        </div>
        <div v-for="item in items" :key="item.id" class="processing-row">
          <RouterLink class="processing-info" :to="`/library/${item.id}`">
            <strong>{{ item.title }}</strong>
            <small>{{ item.summary || item.bodyPreview || '—' }}</small>
          </RouterLink>
          <span class="table-label">{{ sourceLabel(item.source.kind) }}</span>
          <span class="table-label">{{ item.category?.name || t('common.uncategorized') }}</span>
          <span class="status-pill" :class="statusClass(item.processingStatus)">
            {{ statusLabel(item.processingStatus) }}
          </span>
          <span class="processing-time">{{ formatUpdated(item.updatedAt) }}</span>
          <div class="row-actions">
            <button
              v-if="item.processingStatus === 'review'"
              type="button"
              class="button primary processing-review-action"
              :disabled="reviewingId === item.id"
              @click="confirmReview(item)"
            >
              <LoaderCircle v-if="reviewingId === item.id" class="spin" :size="15" />
              <Check v-else :size="15" />
              <span>{{ t('processing.confirmReview') }}</span>
            </button>
            <RouterLink v-else class="text-button" :to="`/library/${item.id}`">
              {{ t('processing.open') }}
            </RouterLink>
          </div>
        </div>
      </template>
      <footer
        v-if="!loading && pageInfo && pageInfo.totalItems > 0"
        class="library-pagination"
        :aria-label="t('processing.pagination')"
      >
        <span class="pagination-summary">
          {{
            t('library.pageSummary', {
              page: pageInfo.page,
              pages: pageInfo.totalPages,
              total: pageInfo.totalItems,
            })
          }}
        </span>
        <div class="pagination-controls">
          <button
            type="button"
            class="pagination-button"
            :aria-label="t('library.previousPage')"
            :disabled="pageInfo.page <= 1"
            @click="load(pageInfo.page - 1)"
          >
            <ChevronLeft :size="16" />
          </button>
          <button
            type="button"
            class="pagination-button"
            :aria-label="t('library.nextPage')"
            :disabled="pageInfo.page >= pageInfo.totalPages"
            @click="load(pageInfo.page + 1)"
          >
            <ChevronRight :size="16" />
          </button>
        </div>
      </footer>
    </section>
  </section>
</template>

<script setup lang="ts">
import {
  Check,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Inbox,
  LoaderCircle,
} from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { confirmKnowledgeReview, listKnowledgeItems } from '@/api/client'
import {
  knowledgeStatusTone,
  knowledgeStatusTranslationKey,
  notifyReviewQueueChanged,
  PENDING_PROCESSING_STATUSES,
  PENDING_PROCESSING_STATUS_QUERY,
} from '@/lib/knowledge-status'
import type { KnowledgeItemSummary, KnowledgePageInfo } from '@/types/api'

const { t, locale } = useI18n()
const QUEUE_STATUS_SET = new Set<string>(PENDING_PROCESSING_STATUSES)
const items = ref<KnowledgeItemSummary[]>([])
const loading = ref(true)
const loadError = ref('')
const reviewError = ref('')
const reviewNotice = ref('')
const reviewingId = ref('')
const pageInfo = ref<KnowledgePageInfo | null>(null)

function statusClass(status: string) {
  return knowledgeStatusTone(status)
}

function statusLabel(status: string) {
  return t(knowledgeStatusTranslationKey(status))
}

function sourceLabel(kind: string) {
  if (kind === 'text') return t('library.text')
  if (kind === 'internet_result') return t('library.internetResult')
  if (kind === 'webpage') return t('library.webpage')
  if (kind === 'document') return t('library.document')
  if (kind === 'screenshot') return t('library.screenshot')
  return t('library.manual')
}

function formatUpdated(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    month: 'short',
    day: 'numeric',
  }).format(date)
}

async function load(page = 1) {
  loading.value = true
  loadError.value = ''
  reviewError.value = ''
  try {
    const response = await listKnowledgeItems(20, page, 'all', PENDING_PROCESSING_STATUS_QUERY)
    items.value = response.items.filter((item) => QUEUE_STATUS_SET.has(item.processingStatus))
    pageInfo.value = response.pageInfo ?? null
  } catch (caught) {
    loadError.value = caught instanceof Error ? caught.message : t('processing.loadFailed')
  } finally {
    loading.value = false
  }
}

async function confirmReview(item: KnowledgeItemSummary) {
  if (reviewingId.value) return
  reviewingId.value = item.id
  reviewError.value = ''
  reviewNotice.value = ''
  try {
    await confirmKnowledgeReview(item.id)
    await load(pageInfo.value?.page ?? 1)
    reviewNotice.value = t('processing.reviewConfirmed')
    notifyReviewQueueChanged()
  } catch (caught) {
    reviewError.value = caught instanceof Error ? caught.message : t('processing.reviewFailed')
  } finally {
    reviewingId.value = ''
  }
}

onMounted(() => load())
</script>
