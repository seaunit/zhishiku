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

    <form class="search-sheet" role="search" @submit.prevent="submitSearch">
      <label for="knowledge-search">{{ t('home.searchLabel') }}</label>
      <div class="search-control">
        <Search :size="21" aria-hidden="true" />
        <input
          id="knowledge-search"
          v-model="query"
          type="search"
          name="q"
          autocomplete="off"
          :placeholder="t('home.searchPlaceholder')"
        />
        <button class="button primary search-submit" type="submit" :disabled="loading">
          <LoaderCircle v-if="loading" class="spin" :size="18" />
          <span v-else>{{ t('home.searchButton') }}</span>
        </button>
      </div>
      <div class="search-meta">
        <span>{{ searchedQuery ? t('home.resultCount', { count: items.length }) : t('home.recentCount', { count: items.length }) }}</span>
        <span v-if="degraded" class="muted">{{ t('home.staleIndex') }}</span>
      </div>
    </form>

    <section class="results-panel" aria-labelledby="results-title">
      <header class="results-head">
        <h2 id="results-title">{{ searchedQuery ? t('home.searchButton') : t('home.recentTitle') }}</h2>
        <span v-if="items.length" class="count-mark">{{ items.length }}</span>
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
        <h3>{{ searchedQuery ? t('home.noResults') : t('home.emptyLibrary') }}</h3>
        <p>{{ searchedQuery ? t('home.noResultsHint') : t('home.emptyLibraryHint') }}</p>
        <RouterLink class="button secondary" to="/capture">
          <SquarePen :size="17" />
          <span>{{ t('nav.capture') }}</span>
        </RouterLink>
      </div>

      <ul v-else class="result-list" :aria-label="t('a11y.searchResults')">
        <li v-for="item in items" :key="item.id" class="result-row">
          <div class="result-main">
            <RouterLink class="result-title" :to="`/library/${item.id}`">
              {{ item.title || t('home.untitled') }}
            </RouterLink>
            <p>{{ item.summary || item.bodyPreview || '—' }}</p>
            <div class="result-meta">
              <span>{{ t('home.updated', { date: formatDate(item.updatedAt) }) }}</span>
              <span v-if="item.category" class="tag">{{ item.category.name }}</span>
              <span v-for="tag in item.tags.slice(0, 3)" :key="tag.id" class="tag">
                {{ tag.name }}
              </span>
            </div>
          </div>
          <RouterLink class="row-open" :to="`/library/${item.id}`" :aria-label="t('home.open')">
            <ArrowUpRight :size="18" />
          </RouterLink>
        </li>
      </ul>
    </section>
  </section>
</template>

<script setup lang="ts">
import {
  ArrowUpRight,
  CircleAlert,
  LoaderCircle,
  Search,
  SquarePen,
} from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listKnowledgeItems, searchKnowledge } from '@/api/client'
import type { KnowledgeItemSummary } from '@/types/api'

const { t, locale } = useI18n()
const query = ref('')
const searchedQuery = ref('')
const items = ref<KnowledgeItemSummary[]>([])
const loading = ref(false)
const error = ref('')
const degraded = ref(false)

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date)
}

async function load() {
  loading.value = true
  error.value = ''
  degraded.value = false
  const normalized = query.value.trim()

  try {
    if (normalized) {
      const response = await searchKnowledge(normalized)
      searchedQuery.value = normalized
      items.value = response.items.map((hit) => hit.item)
      degraded.value = response.degraded
    } else {
      const response = await listKnowledgeItems(20)
      searchedQuery.value = ''
      items.value = response.items
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

onMounted(load)
</script>

