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
      <div class="ocean-chart-backdrop" aria-hidden="true">
        <svg viewBox="0 0 1600 1100" preserveAspectRatio="xMidYMid slice">
          <g class="ocean-contours ocean-contours--far">
            <path d="M-120 112C108 67 253 145 430 122S740 31 925 89s330 86 795 -4" />
            <path d="M-140 176C88 118 268 210 454 178S760 72 956 137s346 97 792 4" />
            <path d="M-110 246C113 178 305 274 508 234S816 126 1006 194s322 110 742 36" />
            <path d="M-150 326C96 246 286 356 514 304S824 205 1034 268s306 126 744 36" />
            <path d="M-100 406C127 336 335 428 533 392S858 286 1050 350s282 122 682 54" />
            <path d="M-130 492C122 405 338 522 562 462S876 363 1073 427s280 130 674 48" />
            <path d="M-120 588C133 495 362 604 574 554S908 450 1100 516s258 128 640 50" />
            <path d="M-100 700C144 606 367 724 600 665S932 566 1127 631s254 135 618 45" />
          </g>
          <g class="ocean-contours ocean-contours--near">
            <path d="M-180 368C118 276 298 424 538 364S904 192 1154 286s356 142 740 14" />
            <path d="M-150 462C156 360 338 516 594 446S950 284 1206 372s306 142 694 12" />
            <path d="M-170 566C140 454 342 622 608 544S978 382 1236 470s272 138 648 12" />
            <path d="M-160 684C154 566 356 742 628 655S1002 486 1264 578s260 142 620 14" />
            <path d="M-140 812C168 684 376 868 652 778S1028 602 1292 694s244 146 596 14" />
          </g>
        </svg>
      </div>
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
            :email="accountIdentifier"
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
import {
  CreditCard,
  Home,
  Inbox,
  Library,
  MessageCircleMore,
  Plus,
  Settings,
  ShieldCheck,
  Trash2,
} from 'lucide-vue-next'
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
  { name: 'chat', to: '/chat', label: t('nav.chat'), icon: MessageCircleMore },
  { name: 'capture', to: '/capture', label: t('nav.capture'), icon: Plus },
  { name: 'processing', to: '/processing', label: t('nav.processing'), icon: Inbox },
  { name: 'billing', to: '/billing', label: t('nav.billing'), icon: CreditCard },
  ...(session.session?.admin
    ? [{ name: 'admin', to: '/admin/users', label: t('nav.admin'), icon: ShieldCheck }]
    : []),
  { name: 'trash', to: '/trash', label: t('nav.trash'), icon: Trash2 },
])

const accountDisplayName = computed(
  () => session.session?.displayName || session.session?.email.split('@')[0] || t('account.defaultName'),
)
const accountInitial = computed(() => firstCharacter(accountDisplayName.value))
const accountIdentifier = computed(
  () =>
    session.session?.loginName && session.session.loginName !== session.session.email
      ? session.session.loginName
      : session.session?.email || '',
)

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
