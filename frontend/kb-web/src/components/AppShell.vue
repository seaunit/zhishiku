<template>
  <div class="app-shell">
    <header class="mobile-header">
      <button
        type="button"
        class="icon-button menu-button"
        :aria-label="ui.menuOpen ? t('a11y.closeMenu') : t('a11y.openMenu')"
        :aria-expanded="ui.menuOpen"
        aria-controls="primary-sidebar"
        @click="ui.toggleMenu"
      >
        <X v-if="ui.menuOpen" :size="20" />
        <Menu v-else :size="20" />
      </button>
      <BrandMark />
      <RouterLink class="icon-button" :aria-label="t('nav.capture')" to="/capture" @click="ui.closeMenu">
        <SquarePen :size="19" />
      </RouterLink>
    </header>

    <button
      v-if="ui.menuOpen"
      type="button"
      class="menu-scrim"
      :aria-label="t('a11y.closeMenu')"
      @click="ui.closeMenu"
    />

    <aside
      id="primary-sidebar"
      class="sidebar"
      :class="{ open: ui.menuOpen }"
      :aria-label="t('a11y.mainNavigation')"
    >
      <div class="sidebar-head">
        <BrandMark />
        <button
          type="button"
          class="icon-button mobile-only"
          :aria-label="t('a11y.closeMenu')"
          @click="ui.closeMenu"
        >
          <X :size="20" />
        </button>
      </div>

      <nav class="primary-nav" :aria-label="t('a11y.mainNavigation')">
        <RouterLink class="nav-item" to="/" exact-active-class="active" @click="ui.closeMenu">
          <Search :size="18" />
          <span>{{ t('nav.search') }}</span>
        </RouterLink>
        <RouterLink class="nav-item" to="/capture" @click="ui.closeMenu">
          <SquarePen :size="18" />
          <span>{{ t('nav.capture') }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <LocaleSwitch />
        <button type="button" class="account-row" @click="signOut">
          <LogOut :size="17" />
          <span>{{ t('nav.logout') }}</span>
        </button>
      </div>
    </aside>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { LogOut, Menu, Search, SquarePen, X } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useUiStore } from '@/stores/ui'
import BrandMark from './BrandMark.vue'
import LocaleSwitch from './LocaleSwitch.vue'

const { t } = useI18n()
const ui = useUiStore()
const session = useSessionStore()
const router = useRouter()

async function signOut() {
  await session.logout()
  ui.closeMenu()
  await router.push({ name: 'login' })
}
</script>

