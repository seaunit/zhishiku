<template>
  <Teleport to="body">
    <div class="lightbox-backdrop" @mousedown.self="emit('close')">
      <section
        ref="dialog"
        class="image-lightbox"
        role="dialog"
        aria-modal="true"
        :aria-label="t('detail.imagePreview')"
        tabindex="-1"
        @keydown="handleKeydown"
      >
        <header class="lightbox-head">
          <h2>{{ t('detail.imagePreview') }}</h2>
          <button
            ref="closeButton"
            type="button"
            class="button secondary"
            @click="emit('close')"
          >
            {{ t('common.close') }}
          </button>
        </header>
        <div class="lightbox-image">
          <SafeImage
            :asset-id="String(node.attrs?.assetId || '')"
            :alt="String(node.attrs?.alt || '')"
            :decorative="Boolean(node.attrs?.decorative)"
            :preview-url="media.getPreview(String(node.attrs?.assetId || ''))"
            :aspect-ratio="Number(node.attrs?.aspectRatio || 1.5)"
          />
        </div>
        <p v-if="node.attrs?.caption" class="lightbox-caption">
          {{ node.attrs.caption }}
        </p>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import SafeImage from '@/components/SafeImage.vue'
import { useMediaStore } from '@/stores/media'
import type { RichTextNode } from '@/types/rich-text'

const props = defineProps<{
  node: RichTextNode
}>()

const emit = defineEmits<{
  close: []
}>()

const { t } = useI18n()
const media = useMediaStore()
const dialog = ref<HTMLElement>()
const closeButton = ref<HTMLButtonElement>()
let previousFocus: HTMLElement | null = null

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
    return
  }
  if (event.key !== 'Tab') {
    return
  }
  const focusable = dialog.value?.querySelectorAll<HTMLElement>(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
  )
  if (!focusable?.length) {
    return
  }
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(async () => {
  previousFocus = document.activeElement as HTMLElement | null
  document.body.classList.add('dialog-open')
  await nextTick()
  closeButton.value?.focus()
})

onBeforeUnmount(() => {
  document.body.classList.remove('dialog-open')
  previousFocus?.focus()
})

void props
</script>
