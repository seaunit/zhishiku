<template>
  <Teleport to="body">
    <div class="dialog-backdrop">
      <section
        ref="dialog"
        class="conflict-dialog"
        role="alertdialog"
        aria-modal="true"
        :aria-label="t('conflict.title')"
        tabindex="-1"
      >
        <header>
          <h2>{{ t('conflict.title') }}</h2>
          <p>{{ t('conflict.body') }}</p>
          <p v-if="detail" class="conflict-detail">{{ detail }}</p>
        </header>
        <div class="conflict-actions">
          <button
            ref="firstAction"
            type="button"
            class="button primary"
            :disabled="busy"
            @click="emit('reload')"
          >
            {{ t('conflict.reload') }}
          </button>
          <button
            type="button"
            class="button secondary"
            :disabled="busy"
            @click="emit('save-copy')"
          >
            {{ t('conflict.saveCopy') }}
          </button>
          <button
            type="button"
            class="button secondary danger"
            :disabled="busy"
            @click="emit('overwrite')"
          >
            {{ t('conflict.overwrite') }}
          </button>
          <button
            type="button"
            class="text-button"
            :disabled="busy"
            @click="emit('cancel')"
          >
            {{ t('conflict.keepEditing') }}
          </button>
        </div>
        <p v-if="busy" class="conflict-busy" role="status">
          {{ t('common.loading') }}
        </p>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{
  detail?: string
  busy?: boolean
}>()

const emit = defineEmits<{
  reload: []
  'save-copy': []
  overwrite: []
  cancel: []
}>()

const { t } = useI18n()
const firstAction = ref<HTMLButtonElement>()
let previousFocus: HTMLElement | null = null

onMounted(() => {
  previousFocus = document.activeElement as HTMLElement | null
  document.body.classList.add('dialog-open')
  firstAction.value?.focus()
})

onBeforeUnmount(() => {
  document.body.classList.remove('dialog-open')
  previousFocus?.focus()
})
</script>
