<template>
  <section class="page detail-page">
    <div class="detail-topline">
      <RouterLink class="back-link" to="/library">
        <ArrowLeft :size="17" />
        <span>{{ t('detail.backToLibrary') }}</span>
      </RouterLink>
      <span v-if="item" class="detail-top-actions">
        <button
          type="button"
          class="text-button danger-text detail-delete-action"
          @click="deleteOpen = true"
        >
          <Trash2 :size="17" />
          <span>{{ t('detail.delete') }}</span>
        </button>
        <RouterLink
          v-if="item.bodyDocument"
          class="button primary detail-edit-action"
          :to="`/library/${item.id}/edit`"
        >
          <Pencil :size="17" />
          <span>{{ t('detail.edit') }}</span>
        </RouterLink>
        <button
          v-if="item.processingStatus === 'review'"
          type="button"
          class="button primary detail-review-action"
          :disabled="reviewing"
          @click="confirmReview"
        >
          <LoaderCircle v-if="reviewing" class="spin" :size="16" />
          <Check v-else :size="16" />
          <span>{{ t('detail.confirmReview') }}</span>
        </button>
      </span>
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
            <div class="aside-section-heading">
              <h2>{{ t('detail.classification') }}</h2>
              <span v-if="!classificationEditing" class="aside-heading-actions">
                <button
                  type="button"
                  class="text-button"
                  :disabled="reanalyzeBusy"
                  @click="prepareReanalysis"
                >
                  <LoaderCircle v-if="reanalyzeBusy" class="spin" :size="14" />
                  <span>{{ t('detail.reanalyze') }}</span>
                </button>
                <button type="button" class="text-button" @click="beginClassification">
                  {{ t('detail.correctClassification') }}
                </button>
              </span>
            </div>
            <form
              v-if="classificationEditing"
              class="classification-form"
              @submit.prevent="saveClassification"
            >
              <label>
                <span>{{ t('detail.category') }}</span>
                <input v-model.trim="categoryDraft" maxlength="100" required />
              </label>
              <label>
                <span>{{ t('detail.tags') }}</span>
                <input
                  v-model.trim="tagsDraft"
                  maxlength="500"
                  :placeholder="t('detail.tagsPlaceholder')"
                  required
                />
              </label>
              <p v-if="classificationError" class="field-error" role="alert">
                {{ classificationError }}
              </p>
              <div class="classification-actions">
                <button type="button" class="text-button" @click="cancelClassification">
                  {{ t('common.cancel') }}
                </button>
                <button type="submit" class="button primary" :disabled="classificationSaving">
                  <LoaderCircle v-if="classificationSaving" class="spin" :size="16" />
                  <span>{{ t('common.save') }}</span>
                </button>
              </div>
            </form>
            <template v-else>
              <div class="aside-field">
                <span>{{ t('detail.category') }}</span>
                <strong>{{ item.category?.name || t('common.uncategorized') }}</strong>
              </div>
              <div class="aside-field">
                <span>{{ t('detail.tags') }}</span>
                <div class="tag-list">
                  <span v-for="tag in item.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
                  <span v-if="item.tags.length === 0" class="muted">
                    {{ t('detail.noTags') }}
                  </span>
                </div>
              </div>
              <div v-if="item.category?.confidence" class="confidence">
                <span>{{ Math.round(item.category.confidence * 100) }}%</span>
                <div class="meter">
                  <span :style="{ width: `${item.category.confidence * 100}%` }" />
                </div>
              </div>
            </template>
            <div v-if="reanalyzeQuote" class="reanalyze-quote">
              <span>
                {{ t('detail.reanalyzeQuote', { points: reanalyzeQuote.maxPoints }) }}
                <small>
                  {{ t('detail.reanalyzeBalance', {
                    available: reanalyzeQuote.availablePoints,
                    remaining: reanalyzeQuote.projectedRemainingPoints,
                  }) }}
                </small>
              </span>
              <span class="classification-actions">
                <button type="button" class="text-button" @click="cancelReanalysis">
                  {{ t('common.cancel') }}
                </button>
                <button
                  type="button"
                  class="button primary"
                  :disabled="reanalyzeBusy"
                  @click="confirmReanalysis"
                >
                  {{ t('detail.confirmReanalyze') }}
                </button>
              </span>
            </div>
            <p v-if="reanalyzeNotice" class="success-message" role="status">
              {{ reanalyzeNotice }}
            </p>
            <p v-if="reanalyzeError" class="field-error" role="alert">{{ reanalyzeError }}</p>
          </section>

          <section class="aside-section">
            <h2>{{ t('detail.sourceInfo') }}</h2>
            <div class="aside-field">
              <span>{{ t('detail.sourceKind') }}</span>
              <strong>{{ sourceLabel(item.source.kind) }}</strong>
            </div>
            <div v-if="item.source.url" class="aside-field">
              <span>{{ t('detail.sourceUrl') }}</span>
              <a
                class="source-link"
                :href="item.source.url"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ item.source.url }}
              </a>
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
            <p v-if="reviewNotice" class="success-message" role="status">{{ reviewNotice }}</p>
            <p v-if="reviewError" class="field-error" role="alert">{{ reviewError }}</p>
          </section>
        </aside>
      </div>
    </template>

    <ConfirmDialog
      v-if="deleteOpen && item"
      :title="t('detail.deleteTitle')"
      :body="t('detail.deleteBody', { title: item.title })"
      :cancel-label="t('common.cancel')"
      :confirm-label="t('detail.confirmDelete')"
      :busy="deleting"
      :error="deleteError"
      @cancel="closeDelete"
      @confirm="confirmDelete"
    />
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft, Check, CircleAlert, LoaderCircle, Pencil, Trash2 } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  confirmBillingQuote,
  confirmKnowledgeReview,
  createBillingQuote,
  getKnowledgeItem,
  reanalyzeKnowledgeItem,
  recycleKnowledgeItem,
  updateKnowledgeClassification,
} from '@/api/client'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import {
  knowledgeStatusTone,
  knowledgeStatusTranslationKey,
  notifyReviewQueueChanged,
} from '@/lib/knowledge-status'
import { createIdempotencyKey } from '@/lib/id'
import { validateRichTextDocument } from '@/lib/rich-text'
import RichTextDocument from '@/components/RichTextDocument.vue'
import type { BillingQuote, KnowledgeItem } from '@/types/api'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const item = ref<KnowledgeItem | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const documentError = ref('')
const deleteOpen = ref(false)
const deleting = ref(false)
const deleteError = ref('')
const classificationEditing = ref(false)
const classificationSaving = ref(false)
const classificationError = ref('')
const categoryDraft = ref('')
const tagsDraft = ref('')
const reanalyzeQuote = ref<BillingQuote | null>(null)
const reanalyzeBusy = ref(false)
const reanalyzeNotice = ref('')
const reanalyzeError = ref('')
const reanalyzeKey = ref('')
const reviewing = ref(false)
const reviewNotice = ref('')
const reviewError = ref('')

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

async function confirmDelete() {
  if (!item.value) return
  deleting.value = true
  deleteError.value = ''
  try {
    await recycleKnowledgeItem(item.value.id)
    deleteOpen.value = false
    await router.replace('/trash')
  } catch (caught) {
    deleteError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    deleting.value = false
  }
}

function beginClassification() {
  classificationError.value = ''
  categoryDraft.value = item.value?.category?.name || ''
  tagsDraft.value = item.value?.tags.map((tag) => tag.name).join('、') || ''
  classificationEditing.value = true
}

function cancelClassification() {
  classificationEditing.value = false
  classificationError.value = ''
}

async function saveClassification() {
  if (!item.value) return
  const tags = tagsDraft.value
    .split(/[、,，\n]/)
    .map((value) => value.trim())
    .filter(Boolean)
  if (!categoryDraft.value) {
    classificationError.value = t('detail.categoryRequired')
    return
  }
  if (tags.length === 0) {
    classificationError.value = t('detail.tagsRequired')
    return
  }
  classificationSaving.value = true
  classificationError.value = ''
  try {
    item.value = await updateKnowledgeClassification(
      item.value.id,
      categoryDraft.value,
      tags,
    )
    classificationEditing.value = false
  } catch (caught) {
    classificationError.value =
      caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    classificationSaving.value = false
  }
}

async function prepareReanalysis() {
  if (!item.value || reanalyzeBusy.value) return
  reanalyzeBusy.value = true
  reanalyzeError.value = ''
  reanalyzeNotice.value = ''
  try {
    reanalyzeQuote.value = await createBillingQuote({
      operation: 'reanalyze',
      itemId: item.value.id,
      lines: [{ feature: 'classify', quantity: 1 }],
      useAutoCharge: true,
    })
    reanalyzeKey.value = createIdempotencyKey('knowledge-reanalyze')
  } catch (caught) {
    reanalyzeError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    reanalyzeBusy.value = false
  }
}

async function confirmReanalysis() {
  if (!item.value || !reanalyzeQuote.value || reanalyzeBusy.value) return
  reanalyzeBusy.value = true
  reanalyzeError.value = ''
  try {
    const task = await confirmBillingQuote(
      reanalyzeQuote.value.quoteId,
      reanalyzeQuote.value.maxPoints,
    )
    item.value = await reanalyzeKnowledgeItem(item.value.id, task.id, reanalyzeKey.value)
    reanalyzeQuote.value = null
    reanalyzeKey.value = ''
    reanalyzeNotice.value = t('detail.reanalyzeComplete')
    notifyReviewQueueChanged()
  } catch (caught) {
    reanalyzeError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    reanalyzeBusy.value = false
  }
}

function cancelReanalysis() {
  if (reanalyzeBusy.value) return
  reanalyzeQuote.value = null
  reanalyzeKey.value = ''
  reanalyzeError.value = ''
}

async function confirmReview() {
  if (!item.value || reviewing.value) return
  reviewing.value = true
  reviewError.value = ''
  reviewNotice.value = ''
  try {
    item.value = await confirmKnowledgeReview(item.value.id)
    reviewNotice.value = t('detail.reviewConfirmed')
    notifyReviewQueueChanged()
  } catch (caught) {
    reviewError.value = caught instanceof Error ? caught.message : t('detail.reviewFailed')
  } finally {
    reviewing.value = false
  }
}

function closeDelete() {
  if (deleting.value) return
  deleteOpen.value = false
  deleteError.value = ''
}

onMounted(load)
</script>

