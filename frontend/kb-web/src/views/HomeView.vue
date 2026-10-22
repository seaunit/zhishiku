<template>
  <section class="page home-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('home.title') }}</h1>
        <p>{{ t('home.subtitle') }}</p>
      </div>
      <RouterLink class="button primary desktop-action" to="/capture">
        <SquarePen :size="17" />
        <span>{{ t('nav.capture') }}</span>
      </RouterLink>
    </header>

    <form class="search-shell" role="search" @submit.prevent="submitSearch">
      <div class="search-field">
        <Search :size="20" aria-hidden="true" />
        <input
          id="knowledge-search"
          v-model="query"
          type="search"
          name="q"
          autocomplete="off"
          :aria-label="t('home.searchLabel')"
          :placeholder="t('home.searchPlaceholder')"
        />
        <kbd>Enter</kbd>
      </div>
      <button class="search-action" type="submit" :disabled="loading">
        <LoaderCircle v-if="loading" class="spin" :size="18" />
        <template v-else>
          <span>{{ t('home.searchButton') }}</span>
          <ArrowRight :size="18" />
        </template>
      </button>
    </form>

    <div class="search-context">
      <span class="local-first"><i aria-hidden="true" />{{ t('home.localFirst') }}</span>
      <span>{{ t('home.searchHint') }}</span>
    </div>

    <div class="stats-strip" :aria-label="t('a11y.knowledgeStats')">
      <div class="stat-cell">
        <span>{{ t('home.statTotal') }}</span>
        <strong class="num">{{ allItems.length }}</strong>
      </div>
      <div class="stat-cell">
        <span>{{ t('home.statCategories') }}</span>
        <strong class="num">{{ categoryCount }}</strong>
      </div>
      <div class="stat-cell">
        <span>{{ t('home.statPending') }}</span>
        <strong class="num">{{ queueItems.length }}</strong>
      </div>
      <div class="stat-cell">
        <span>{{ t('home.statLatest') }}</span>
        <strong class="num">{{ latestTime }}</strong>
      </div>
    </div>

    <div class="home-grid">
      <section class="panel results-panel" aria-labelledby="results-title">
        <header class="panel-heading">
          <div>
            <h2 id="results-title">{{ t('home.recentTitle') }}</h2>
            <p>
              {{
                searchedQuery
                  ? t('home.resultCount', { count: items.length })
                  : t('home.recentSummary', { count: items.length })
              }}
            </p>
          </div>
          <RouterLink class="text-link" to="/library">{{ t('home.viewAll') }}</RouterLink>
        </header>

        <div v-if="loading" class="result-list" aria-busy="true" :aria-label="t('a11y.loading')">
          <div v-for="index in 4" :key="index" class="result-skeleton">
            <span class="skeleton title-line" />
            <span class="skeleton text-line" />
            <span class="skeleton meta-line" />
          </div>
        </div>

        <div v-else-if="error" class="inline-error" role="alert">
          <CircleAlert :size="20" />
          <div>
            <strong>{{ searchedQuery ? t('home.searchFailed') : t('home.loadFailed') }}</strong>
            <p>{{ error }}</p>
          </div>
          <button type="button" class="text-button" @click="load">{{ t('common.retry') }}</button>
        </div>

        <div v-else-if="items.length === 0" class="empty-state">
          <Filter :size="22" />
          <h3>{{ searchedQuery ? t('home.noResults') : t('home.emptyLibrary') }}</h3>
          <p>{{ searchedQuery ? t('home.noResultsHint') : t('home.emptyLibraryHint') }}</p>
          <RouterLink class="button secondary" to="/capture">
            <SquarePen :size="17" />
            <span>{{ t('nav.capture') }}</span>
          </RouterLink>
        </div>

        <div v-else class="result-list" :aria-label="t('a11y.searchResults')">
          <RouterLink
            v-for="item in displayedItems"
            :key="item.id"
            class="result-row"
            :to="`/library/${item.id}`"
          >
            <span class="source-icon">
              <component :is="sourceIcon(item.source.kind)" :size="15" />
            </span>
            <span class="result-body">
              <strong class="result-title">{{ item.title || t('home.untitled') }}</strong>
              <span class="result-summary">{{ item.summary || item.bodyPreview || '—' }}</span>
              <span class="result-meta">
                <span>{{ item.category?.name || t('common.uncategorized') }}</span>
                <span>{{ item.tags.slice(0, 2).map((tag) => tag.name).join(' / ') || '—' }}</span>
                <span>{{ sourceLabel(item.source.kind) }}</span>
                <span v-if="matchedFieldsByItem[item.id]?.length">
                  {{
                    t('home.matchedFields', {
                      fields: matchedFieldsByItem[item.id]
                        .map((field) => t(`home.match.${field}`))
                        .join(t('common.listSeparator')),
                    })
                  }}
                </span>
              </span>
            </span>
            <span class="status-pill" :class="statusClass(item.processingStatus)">
              {{ statusLabel(item.processingStatus) }}
            </span>
          </RouterLink>
        </div>

        <div class="internet-search">
          <button
            v-if="!externalVisible"
            type="button"
            class="internet-trigger"
            :disabled="!searchedQuery || loading"
            @click="searchExternalInternet"
          >
            <span>
              <strong>
                <Globe2 :size="16" />
                {{ t('home.internetSearchWithPoints', { points: 1 }) }}
              </strong>
              <small>
                {{
                  searchedQuery
                    ? t('home.internetHint')
                    : t('home.internetHintPending')
                }}
              </small>
            </span>
            <ArrowRight :size="17" />
          </button>

          <div v-else class="external-results">
            <header class="external-heading">
              <span><Globe2 :size="15" />{{ t('home.internetResults') }}</span>
              <span v-if="externalLoading" class="external-status">
                <LoaderCircle class="spin" :size="14" />
                {{ t('home.internetLoading') }}
              </span>
              <span v-else>{{ externalItems.length }}</span>
            </header>

            <div v-if="externalError" class="inline-error external-error" role="alert">
              <CircleAlert :size="18" />
              <div>
                <strong>{{ t('home.internetFailed') }}</strong>
                <p>{{ externalError }}</p>
              </div>
              <button type="button" class="text-button" @click="searchExternalInternet">
                {{ t('home.internetRetry') }}
              </button>
            </div>

            <div v-else-if="externalLoading" class="external-loading">
              <span class="skeleton title-line" />
              <span class="skeleton text-line" />
            </div>

            <div v-else-if="externalItems.length === 0" class="external-empty">
              {{ t('home.internetEmpty') }}
            </div>

            <template v-else>
              <article
                v-for="candidate in externalItems"
                :key="candidate.candidateId"
                class="external-row"
              >
                <Globe2 :size="16" />
                <div>
                  <a
                    class="external-title"
                    :href="candidate.url"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {{ candidate.title }}
                  </a>
                  <p>{{ candidate.snippet || candidate.provider }}</p>
                  <small>{{ candidate.provider }}</small>
                  <p
                    v-if="candidateErrors[candidate.candidateId]"
                    class="candidate-error"
                    role="alert"
                  >
                    {{ candidateErrors[candidate.candidateId] }}
                  </p>
                </div>
                <div class="external-actions">
                  <a
                    class="text-link"
                    :href="candidate.url"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {{ t('home.internetOpen') }}
                  </a>
                  <button
                    type="button"
                    class="button secondary"
                    :disabled="savingCandidateId === candidate.candidateId"
                    @click="saveCandidate(candidate)"
                  >
                    <LoaderCircle
                      v-if="savingCandidateId === candidate.candidateId"
                      class="spin"
                      :size="15"
                    />
                    {{
                      savingCandidateId === candidate.candidateId
                        ? t('home.internetSaving')
                        : candidateQuote &&
                            candidateQuoteId === candidate.candidateId
                          ? t('home.confirmImport', { points: candidateQuote.maxPoints })
                          : t('home.internetSave')
                    }}
                  </button>
                </div>
              </article>
            </template>
          </div>
        </div>
      </section>

      <aside class="right-rail">
        <section class="panel depth-panel" aria-labelledby="depth-title">
          <header class="panel-heading compact">
            <div>
              <h2 id="depth-title">{{ t('home.depthTitle') }}</h2>
              <p>{{ t('home.depthHint') }}</p>
            </div>
            <Layers :size="18" class="panel-icon" aria-hidden="true" />
          </header>

          <div class="depth-map" :aria-label="t('a11y.knowledgeMap')">
            <svg viewBox="0 0 320 216" aria-hidden="true">
              <path class="map-contour" d="M-8 70C46 27 92 30 136 61s79 36 129 8 66-11 76 5" />
              <path class="map-contour" d="M-10 126c48-33 95-33 143-1s83 31 126 1 60-24 82-6" />
              <path class="map-contour" d="M-6 181c45-31 96-29 142-5s81 27 121 4 57-22 79-8" />
            </svg>
            <button
              v-for="node in mapNodes"
              :key="node.name"
              type="button"
              class="map-node"
              :class="node.depth"
              :style="{ left: `${node.x}%`, top: `${node.y}%` }"
              :title="`${node.name} · ${node.count}`"
              @click="searchCategory(node.name)"
            >
              <i aria-hidden="true" />
              <span>{{ node.name }}</span>
              <b class="num">{{ node.count }}</b>
            </button>
          </div>

          <div class="depth-legend">
            <span><i class="surface" />{{ t('home.depthSurface') }}</span>
            <span><i class="middle" />{{ t('home.depthMiddle') }}</span>
            <span><i class="deep" />{{ t('home.depthDeep') }}</span>
          </div>
        </section>

        <section class="panel queue-panel" aria-labelledby="queue-title">
          <header class="panel-heading compact">
            <div>
              <h2 id="queue-title">{{ t('home.queueTitle') }}</h2>
              <p>{{ t('home.queueHint') }}</p>
            </div>
            <RouterLink class="text-link" to="/processing">{{ t('home.openQueue') }}</RouterLink>
          </header>

          <div v-if="queueItems.length" class="queue-list">
            <RouterLink
              v-for="item in queueItems.slice(0, 3)"
              :key="item.id"
              class="queue-row"
              :to="`/library/${item.id}`"
            >
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ sourceLabel(item.source.kind) }} · {{ statusLabel(item.processingStatus) }}</small>
              </span>
              <span class="mini-action">{{ t('processing.review') }}</span>
            </RouterLink>
          </div>
          <div v-else class="queue-empty">
            <Inbox :size="18" />
            <span>{{ t('processing.emptyTitle') }}</span>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import {
  ArrowRight,
  CircleAlert,
  FileText,
  Filter,
  Globe2,
  Image,
  Inbox,
  Layers,
  Link2,
  LoaderCircle,
  PenLine,
  Search,
  SquarePen,
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  ApiError,
  listKnowledgeItems,
  confirmBillingQuote,
  createBillingQuote,
  saveInternetCandidate,
  searchInternet,
  searchKnowledge,
} from '@/api/client'
import { knowledgeStatusTone, knowledgeStatusTranslationKey } from '@/lib/knowledge-status'
import { createIdempotencyKey } from '@/lib/id'
import type { ExternalSearchCandidate, KnowledgeItemSummary } from '@/types/api'
import type { BillingQuote } from '@/types/api'

const { t, locale } = useI18n()
const router = useRouter()
const query = ref('')
const searchedQuery = ref('')
const allItems = ref<KnowledgeItemSummary[]>([])
const items = ref<KnowledgeItemSummary[]>([])
const matchedFieldsByItem = ref<Record<string, string[]>>({})
const loading = ref(false)
const error = ref('')
const degraded = ref(false)
const externalVisible = ref(false)
const externalLoading = ref(false)
const externalError = ref('')
const externalItems = ref<ExternalSearchCandidate[]>([])
const candidateErrors = ref<Record<string, string>>({})
const savingCandidateId = ref('')
const externalQuote = ref<BillingQuote | null>(null)
const externalTaskId = ref('')
const candidateQuote = ref<BillingQuote | null>(null)
const candidateQuoteId = ref('')
const externalSearchKey = ref('')
const candidateRequestKeys = ref<Record<string, string>>({})

const displayedItems = computed(() => items.value.slice(0, 5))
const queueItems = computed(() =>
  allItems.value.filter((item) => isPending(item.processingStatus)),
)
const categoryCount = computed(
  () => new Set(allItems.value.map((item) => item.category?.name).filter(Boolean)).size,
)
const latestTime = computed(() => {
  if (!allItems.value.length) return '—'
  const latest = allItems.value.reduce((current, item) =>
    new Date(item.updatedAt) > new Date(current.updatedAt) ? item : current,
  )
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(latest.updatedAt))
})
const mapNodes = computed(() => {
  const counts = new Map<string, number>()
  for (const item of allItems.value) {
    const name = item.category?.name || t('common.uncategorized')
    counts.set(name, (counts.get(name) || 0) + 1)
  }
  const entries = [...counts.entries()].sort((left, right) => right[1] - left[1]).slice(0, 6)
  const positions = [
    { x: 20, y: 24, depth: 'surface' },
    { x: 65, y: 24, depth: 'middle' },
    { x: 40, y: 48, depth: 'middle' },
    { x: 79, y: 55, depth: 'deep' },
    { x: 26, y: 76, depth: 'deep' },
    { x: 64, y: 78, depth: 'deep' },
  ]
  return entries.map(([name, count], index) => ({ name, count, ...positions[index] }))
})

function isPending(status: string) {
  return ['pending', 'queued', 'running', 'processing', 'review', 'failed'].includes(status)
}

function sourceIcon(kind: string) {
  if (kind === 'webpage') return Link2
  if (kind === 'document') return FileText
  if (kind === 'screenshot') return Image
  if (kind === 'text') return FileText
  if (kind === 'internet_result') return Globe2
  return PenLine
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

function statusClass(status: string) {
  return knowledgeStatusTone(status)
}

function statusLabel(status: string) {
  return t(knowledgeStatusTranslationKey(status))
}

async function load() {
  loading.value = true
  error.value = ''
  degraded.value = false
  externalVisible.value = false
  externalLoading.value = false
  externalError.value = ''
  externalItems.value = []
  candidateErrors.value = {}
  externalQuote.value = null
  externalTaskId.value = ''
  candidateQuote.value = null
  candidateQuoteId.value = ''
  externalSearchKey.value = ''
  candidateRequestKeys.value = {}
  matchedFieldsByItem.value = {}
  const normalized = query.value.trim()
  try {
    if (normalized) {
      const response = await searchKnowledge(normalized)
      searchedQuery.value = normalized
      items.value = response.items.map((hit) => hit.item)
      matchedFieldsByItem.value = Object.fromEntries(
        response.items.map((hit) => [hit.item.id, hit.matchedFields]),
      )
      degraded.value = response.degraded
    } else {
      const response = await listKnowledgeItems(20)
      allItems.value = response.items
      items.value = response.items
      searchedQuery.value = ''
    }
  } catch (caught) {
    items.value = []
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

async function submitSearch() {
  await load()
}

async function searchCategory(name: string) {
  query.value = name
  await load()
}

async function searchExternalInternet() {
  if (!searchedQuery.value) return
  externalVisible.value = true
  externalError.value = ''
  externalLoading.value = true
  externalItems.value = []
  candidateErrors.value = {}
  try {
    if (!externalQuote.value) {
      externalQuote.value = await createBillingQuote({
        operation: 'internet_search',
        lines: [{ feature: 'internet_search', quantity: 1 }],
      })
    }
    if (!externalTaskId.value) {
      const task = await confirmBillingQuote(
        externalQuote.value.quoteId,
        externalQuote.value.maxPoints,
      )
      externalTaskId.value = task.id
    }
    const requestKey =
      externalSearchKey.value ||
      (externalSearchKey.value = createIdempotencyKey('internet-search'))
    const response = await searchInternet(
      searchedQuery.value,
      externalTaskId.value,
      false,
      requestKey,
    )
    externalQuote.value = null
    externalTaskId.value = ''
    externalSearchKey.value = ''
    externalItems.value = response.items
  } catch (caught) {
    externalError.value = billingMessage(caught)
  } finally {
    externalLoading.value = false
  }
}

async function saveCandidate(candidate: ExternalSearchCandidate) {
  savingCandidateId.value = candidate.candidateId
  const nextCandidateErrors = { ...candidateErrors.value }
  delete nextCandidateErrors[candidate.candidateId]
  candidateErrors.value = nextCandidateErrors
  try {
    if (!candidateQuote.value || candidateQuoteId.value !== candidate.candidateId) {
      candidateQuote.value = await createBillingQuote({
        operation: 'internet_import',
        lines: [{ feature: 'classify', quantity: 1 }],
      })
      candidateQuoteId.value = candidate.candidateId
      return
    }
    const task = await confirmBillingQuote(
      candidateQuote.value.quoteId,
      candidateQuote.value.maxPoints,
    )
    const requestKey =
      candidateRequestKeys.value[candidate.candidateId] ||
      (candidateRequestKeys.value[candidate.candidateId] = createIdempotencyKey('candidate-save'))
    const saved = await saveInternetCandidate(
      candidate.candidateId,
      task.id,
      requestKey,
    )
    candidateQuote.value = null
    candidateQuoteId.value = ''
    await router.push(
      saved.items.length === 1 ? `/library/${saved.items[0].id}` : '/library',
    )
  } catch (caught) {
    candidateErrors.value = {
      ...candidateErrors.value,
      [candidate.candidateId]: candidateSaveMessage(caught),
    }
    candidateQuote.value = null
    candidateQuoteId.value = ''
  } finally {
    savingCandidateId.value = ''
  }
}

function candidateSaveMessage(caught: unknown) {
  if (caught instanceof ApiError) {
    if (
      caught.code === 'EXTERNAL_SOURCE_UNAVAILABLE' ||
      caught.code === 'EXTERNAL_SEARCH_UNAVAILABLE'
    ) {
      return t('home.internetSourceUnavailable')
    }
    if (caught.code === 'WEB_PAGE_ORGANIZATION_FAILED') {
      return t('home.internetCandidateOrganizeFailed')
    }
  }
  return billingMessage(caught)
}

function billingMessage(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'INSUFFICIENT_POINTS') return t('home.insufficientPoints')
    if (caught.code === 'BILLING_DISABLED' || caught.code === 'BILLING_BRIDGE_UNAVAILABLE') {
      return t('home.billingUnavailable')
    }
  }
  return caught instanceof Error ? caught.message : t('errors.generic')
}

onMounted(load)
</script>
