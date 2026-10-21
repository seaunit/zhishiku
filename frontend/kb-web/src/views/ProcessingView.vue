<template>
  <section class="page processing-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('processing.title') }}</h1>
        <p>{{ t('processing.subtitle') }}</p>
      </div>
    </header>

    <section class="panel processing-panel">
      <div v-if="loading" class="library-skeleton" :aria-label="t('a11y.loading')">
        <span v-for="index in 4" :key="index" class="skeleton" />
      </div>
      <div v-else-if="items.length === 0" class="empty-state">
        <Inbox :size="22" />
        <h3>{{ t('processing.emptyTitle') }}</h3>
        <p>{{ t('processing.emptyBody') }}</p>
      </div>
      <RouterLink
        v-for="item in items"
        v-else
        :key="item.id"
        class="processing-row"
        :to="`/library/${item.id}`"
      >
        <span class="processing-info">
          <strong>{{ item.title }}</strong>
          <small>{{ item.summary || item.bodyPreview || '—' }}</small>
        </span>
        <span class="table-label">{{ item.category?.name || t('common.uncategorized') }}</span>
        <span class="status-pill" :class="statusClass(item.processingStatus)">
          {{ statusLabel(item.processingStatus) }}
        </span>
        <span class="mini-action">{{ t('processing.review') }}</span>
      </RouterLink>
    </section>
  </section>
</template>

<script setup lang="ts">
import { Inbox } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listKnowledgeItems } from '@/api/client'
import { knowledgeStatusTone, knowledgeStatusTranslationKey } from '@/lib/knowledge-status'
import type { KnowledgeItemSummary } from '@/types/api'

const { t } = useI18n()
const items = ref<KnowledgeItemSummary[]>([])
const loading = ref(true)

function isPending(status: string) {
  return ['pending', 'queued', 'running', 'processing', 'review', 'failed'].includes(status)
}

function statusClass(status: string) {
  return knowledgeStatusTone(status)
}

function statusLabel(status: string) {
  return t(knowledgeStatusTranslationKey(status))
}

onMounted(async () => {
  try {
    const response = await listKnowledgeItems(100)
    items.value = response.items.filter((item) => isPending(item.processingStatus))
  } finally {
    loading.value = false
  }
})
</script>
