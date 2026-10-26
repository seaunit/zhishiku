<template>
  <Teleport to="body">
    <div class="dialog-backdrop" @click.self="emit('cancel')">
      <section
        ref="dialog"
        class="confirm-dialog"
        role="alertdialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="bodyId"
        tabindex="-1"
        @keydown="onKeydown"
      >
        <header>
          <h2 :id="titleId">{{ title }}</h2>
          <p :id="bodyId">{{ body }}</p>
          <p v-if="error" class="field-error" role="alert">{{ error }}</p>
        </header>
        <div class="confirm-actions">
          <button
            ref="cancelButton"
            type="button"
            class="button secondary"
            :disabled="busy"
            @click="emit('cancel')"
          >
            {{ cancelLabel }}
          </button>
          <button
            ref="confirmButton"
            type="button"
            class="button primary"
            :class="{ danger: tone === 'danger' }"
            :disabled="busy"
            @click="emit('confirm')"
          >
            {{ confirmLabel }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    title: string
    body: string
    cancelLabel: string
    confirmLabel: string
    busy?: boolean
    error?: string
    tone?: 'danger' | 'primary'
  }>(),
  {
    busy: false,
    error: '',
    tone: 'danger',
  },
)
const { tone } = props

const emit = defineEmits<{
  cancel: []
  confirm: []
}>()

const titleId = `confirm-title-${Math.random().toString(36).slice(2)}`
const bodyId = `confirm-body-${Math.random().toString(36).slice(2)}`
const cancelButton = ref<HTMLButtonElement>()
const confirmButton = ref<HTMLButtonElement>()
let previousFocus: HTMLElement | null = null

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('cancel')
    return
  }
  if (event.key !== 'Tab') {
    return
  }
  const first = cancelButton.value
  const last = confirmButton.value
  if (!first || !last) {
    return
  }
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  previousFocus = document.activeElement as HTMLElement | null
  document.body.classList.add('dialog-open')
  cancelButton.value?.focus()
})

onBeforeUnmount(() => {
  document.body.classList.remove('dialog-open')
  previousFocus?.focus()
})
</script>
