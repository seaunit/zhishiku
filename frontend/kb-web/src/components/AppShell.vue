<template>
  <div class="app-shell">
    <aside class="rail" :aria-label="t('a11y.appSidebar')">
      <RouterLink class="rail-brand" to="/" :aria-label="t('nav.home')">
        <BrandMark compact />
      </RouterLink>

      <nav class="rail-nav" :aria-label="t('a11y.mainNavigation')">
        <div class="rail-main">
          <RouterLink
            v-for="item in primaryItems"
            :key="item.name"
            class="rail-item"
            :to="item.to"
            :exact-active-class="item.name === 'home' ? 'is-active' : undefined"
            :active-class="item.name === 'home' ? undefined : 'is-active'"
            :aria-label="item.label"
            :title="item.label"
          >
            <component :is="item.icon" :size="18" :stroke-width="1.6" />
            <span>{{ item.label }}</span>
            <b
              v-if="item.name === 'processing' && pendingCount > 0"
              class="nav-count"
              :aria-label="t('nav.pendingCount', { count: pendingCount })"
            >
              {{ pendingCount }}
            </b>
          </RouterLink>
        </div>

        <div class="rail-bottom">
          <RouterLink
            class="rail-item"
            to="/settings"
            active-class="is-active"
            :aria-label="t('nav.settings')"
            :title="t('nav.settings')"
          >
            <Settings :size="18" :stroke-width="1.6" />
            <span>{{ t('nav.settings') }}</span>
          </RouterLink>
        </div>
      </nav>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div class="scope-line">
          <BrandMark compact />
          <div>
            <p class="scope-label">{{ t('common.scopeLabel') }}</p>
            <p class="scope-name">{{ t('common.appName') }}</p>
          </div>
        </div>

        <div class="topbar-actions">
          <span class="sync-state"><i aria-hidden="true" />{{ t('common.synced') }}</span>
          <LocaleSwitch />
          <AccountMenu
            :display-name="accountDisplayName"
            :email="session.session?.email || ''"
            :initial="accountInitial"
            @request-logout="openLogout"
          />
        </div>
      </header>

      <main class="app-main">
        <RouterView />
      </main>
    </div>

    <ConfirmDialog
      v-if="confirmOpen"
      :title="t('account.logoutTitle')"
      :body="t('account.logoutBody')"
      :cancel-label="t('common.cancel')"
      :confirm-label="signingOut ? t('account.signingOut') : t('account.confirmLogout')"
      :busy="signingOut"
      :error="signOutError"
      @cancel="cancelLogout"
      @confirm="signOut"
    />
  </div>
</template>

<script setup lang="ts">
import { CreditCard, Home, Inbox, Library, Plus, Settings, Trash2 } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { listKnowledgeItems } from '@/api/client'
import {
  PENDING_PROCESSING_STATUS_QUERY,
  REVIEW_QUEUE_CHANGED_EVENT,
} from '@/lib/knowledge-status'
import { useSessionStore } from '@/stores/session'
import AccountMenu from './AccountMenu.vue'
import BrandMark from './BrandMark.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import LocaleSwitch from './LocaleSwitch.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const pendingCount = ref(0)
const confirmOpen = ref(false)
const signingOut = ref(false)
const signOutError = ref('')
let pendingCountRequest = 0

const primaryItems = computed(() => [
  { name: 'home', to: '/', label: t('nav.home'), icon: Home },
  { name: 'library', to: '/library', label: t('nav.library'), icon: Library },
  { name: 'capture', to: '/capture', label: t('nav.capture'), icon: Plus },
  { name: 'processing', to: '/processing', label: t('nav.processing'), icon: Inbox },
  { name: 'billing', to: '/billing', label: t('nav.billing'), icon: CreditCard },
  { name: 'trash', to: '/trash', label: t('nav.trash'), icon: Trash2 },
])

const accountDisplayName = computed(
  () => session.session?.displayName || session.session?.email.split('@')[0] || t('account.defaultName'),
)
const accountInitial = computed(() => firstCharacter(accountDisplayName.value))

async function signOut() {
  if (signingOut.value) {
    return
  }
  signingOut.value = true
  signOutError.value = ''
  try {
    await session.logout()
    confirmOpen.value = false
    await router.push({ name: 'login' })
  } catch {
    signOutError.value = t('account.logoutFailed')
  } finally {
    signingOut.value = false
  }
}

function openLogout() {
  signOutError.value = ''
  confirmOpen.value = true
}

function cancelLogout() {
  confirmOpen.value = false
  signOutError.value = ''
}

function firstCharacter(value: string) {
  return Array.from(value.trim())[0]?.toUpperCase() || '识'
}

async function refreshPendingCount() {
  const requestId = ++pendingCountRequest
  try {
    const response = await listKnowledgeItems(
      1,
      1,
      'all',
      PENDING_PROCESSING_STATUS_QUERY,
    )
    if (requestId === pendingCountRequest) {
      pendingCount.value = response.pageInfo?.totalItems ?? 0
    }
  } catch {
    // Keep the last known count during a transient refresh failure.
  }
}

function handleReviewQueueChanged() {
  void refreshPendingCount()
}

watch(
  () => route.fullPath,
  () => {
    if (session.session) {
      void refreshPendingCount()
    }
  },
)

onMounted(async () => {
  window.addEventListener(REVIEW_QUEUE_CHANGED_EVENT, handleReviewQueueChanged)
  try {
    await session.restore()
    await refreshPendingCount()
  } catch {
    // The API client emits the unauthorized event and redirects to login.
  }
})

onBeforeUnmount(() => {
  window.removeEventListener(REVIEW_QUEUE_CHANGED_EVENT, handleReviewQueueChanged)
})
</script>
