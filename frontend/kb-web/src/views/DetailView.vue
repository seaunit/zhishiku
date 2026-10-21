<template>
  <section class="page detail-page">
    <div class="detail-topline">
      <RouterLink class="back-link" to="/library">
        <ArrowLeft :size="17" />
        <span>{{ t('detail.backToLibrary') }}</span>
      </RouterLink>
      <RouterLink
        v-if="item?.bodyDocument"
        class="button primary detail-edit-action"
        :to="`/library/${item.id}/edit`"
      >
        <Pencil :size="17" />
        <span>{{ t('detail.edit') }}</span>
      </RouterLink>
    </div>

    <div v-if="loading" class="detail-skeleton" aria-busy="true" :aria-label="t('a11y.loading')">
      <span class="skeleton detail-title-line" />
      <span class="skeleton detail-text-line" />
      <span class="skeleton detail-text-line short" />
    </div>

    <div v-else-if="error" class="inline-error" role="alert">
      <CircleAlert :size="20" />
      <div>
        <strong>{{ notFound ? t('detail.notFound') : t('detail.loadFailed') }}</strong>
        <p>{{ error }}</p>
      </div>
      <button v-if="!notFound" type="button" class="text-button" @click="load">
        {{ t('common.retry') }}
      </button>
    </div>

    <template v-else-if="item">
      <div class="detail-layout">
        <article class="detail-main">
          <header class="detail-heading">
            <div>
              <h1>{{ item.title }}</h1>
              <p>{{ t('detail.updated', { date: formatDate(item.updatedAt) }) }}</p>
            </div>
          </header>

          <div class="detail-meta">
            <span class="tag">{{ item.category?.name || t('common.uncategorized') }}</span>
            <span v-for="tag in item.tags.slice(0, 4)" :key="tag.id" class="tag">
              {{ tag.name }}
            </span>
            <span class="status-pill" :class="statusClass(item.processingStatus)">
              {{ statusLabel(item.processingStatus) }}
            </span>
          </div>

          <div class="detail-body">
            <div v-if="documentError" class="inline-error" role="alert">
              <CircleAlert :size="20" />
              <p>{{ documentError }}</p>
            </div>
            <RichTextDocument v-else-if="item.bodyDocument" :document="item.bodyDocument" />
            <div v-else class="legacy-text">
              <h2>{{ t('detail.migrated') }}</h2>
              <p>{{ item.body || '—' }}</p>
            </div>
          </div>

          <aside v-if="item.note" class="note-panel">
            <h2>{{ t('detail.note') }}</h2>
            <p>{{ item.note }}</p>
          </aside>
        </article>

        <aside class="detail-aside">
          <section class="aside-section">
            <h2>{{ t('detail.classification') }}</h2>
            <div class="aside-field">
              <span>{{ t('detail.category') }}</span>
              <strong>{{ item.category?.name || t('common.uncategorized') }}</strong>
            </div>
            <div class="aside-field">
              <span>{{ t('detail.tags') }}</span>
              <div class="tag-list">
                <span v-for="tag in item.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
                <span v-if="item.tags.length === 0" class="muted">{{ t('detail.noTags') }}</span>
              </div>
            </div>
            <div v-if="item.category?.confidence" class="confidence">
              <span>{{ Math.round(item.category.confidence * 100) }}%</span>
              <div class="meter">
                <span :style="{ width: `${item.category.confidence * 100}%` }" />
              </div>
            </div>
          </section>

          <section class="aside-section">
            <h2>{{ t('detail.sourceInfo') }}</h2>
            <div class="aside-field">
              <span>{{ t('detail.sourceKind') }}</span>
              <strong>{{ sourceLabel(item.source.kind) }}</strong>
            </div>
            <div class="aside-field">
              <span>{{ t('detail.capturedAt') }}</span>
              <strong>{{ item.source.capturedAt ? formatDate(item.source.capturedAt) : '—' }}</strong>
            </div>
            <div class="aside-field">
              <span>{{ t('detail.updatedAt') }}</span>
              <strong>{{ formatDate(item.updatedAt) }}</strong>
            </div>
          </section>

          <section class="aside-section">
            <h2>{{ t('detail.statusInfo') }}</h2>
            <div class="aside-field">
              <span>{{ t('detail.processingStatus') }}</span>
              <strong>{{ statusLabel(item.processingStatus) }}</strong>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft, CircleAlert, Pencil } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { getKnowledgeItem } from '@/api/client'
import { knowledgeStatusTone, knowledgeStatusTranslationKey } from '@/lib/knowledge-status'
import { validateRichTextDocument } from '@/lib/rich-text'
import RichTextDocument from '@/components/RichTextDocument.vue'
import type { KnowledgeItem } from '@/types/api'

const { t, locale } = useI18n()
const route = useRoute()
const item = ref<KnowledgeItem | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const documentError = ref('')

function statusClass(status: string) {
  return knowledgeStatusTone(status)
}

function statusLabel(status: string) {
  return t(knowledgeStatusTranslationKey(status))
}

function sourceLabel(kind: string) {
  if (kind === 'webpage') return t('library.webpage')
  if (kind === 'document') return t('library.document')
  if (kind === 'screenshot') return t('library.screenshot')
  if (kind === 'manual' || kind === 'native') return t('library.manual')
  return kind
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date)
}

async function load() {
  loading.value = true
  error.value = ''
  notFound.value = false
  documentError.value = ''
  try {
    const loaded = await getKnowledgeItem(String(route.params.id))
    if (loaded.bodyDocument) {
      const result = validateRichTextDocument(loaded.bodyDocument)
      if (!result.ok) {
        documentError.value = result.message || t('errors.generic')
      }
    }
    item.value = loaded
  } catch (caught) {
    const status =
      typeof caught === 'object' && caught !== null && 'status' in caught
        ? Number(caught.status)
        : 0
    notFound.value = status === 404
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

