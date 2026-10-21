<template>
  <div ref="root" class="account-control">
    <button
      ref="trigger"
      type="button"
      class="avatar-button"
      aria-haspopup="menu"
      :aria-expanded="open"
      aria-controls="account-menu"
      :aria-label="t('account.openMenu', { name: displayName })"
      :title="t('account.openMenu', { name: displayName })"
      @click="toggle"
      @keydown.down.prevent="openMenu"
    >
      {{ initial }}
    </button>

    <Transition name="account-menu">
      <div
        v-if="open"
        id="account-menu"
        class="account-menu"
        role="menu"
        :aria-label="t('account.menuLabel')"
        @keydown="onMenuKeydown"
      >
        <header
          class="account-menu-summary"
          role="group"
          :aria-label="t('account.currentAccount')"
        >
          <span class="account-avatar-large" aria-hidden="true">{{ initial }}</span>
          <span>
            <strong :title="displayName">{{ displayName }}</strong>
            <small :title="email">{{ email }}</small>
          </span>
        </header>

        <div class="account-menu-actions">
          <RouterLink
            class="account-menu-item"
            role="menuitem"
            to="/profile"
            @click="closeMenu"
          >
            <UserRound :size="17" :stroke-width="1.7" />
            <span>{{ t('account.profile') }}</span>
          </RouterLink>
          <button
            type="button"
            class="account-menu-item account-menu-logout"
            role="menuitem"
            @click="requestLogout"
          >
            <LogOut :size="17" :stroke-width="1.7" />
            <span>{{ t('account.logout') }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { LogOut, UserRound } from 'lucide-vue-next'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{
  displayName: string
  email: string
  initial: string
}>()

const emit = defineEmits<{
  'request-logout': []
}>()

const { t } = useI18n()
const root = ref<HTMLElement>()
const trigger = ref<HTMLButtonElement>()
const open = ref(false)

function toggle() {
  open.value = !open.value
}

function openMenu() {
  open.value = true
}

function closeMenu(restoreFocus = false) {
  open.value = false
  if (restoreFocus) {
    window.setTimeout(() => trigger.value?.focus(), 0)
  }
}

function requestLogout() {
  closeMenu()
  emit('request-logout')
}

function onMenuKeydown(event: KeyboardEvent) {
  const items = Array.from(
    root.value?.querySelectorAll<HTMLElement>('[role="menuitem"]') || [],
  )
  const currentIndex = items.indexOf(document.activeElement as HTMLElement)
  if (event.key === 'Escape') {
    event.preventDefault()
    closeMenu(true)
    return
  }
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    const step = event.key === 'ArrowDown' ? 1 : -1
    const nextIndex = (currentIndex + step + items.length) % items.length
    items[nextIndex]?.focus()
  }
}

function onPointerDown(event: PointerEvent) {
  if (open.value && !root.value?.contains(event.target as Node)) {
    closeMenu(true)
  }
}

watch(open, (value) => {
  if (value) {
    void nextTick(() => {
      root.value?.querySelector<HTMLElement>('[role="menuitem"]')?.focus()
    })
  }
})

document.addEventListener('pointerdown', onPointerDown)
onBeforeUnmount(() => document.removeEventListener('pointerdown', onPointerDown))
</script>
