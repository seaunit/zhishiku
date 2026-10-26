<template>
  <section class="page admin-page">
    <header class="page-heading admin-page-heading">
      <div>
        <h1>{{ t('admin.title') }}</h1>
        <p>{{ t('admin.subtitle') }}</p>
      </div>
      <button class="button secondary" type="button" :disabled="loading" @click="load()">
        <RefreshCw :class="{ spin: loading }" :size="16" />
        <span>{{ t('admin.refresh') }}</span>
      </button>
    </header>

    <section class="admin-summary" :aria-label="t('admin.title')">
      <div>
        <span>{{ t('admin.allAccounts') }}</span>
        <strong class="num">{{ formatNumber(summary.total) }}</strong>
      </div>
      <div>
        <span>{{ t('admin.enabledAccounts') }}</span>
        <strong class="num">{{ formatNumber(summary.enabled) }}</strong>
      </div>
      <div>
        <span>{{ t('admin.disabledAccounts') }}</span>
        <strong class="num">{{ formatNumber(summary.disabled) }}</strong>
      </div>
      <div>
        <span>{{ t('admin.totalConsumed') }}</span>
        <strong class="num">{{ formatNumber(summary.consumed) }}</strong>
      </div>
    </section>

    <section class="admin-sheet" aria-labelledby="admin-account-list-title">
      <header class="admin-sheet-head">
        <div>
          <h2 id="admin-account-list-title">{{ t('admin.accountList') }}</h2>
          <p>{{ listSummary }}</p>
        </div>
      </header>

      <div class="admin-toolbar">
        <form class="admin-search" role="search" @submit.prevent="applySearch">
          <Search :size="16" aria-hidden="true" />
          <input
            v-model="query"
            type="search"
            autocomplete="off"
            :aria-label="t('admin.searchLabel')"
            :placeholder="t('admin.searchPlaceholder')"
            @input="scheduleSearch"
          />
        </form>
        <div class="admin-filter-tabs" role="tablist" :aria-label="t('admin.statusFilter')">
          <button
            v-for="filter in statusFilters"
            :key="filter"
            type="button"
            role="tab"
            :aria-selected="statusFilter === filter"
            :class="{ 'is-active': statusFilter === filter }"
            @click="selectStatus(filter)"
          >
            {{ statusLabel(filter) }}
          </button>
        </div>
      </div>

      <div class="admin-table-head" aria-hidden="true">
        <span>{{ t('admin.account') }}</span>
        <span>{{ t('admin.status') }}</span>
        <span>{{ t('admin.available') }}</span>
        <span>{{ t('admin.reserved') }}</span>
        <span>{{ t('admin.consumed') }}</span>
        <span>{{ t('admin.actions') }}</span>
      </div>

      <div v-if="loading && users.length === 0" class="admin-account-list" aria-busy="true">
        <div v-for="index in 5" :key="index" class="admin-row-skeleton">
          <span />
          <span />
          <span />
        </div>
      </div>

      <div v-else-if="error" class="admin-state admin-error" role="alert">
        <CircleAlert :size="22" />
        <strong>{{ t('admin.loadFailed') }}</strong>
        <span>{{ error }}</span>
        <button class="button secondary" type="button" @click="load()">
          {{ t('common.retry') }}
        </button>
      </div>

      <div v-else-if="users.length === 0" class="admin-state">
        <Filter :size="22" />
        <strong>{{ t('admin.emptyTitle') }}</strong>
        <span>{{ t('admin.emptyBody') }}</span>
      </div>

      <div v-else class="admin-account-list">
        <article
          v-for="(user, index) in users"
          :key="user.ownerId"
          class="admin-account-row"
          :data-status="user.status"
        >
          <div class="admin-identity">
            <span class="admin-avatar" :class="avatarTone(user, index)">
              {{ firstCharacter(user.displayName || user.loginName) }}
            </span>
            <span>
              <strong>{{ user.displayName }}</strong>
              <small>
                {{ user.email }} · {{ formatDate(user.createdAt) }} ·
                {{ t('admin.knowledgeCount', { count: user.knowledgeCount }) }}
              </small>
            </span>
          </div>
          <span class="admin-status" :class="`is-${user.status}`">
            <i aria-hidden="true" />
            {{ user.status === 'enabled' ? t('admin.enabled') : t('admin.disabled') }}
          </span>
          <span class="admin-number">
            <strong>{{ formatNumber(user.availablePoints) }}</strong>
            <small>{{ t('admin.points') }}</small>
          </span>
          <span class="admin-number">
            <strong>{{ formatNumber(user.reservedPoints) }}</strong>
            <small>{{ t('admin.points') }}</small>
          </span>
          <span class="admin-number">
            <strong>{{ formatNumber(user.lifetimeConsumed) }}</strong>
            <small>{{ t('admin.points') }}</small>
          </span>
          <span v-if="user.role === 'ADMIN'" class="admin-role-tag">
            {{ t('admin.administrator') }}
          </span>
          <span v-else class="admin-actions">
            <button type="button" @click="openStatus(user)">
              {{ user.status === 'enabled' ? t('admin.disable') : t('admin.enable') }}
            </button>
            <button type="button" @click="openGift(user)">
              {{ t('admin.gift') }}
            </button>
          </span>
        </article>
      </div>

      <footer class="admin-pagination" :aria-label="t('admin.pagination')">
        <span>{{ t('admin.perPage', { limit: pageSize }) }}</span>
        <div>
          <button type="button" :disabled="page <= 1 || loading" @click="load(page - 1)">
            {{ t('admin.previousPage') }}
          </button>
          <strong class="num">{{ page }}</strong>
          <button
            type="button"
            :disabled="page >= Math.max(totalPages, 1) || loading"
            @click="load(page + 1)"
          >
            {{ t('admin.nextPage') }}
          </button>
        </div>
      </footer>
    </section>

    <p v-if="notice" class="admin-notice" role="status">{{ notice }}</p>

    <dialog ref="statusDialog" class="admin-dialog" :aria-labelledby="statusTitleId">
      <form @submit.prevent="submitStatus">
        <header>
          <div>
            <h2 :id="statusTitleId">
              {{
                selectedUser?.status === 'enabled'
                  ? t('admin.statusTitleDisable')
                  : t('admin.statusTitleEnable')
              }}
            </h2>
            <p v-if="selectedUser">{{ targetLabel(selectedUser) }}</p>
          </div>
          <button
            class="icon-button"
            type="button"
            :aria-label="t('admin.closeDialog')"
            @click="closeStatus"
          >
            <X :size="18" />
          </button>
        </header>
        <p class="admin-dialog-note">
          {{
            selectedUser?.status === 'enabled'
              ? t('admin.statusBodyDisable')
              : t('admin.statusBodyEnable')
          }}
        </p>
        <label class="field">
          <span>{{ t('admin.reason') }}</span>
          <textarea
            v-model="statusReason"
            class="input"
            rows="3"
            maxlength="300"
            :placeholder="t('admin.reasonPlaceholder')"
            required
          />
        </label>
        <p v-if="dialogError" class="form-error" role="alert">{{ dialogError }}</p>
        <div class="admin-dialog-actions">
          <button class="button secondary" type="button" @click="closeStatus">
            {{ t('common.cancel') }}
          </button>
          <button class="button primary" type="submit" :disabled="actionSubmitting">
            <LoaderCircle v-if="actionSubmitting" class="spin" :size="16" />
            {{
              selectedUser?.status === 'enabled'
                ? t('admin.confirmDisable')
                : t('admin.confirmEnable')
            }}
          </button>
        </div>
      </form>
    </dialog>

    <dialog ref="giftDialog" class="admin-dialog" :aria-labelledby="giftTitleId">
      <form @submit.prevent="submitGift">
        <header>
          <div>
            <h2 :id="giftTitleId">{{ t('admin.giftTitle') }}</h2>
            <p v-if="selectedUser">{{ targetLabel(selectedUser) }}</p>
          </div>
          <button
            class="icon-button"
            type="button"
            :aria-label="t('admin.closeDialog')"
            @click="closeGift"
          >
            <X :size="18" />
          </button>
        </header>
        <label class="field">
          <span>{{ t('admin.giftPoints') }}</span>
          <input
            v-model.number="giftPoints"
            class="input num"
            type="number"
            min="1"
            max="100000"
            inputmode="numeric"
            :placeholder="t('admin.giftPointsPlaceholder')"
            required
          />
        </label>
        <label class="field">
          <span>{{ t('admin.reason') }}</span>
          <textarea
            v-model="giftReason"
            class="input"
            rows="3"
            maxlength="300"
            :placeholder="t('admin.giftReasonPlaceholder')"
            required
          />
        </label>
        <p class="admin-dialog-note">{{ t('admin.giftNote') }}</p>
        <p v-if="dialogError" class="form-error" role="alert">{{ dialogError }}</p>
        <div class="admin-dialog-actions">
          <button class="button secondary" type="button" @click="closeGift">
            {{ t('common.cancel') }}
          </button>
          <button class="button primary" type="submit" :disabled="actionSubmitting">
            <LoaderCircle v-if="actionSubmitting" class="spin" :size="16" />
            {{ t('admin.confirmGift') }}
          </button>
        </div>
      </form>
    </dialog>
  </section>
</template>

<script setup lang="ts">
import {
  CircleAlert,
  Filter,
  LoaderCircle,
  RefreshCw,
  Search,
  X,
} from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ApiError, grantAdminUserPoints, listAdminUsers, updateAdminUserStatus } from '@/api/client'
import { createIdempotencyKey } from '@/lib/id'
import type { AdminUser } from '@/types/api'

type StatusFilter = 'all' | 'enabled' | 'disabled'

const { t, locale } = useI18n()
const statusFilters: StatusFilter[] = ['all', 'enabled', 'disabled']
const users = ref<AdminUser[]>([])
const query = ref('')
const appliedQuery = ref('')
const statusFilter = ref<StatusFilter>('all')
const page = ref(1)
const pageSize = 20
const totalItems = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')
const notice = ref('')
const summary = reactive({ total: 0, enabled: 0, disabled: 0, consumed: 0 })
const selectedUser = ref<AdminUser | null>(null)
const statusReason = ref('')
const giftPoints = ref<number | null>(null)
const giftReason = ref('')
const dialogError = ref('')
const actionSubmitting = ref(false)
const statusDialog = ref<HTMLDialogElement>()
const giftDialog = ref<HTMLDialogElement>()
const statusTitleId = 'admin-status-dialog-title'
const giftTitleId = 'admin-gift-dialog-title'
let pendingStatusKey = ''
let pendingGiftKey = ''
let searchTimer: ReturnType<typeof setTimeout> | undefined
let noticeTimer: ReturnType<typeof setTimeout> | undefined

const listSummary = computed(() => {
  if (appliedQuery.value || statusFilter.value !== 'all') {
    return t('admin.matchedSummary', { count: totalItems.value })
  }
  return t('admin.pageSummary', {
    page: page.value,
    pages: Math.max(totalPages.value, 1),
    total: totalItems.value,
  })
})

async function load(requestedPage = page.value) {
  loading.value = true
  error.value = ''
  try {
    const response = await listAdminUsers(
      appliedQuery.value,
      requestedPage,
      pageSize,
      statusFilter.value,
    )
    users.value = response.items
    page.value = response.page
    totalItems.value = response.totalItems
    totalPages.value = response.totalPages
    summary.total = response.enabledCount + response.disabledCount
    summary.enabled = response.enabledCount
    summary.disabled = response.disabledCount
    summary.consumed = response.totalConsumed
  } catch (caught) {
    error.value = errorMessage(caught, t('admin.loadFailed'))
  } finally {
    loading.value = false
  }
}

function scheduleSearch() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(applySearch, 250)
}

function applySearch() {
  appliedQuery.value = query.value.trim()
  void load(1)
}

function selectStatus(filter: StatusFilter) {
  if (statusFilter.value === filter) {
    return
  }
  statusFilter.value = filter
  void load(1)
}

function statusLabel(filter: StatusFilter) {
  if (filter === 'enabled') return t('admin.enabled')
  if (filter === 'disabled') return t('admin.disabled')
  return t('admin.all')
}

function openStatus(user: AdminUser) {
  selectedUser.value = user
  statusReason.value = ''
  dialogError.value = ''
  pendingStatusKey = createIdempotencyKey('admin-status')
  statusDialog.value?.showModal()
}

function closeStatus() {
  if (actionSubmitting.value) {
    return
  }
  statusDialog.value?.close()
  selectedUser.value = null
  dialogError.value = ''
  pendingStatusKey = ''
}

async function submitStatus() {
  if (!selectedUser.value) {
    return
  }
  if (!statusReason.value.trim()) {
    dialogError.value = t('admin.reasonRequired')
    return
  }
  actionSubmitting.value = true
  dialogError.value = ''
  try {
    const enabled = selectedUser.value.status !== 'enabled'
    await updateAdminUserStatus(
      selectedUser.value.ownerId,
      enabled,
      statusReason.value.trim(),
      pendingStatusKey,
    )
    statusDialog.value?.close()
    selectedUser.value = null
    pendingStatusKey = ''
    showNotice(t('admin.statusUpdated'))
    await load()
  } catch (caught) {
    dialogError.value = errorMessage(caught, t('admin.statusFailed'))
  } finally {
    actionSubmitting.value = false
  }
}

function openGift(user: AdminUser) {
  selectedUser.value = user
  giftPoints.value = null
  giftReason.value = ''
  dialogError.value = ''
  pendingGiftKey = createIdempotencyKey('admin-points')
  giftDialog.value?.showModal()
}

function closeGift() {
  if (actionSubmitting.value) {
    return
  }
  giftDialog.value?.close()
  selectedUser.value = null
  dialogError.value = ''
  pendingGiftKey = ''
}

async function submitGift() {
  if (!selectedUser.value) {
    return
  }
  const points = Number(giftPoints.value)
  if (!Number.isInteger(points) || points < 1 || points > 100000) {
    dialogError.value = t('admin.giftRangeRequired')
    return
  }
  if (!giftReason.value.trim()) {
    dialogError.value = t('admin.giftReasonRequired')
    return
  }
  actionSubmitting.value = true
  dialogError.value = ''
  try {
    await grantAdminUserPoints(
      selectedUser.value.ownerId,
      points,
      giftReason.value.trim(),
      pendingGiftKey,
    )
    giftDialog.value?.close()
    selectedUser.value = null
    pendingGiftKey = ''
    showNotice(t('admin.giftSuccess', { points: formatNumber(points) }))
    await load()
  } catch (caught) {
    dialogError.value = errorMessage(caught, t('admin.giftFailed'))
  } finally {
    actionSubmitting.value = false
  }
}

function showNotice(message: string) {
  notice.value = message
  if (noticeTimer) {
    clearTimeout(noticeTimer)
  }
  noticeTimer = setTimeout(() => {
    notice.value = ''
  }, 3000)
}

function errorMessage(caught: unknown, fallback: string) {
  return caught instanceof ApiError && caught.message ? caught.message : fallback
}

function targetLabel(user: AdminUser) {
  return t('admin.targetAccount', {
    name: user.displayName || user.loginName,
    email: user.email,
  })
}

function firstCharacter(value: string) {
  return Array.from(value.trim())[0]?.toUpperCase() || '识'
}

function avatarTone(user: AdminUser, index: number) {
  if (user.role === 'ADMIN') return 'is-admin'
  if (user.status === 'disabled') return 'is-muted'
  return ['is-teal', 'is-warm', 'is-cool'][index % 3]
}

function formatNumber(value: number) {
  return new Intl.NumberFormat(locale.value).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

onMounted(() => {
  void load(1)
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (noticeTimer) clearTimeout(noticeTimer)
})
</script>
