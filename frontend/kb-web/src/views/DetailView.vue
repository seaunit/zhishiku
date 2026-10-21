<template>
  <section class="page detail-page">
    <RouterLink class="back-link" to="/">
      <ArrowLeft :size="17" />
      <span>{{ t('detail.backToSearch') }}</span>
    </RouterLink>

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
      <header class="detail-heading">
        <div>
          <h1>{{ item.title }}</h1>
          <p>{{ t('detail.updated', { date: formatDate(item.updatedAt) }) }}</p>
        </div>
        <RouterLink
          v-if="item.bodyDocument"
          class="button primary"
          :to="`/library/${item.id}/edit`"
        >
          <Pencil :size="17" />
          <span>{{ t('detail.edit') }}</span>
        </RouterLink>
      </header>

      <div class="detail-meta">
        <span class="tag">{{ item.source.kind }}</span>
        <span v-for="tag in item.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
        <span class="muted">{{ t('detail.processing', { status: item.processingStatus }) }}</span>
      </div>

      <article class="detail-sheet">
        <div v-if="documentError" class="inline-error" role="alert">
          <CircleAlert :size="20" />
          <p>{{ documentError }}</p>
        </div>
        <RichTextDocument v-else-if="item.bodyDocument" :document="item.bodyDocument" />
        <div v-else class="legacy-text">
          <h2>{{ t('detail.migrated') }}</h2>
          <p>{{ item.body || '—' }}</p>
        </div>
      </article>

      <aside v-if="item.note" class="note-panel">
        <h2>{{ t('detail.note') }}</h2>
        <p>{{ item.note }}</p>
      </aside>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft, CircleAlert, Pencil } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { getKnowledgeItem } from '@/api/client'
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

