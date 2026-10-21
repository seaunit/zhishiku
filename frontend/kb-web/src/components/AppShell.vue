<template>
  <div class="app-shell">
    <aside class="rail" aria-label="应用侧栏">
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
            <b v-if="item.name === 'processing' && pendingCount > 0" class="nav-count">
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
          <button
            type="button"
            class="avatar-button"
            :aria-label="t('nav.logout')"
            :title="t('nav.logout')"
            @click="signOut"
          >
            {{ accountInitial }}
          </button>
        </div>
      </header>

      <main class="app-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Home, Inbox, Library, Plus, Settings, Trash2 } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import BrandMark from './BrandMark.vue'
import LocaleSwitch from './LocaleSwitch.vue'

const { t } = useI18n()
const router = useRouter()
const session = useSessionStore()
const pendingCount = ref(0)

const primaryItems = computed(() => [
  { name: 'home', to: '/', label: t('nav.home'), icon: Home },
  { name: 'library', to: '/library', label: t('nav.library'), icon: Library },
  { name: 'capture', to: '/capture', label: t('nav.capture'), icon: Plus },
  { name: 'processing', to: '/processing', label: t('nav.processing'), icon: Inbox },
  { name: 'trash', to: '/trash', label: t('nav.trash'), icon: Trash2 },
])

const accountInitial = computed(() => {
  const email = session.session?.email
  return email ? email.charAt(0).toUpperCase() : '识'
})

async function signOut() {
  await session.logout()
  await router.push({ name: 'login' })
}
</script>
