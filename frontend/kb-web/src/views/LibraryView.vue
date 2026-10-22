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

    <form class="search-shell library-search-shell" role="search" @submit.prevent="submitSearch">
      <div class="search-field">
        <Search :size="19" aria-hidden="true" />
        <input
          v-model="searchQuery"
          type="search"
          maxlength="300"
          autocomplete="off"
          :aria-label="t('library.searchLabel')"
          :placeholder="t('library.searchPlaceholder')"
        />
        <button
          v-if="searchQuery || searchedQuery"
          type="button"
          class="search-clear"
          :aria-label="t('library.clearSearch')"
          @click="clearSearch"
        >
          <X :size="15" />
        </button>
      </div>
      <button class="button primary library-search-submit" type="submit">
        <LoaderCircle v-if="loading" class="spin" :size="17" />
        <Search v-else :size="17" />
        <span>{{ t('library.searchAction') }}</span>
      </button>
    </form>

    <div class="view-toolbar">
      <div class="filter-row" role="group" :aria-label="t('library.source')">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          class="filter-chip"
          :class="{ 'is-active': activeFilter === filter.value }"
          @click="setFilter(filter.value)"
        >
          {{ t(filter.label) }}
          <span class="num">{{ countFor(filter.value) }}</span>
        </button>
      </div>
      <div class="library-toolbar-meta">
        <button
          type="button"
          class="filter-chip mobile-select-all"
            :disabled="items.length === 0"
          @click="toggleAllVisible"
        >
          {{ allVisibleSelected ? t('library.clearSelection') : t('library.selectAllVisible') }}
        </button>
        <span class="freshness">{{ t('library.freshness', { time: freshness }) }}</span>
      </div>
    </div>

    <div v-if="selectedIds.size" class="bulk-actions" role="status" aria-live="polite">
      <span>{{ t('library.selectedCount', { count: selectedIds.size }) }}</span>
      <button type="button" class="button secondary danger" @click="requestBulkDelete">
        <Trash2 :size="16" />
        <span>{{ t('library.bulkDelete') }}</span>
      </button>
    </div>

    <p v-if="notice" class="billing-notice" role="status">{{ notice }}</p>

    <div v-if="error && items.length" class="inline-error library-load-error" role="alert">
      <CircleAlert :size="20" />
      <div>
        <strong>{{ t('home.loadFailed') }}</strong>
        <p>{{ error }}</p>
      </div>
      <button type="button" class="text-button" @click="load">{{ t('common.retry') }}</button>
    </div>

    <section class="panel library-panel" :aria-busy="loading">
      <div v-if="loading" class="library-skeleton" :aria-label="t('a11y.loading')">
        <span v-for="index in 6" :key="index" class="skeleton" />
      </div>

      <div v-else-if="error && !items.length" class="inline-error" role="alert">
        <CircleAlert :size="20" />
        <div>
          <strong>{{ t('home.loadFailed') }}</strong>
          <p>{{ error }}</p>
        </div>
        <button type="button" class="text-button" @click="load">{{ t('common.retry') }}</button>
      </div>

      <div v-else-if="items.length === 0" class="empty-state">
        <BookOpen :size="22" />
        <h3>{{ searchedQuery ? t('library.noResultsTitle') : t('library.emptyTitle') }}</h3>
        <p>{{ searchedQuery ? t('library.noResultsBody') : t('library.emptyBody') }}</p>
        <button
          v-if="searchedQuery"
          type="button"
          class="button secondary"
          @click="clearSearch"
        >
          <X :size="16" />
          <span>{{ t('library.clearSearch') }}</span>
        </button>
      </div>

      <div v-else class="knowledge-table">
        <div class="knowledge-table-head">
          <label class="row-checkbox">
            <input
              type="checkbox"
              :checked="allVisibleSelected"
              :indeterminate="someVisibleSelected && !allVisibleSelected"
              :aria-label="t('library.selectVisible')"
              @change="toggleVisibleSelection"
            />
          </label>
          <div class="knowledge-head-grid" aria-hidden="true">
            <span>{{ t('library.knowledge') }}</span>
            <span>{{ t('library.category') }}</span>
            <span>{{ t('library.updated') }}</span>
            <span>{{ t('library.status') }}</span>
          </div>
          <span class="row-action-head" aria-hidden="true" />
        </div>
        <div
          v-for="item in items"
          :key="item.id"
          class="knowledge-row"
          :class="{ 'is-selected': selectedIds.has(item.id) }"
        >
          <label class="row-checkbox">
            <input
              type="checkbox"
              :checked="selectedIds.has(item.id)"
              :aria-label="t('library.selectItem', { title: item.title })"
              @change="toggleSelection(item.id, ($event.target as HTMLInputElement).checked)"
            />
          </label>
          <RouterLink class="knowledge-row-link" :to="`/library/${item.id}`">
            <span class="knowledge-main">
              <span class="source-icon">
                <component :is="sourceIcon(item.source.kind)" :size="15" />
              </span>
              <span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.summary || item.bodyPreview || '—' }}</small>
                <span v-if="matchedFieldsByItem[item.id]?.length" class="match-fields">
                  {{
                    t('library.matchedFields', {
                      fields: matchedFieldsByItem[item.id]
                        .map((field) => t(`home.match.${field}`))
                        .join(t('common.listSeparator')),
                    })
                  }}
                </span>
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
          <button
            type="button"
            class="row-delete"
            :aria-label="t('library.deleteItem', { title: item.title })"
            @click="requestDelete(item)"
          >
            <Trash2 :size="16" />
          </button>
        </div>
      </div>

      <footer
        v-if="!loading && pageInfo.totalItems > 0"
        class="library-pagination"
        :aria-label="t('library.pagination')"
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
          <label class="pagination-size">
            <span>{{ t('library.perPage') }}</span>
            <select v-model.number="pageSize" @change="changePageSize">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
          <button
            type="button"
            class="pagination-button"
            :aria-label="t('library.previousPage')"
            :disabled="pageInfo.page <= 1"
            @click="goToPage(pageInfo.page - 1)"
          >
            <ChevronLeft :size="16" />
          </button>
          <template v-for="pageNumber in visiblePageNumbers" :key="pageNumber">
            <span v-if="pageNumber === 'ellipsis'" class="pagination-ellipsis">…</span>
            <button
              v-else
              type="button"
              class="pagination-button num"
              :class="{ 'is-active': pageNumber === pageInfo.page }"
              :aria-label="t('library.pageNumber', { page: pageNumber })"
              :aria-current="pageNumber === pageInfo.page ? 'page' : undefined"
              @click="goToPage(pageNumber)"
            >
              {{ pageNumber }}
            </button>
          </template>
          <button
            type="button"
            class="pagination-button"
            :aria-label="t('library.nextPage')"
            :disabled="pageInfo.page >= pageInfo.totalPages"
            @click="goToPage(pageInfo.page + 1)"
          >
            <ChevronRight :size="16" />
          </button>
        </div>
      </footer>
    </section>

    <ConfirmDialog
      v-if="deleteTargets.length"
      :title="deleteDialogTitle"
      :body="deleteDialogBody"
      :cancel-label="t('common.cancel')"
      :confirm-label="t('library.confirmDelete')"
      :busy="deleting"
      :error="actionError"
      @cancel="closeDeleteDialog"
      @confirm="confirmDelete"
    />
  </section>
</template>

<script setup lang="ts">
import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  FileText,
  Globe2,
  Image,
  Link2,
  LoaderCircle,
  PenLine,
  Plus,
  Search,
  Trash2,
  X,
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  bulkRecycleKnowledgeItems,
  listKnowledgeItems,
  recycleKnowledgeItem,
  searchKnowledge,
} from '@/api/client'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { knowledgeStatusTone, knowledgeStatusTranslationKey } from '@/lib/knowledge-status'
import { createIdempotencyKey } from '@/lib/id'
import type { KnowledgeItemSummary, KnowledgePageInfo } from '@/types/api'

const { t, locale } = useI18n()
const items = ref<KnowledgeItemSummary[]>([])
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const notice = ref('')
const activeFilter = ref('all')
const searchQuery = ref('')
const searchedQuery = ref('')
const matchedFieldsByItem = ref<Record<string, string[]>>({})
const selectedIds = ref(new Set<string>())
const deleteTargets = ref<KnowledgeItemSummary[]>([])
const deleting = ref(false)
const bulkDeleteKey = ref('')
const page = ref(1)
const pageSize = ref(10)
const pageInfo = ref<KnowledgePageInfo>({
  page: 1,
  pageSize: 10,
  totalItems: 0,
  totalPages: 1,
  sourceCounts: {},
})
let loadSequence = 0

const filters = [
  { value: 'all', label: 'library.all' },
  { value: 'webpage', label: 'library.webpage' },
  { value: 'document', label: 'library.document' },
  { value: 'screenshot', label: 'library.screenshot' },
  { value: 'text', label: 'library.text' },
  { value: 'manual', label: 'library.manual' },
]

const allVisibleSelected = computed(
  () =>
    items.value.length > 0 &&
    items.value.every((item) => selectedIds.value.has(item.id)),
)
const someVisibleSelected = computed(() =>
  items.value.some((item) => selectedIds.value.has(item.id)),
)
const visiblePageNumbers = computed<(number | 'ellipsis')[]>(() => {
  const total = pageInfo.value.totalPages
  const current = pageInfo.value.page
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1)
  const pages: (number | 'ellipsis')[] = [1]
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  if (start > 2) pages.push('ellipsis')
  for (let value = start; value <= end; value += 1) pages.push(value)
  if (end < total - 1) pages.push('ellipsis')
  pages.push(total)
  return pages
})
const deleteDialogTitle = computed(() =>
  deleteTargets.value.length > 1
    ? t('library.bulkDeleteTitle')
    : t('library.deleteTitle'),
)
const deleteDialogBody = computed(() => {
  if (deleteTargets.value.length > 1) {
    const titles = deleteTargets.value
      .slice(0, 3)
      .map((item) => item.title)
      .join(t('common.listSeparator'))
    return t('library.bulkDeleteBody', {
      count: deleteTargets.value.length,
      titles,
    })
  }
  return t('library.deleteBody', { title: deleteTargets.value[0]?.title || '' })
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
  return pageInfo.value.sourceCounts[filter] ?? 0
}

function sourceIcon(kind: string) {
  if (kind === 'webpage') return Link2
  if (kind === 'document') return FileText
  if (kind === 'screenshot') return Image
  if (kind === 'internet_result') return Globe2
  if (kind === 'text') return FileText
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

function clearSelection() {
  selectedIds.value = new Set()
}

async function setFilter(filter: string) {
  if (activeFilter.value === filter) return
  activeFilter.value = filter
  page.value = 1
  clearSelection()
  await load()
}

function toggleSelection(id: string, checked: boolean) {
  const next = new Set(selectedIds.value)
  if (checked) next.add(id)
  else next.delete(id)
  selectedIds.value = next
}

function toggleVisibleSelection(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  setVisibleSelection(checked)
}

function toggleAllVisible() {
  setVisibleSelection(!allVisibleSelected.value)
}

function setVisibleSelection(checked: boolean) {
  const next = new Set(selectedIds.value)
  for (const item of items.value) {
    if (checked) next.add(item.id)
    else next.delete(item.id)
  }
  selectedIds.value = next
}

async function load() {
  const requestId = ++loadSequence
  loading.value = true
  error.value = ''
  notice.value = ''
  const normalized = searchQuery.value.trim()
  try {
    if (normalized) {
      const response = await searchKnowledge(normalized, pageSize.value, page.value, activeFilter.value)
      if (requestId !== loadSequence) return
      items.value = response.items.map((hit) => hit.item)
      matchedFieldsByItem.value = Object.fromEntries(
        response.items.map((hit) => [hit.item.id, hit.matchedFields]),
      )
      searchedQuery.value = normalized
      applyPageInfo(response.pageInfo)
    } else {
      const response = await listKnowledgeItems(pageSize.value, page.value, activeFilter.value)
      if (requestId !== loadSequence) return
      items.value = response.items
      matchedFieldsByItem.value = {}
      searchedQuery.value = ''
      applyPageInfo(response.pageInfo)
    }
    clearSelection()
  } catch (caught) {
    if (requestId !== loadSequence) return
    error.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    if (requestId === loadSequence) {
      loading.value = false
    }
  }
}

async function submitSearch() {
  page.value = 1
  await load()
}

async function clearSearch() {
  searchQuery.value = ''
  searchedQuery.value = ''
  page.value = 1
  await load()
}

function applyPageInfo(next?: KnowledgePageInfo) {
  if (next) {
    pageInfo.value = next
    page.value = next.page
    pageSize.value = next.pageSize
    return
  }
  pageInfo.value = {
    page: 1,
    pageSize: pageSize.value,
    totalItems: items.value.length,
    totalPages: 1,
    sourceCounts: {
      all: items.value.length,
      webpage: items.value.filter((item) => item.source.kind === 'webpage').length,
      document: items.value.filter((item) => item.source.kind === 'document').length,
      screenshot: items.value.filter((item) => item.source.kind === 'screenshot').length,
      text: items.value.filter((item) => item.source.kind === 'text').length,
      manual: items.value.filter((item) => item.source.kind === 'manual').length,
    },
  }
}

async function goToPage(nextPage: number) {
  if (nextPage < 1 || nextPage > pageInfo.value.totalPages || nextPage === pageInfo.value.page) {
    return
  }
  page.value = nextPage
  clearSelection()
  await load()
  document.querySelector('.library-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function changePageSize() {
  page.value = 1
  clearSelection()
  await load()
}

function requestDelete(item: KnowledgeItemSummary) {
  actionError.value = ''
  deleteTargets.value = [item]
}

function requestBulkDelete() {
  const selected = items.value.filter((item) => selectedIds.value.has(item.id))
  if (!selected.length) return
  actionError.value = ''
  deleteTargets.value = selected.slice(0, 100)
  bulkDeleteKey.value = createIdempotencyKey('bulk-recycle')
}

async function confirmDelete() {
  if (!deleteTargets.value.length || deleting.value) return
  deleting.value = true
  actionError.value = ''
  const targets = [...deleteTargets.value]
  try {
    if (targets.length === 1) {
      await recycleKnowledgeItem(targets[0].id)
    } else {
      await bulkRecycleKnowledgeItems(
        targets.map((item) => item.id),
        bulkDeleteKey.value,
      )
    }
    clearSelection()
    deleteTargets.value = []
    bulkDeleteKey.value = ''
    const message =
      targets.length === 1
        ? t('library.deletedOne')
        : t('library.deletedMany', { count: targets.length })
    await load()
    notice.value = message
  } catch {
    actionError.value =
      targets.length === 1
        ? t('library.deleteFailed')
        : t('library.bulkDeleteFailed')
  } finally {
    deleting.value = false
  }
}

function closeDeleteDialog() {
  if (deleting.value) return
  deleteTargets.value = []
  actionError.value = ''
  bulkDeleteKey.value = ''
}

onMounted(load)
</script>
