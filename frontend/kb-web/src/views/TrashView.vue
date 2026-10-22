<template>
  <section class="page trash-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('trash.title') }}</h1>
        <p>{{ t('trash.subtitle') }}</p>
      </div>
    </header>

    <section class="panel trash-panel" :aria-busy="loading">
      <div v-if="loading" class="library-skeleton" :aria-label="t('a11y.loading')">
        <span v-for="index in 4" :key="index" class="skeleton" />
      </div>

      <div v-else-if="error" class="inline-error" role="alert">
        <CircleAlert :size="20" />
        <div>
          <strong>{{ t('trash.loadFailed') }}</strong>
          <p>{{ error }}</p>
        </div>
        <button type="button" class="text-button" @click="load">{{ t('common.retry') }}</button>
      </div>

      <div v-else-if="items.length === 0" class="empty-state">
        <Trash2 :size="22" />
        <h3>{{ t('trash.emptyTitle') }}</h3>
        <p>{{ t('trash.emptyBody') }}</p>
      </div>

      <div v-else class="trash-list">
        <article v-for="item in items" :key="item.id" class="trash-row">
          <span class="trash-main">
            <strong>{{ item.title }}</strong>
            <small>{{ item.summary || item.bodyPreview || '—' }}</small>
          </span>
          <span class="trash-meta">
            <span>{{ sourceLabel(item.source.kind) }}</span>
            <span>{{ formatDate(item.updatedAt) }}</span>
          </span>
          <span class="trash-actions">
            <button
              type="button"
              class="button secondary"
              :disabled="busyId === item.id"
              @click="restore(item)"
            >
              <LoaderCircle v-if="busyId === item.id" class="spin" :size="16" />
              <Undo2 v-else :size="16" />
              <span>{{ t('trash.restore') }}</span>
            </button>
            <button
              type="button"
              class="text-button danger-text"
              :disabled="busyId === item.id"
              @click="purgeTarget = item"
            >
              {{ t('trash.purge') }}
            </button>
          </span>
        </article>
      </div>
    </section>

    <ConfirmDialog
      v-if="purgeTarget"
      :title="t('trash.purgeTitle')"
      :body="t('trash.purgeBody', { title: purgeTarget.title })"
      :cancel-label="t('common.cancel')"
      :confirm-label="t('trash.confirmPurge')"
      :busy="busyId === purgeTarget.id"
      :error="actionError"
      @cancel="closePurge"
      @confirm="purge"
    />
  </section>
</template>

<script setup lang="ts">
import { CircleAlert, LoaderCircle, Trash2, Undo2 } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  listTrash,
  purgeKnowledgeItem,
  restoreKnowledgeItem,
} from '@/api/client'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import type { KnowledgeItemSummary } from '@/types/api'

const { t, locale } = useI18n()
const items = ref<KnowledgeItemSummary[]>([])
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const busyId = ref('')
const purgeTarget = ref<KnowledgeItemSummary | null>(null)

function sourceLabel(kind: string) {
  if (kind === 'text') return t('library.text')
  if (kind === 'internet_result') return t('library.internetResult')
  if (kind === 'webpage') return t('library.webpage')
  if (kind === 'document') return t('library.document')
  if (kind === 'screenshot') return t('library.screenshot')
  if (kind === 'manual' || kind === 'native') return t('library.manual')
  return kind
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = (await listTrash()).items
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

async function restore(item: KnowledgeItemSummary) {
  busyId.value = item.id
  error.value = ''
  try {
    await restoreKnowledgeItem(item.id)
    items.value = items.value.filter((candidate) => candidate.id !== item.id)
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    busyId.value = ''
  }
}

async function purge() {
  const item = purgeTarget.value
  if (!item) return
  busyId.value = item.id
  actionError.value = ''
  try {
    await purgeKnowledgeItem(item.id)
    items.value = items.value.filter((candidate) => candidate.id !== item.id)
    purgeTarget.value = null
  } catch (caught) {
    actionError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    busyId.value = ''
  }
}

function closePurge() {
  if (busyId.value) return
  purgeTarget.value = null
  actionError.value = ''
}

onMounted(load)
</script>
