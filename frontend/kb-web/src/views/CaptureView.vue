<template>
  <section class="page capture-page">
    <header class="page-heading capture-heading">
      <div>
        <h1>{{ isEditing ? t('capture.editTitle') : t('capture.createTitle') }}</h1>
        <p>{{ isEditing ? t('capture.editSubtitle') : t('capture.createSubtitle') }}</p>
      </div>
      <RouterLink
        v-if="isEditing"
        class="button secondary"
        :to="`/library/${itemId}`"
      >
        <ArrowLeft :size="17" />
        <span>{{ t('common.back') }}</span>
      </RouterLink>
    </header>

    <div v-if="loading" class="editor-loading" aria-busy="true" :aria-label="t('a11y.loading')">
      <span class="skeleton title-line" />
      <span class="skeleton editor-block" />
    </div>

    <div v-else-if="loadError" class="inline-error" role="alert">
      <CircleAlert :size="20" />
      <div>
        <strong>{{ t('capture.loadFailed') }}</strong>
        <p>{{ loadError }}</p>
      </div>
      <button type="button" class="text-button" @click="loadItem">{{ t('common.retry') }}</button>
    </div>

    <div v-else-if="legacyPending" class="notice-panel">
      <Info :size="20" />
      <p>{{ t('capture.legacyPending') }}</p>
    </div>

    <form v-else class="capture-form" @submit.prevent="save">
      <div class="field">
        <label for="knowledge-title">{{ t('capture.titleLabel') }}</label>
        <input
          id="knowledge-title"
          v-model.trim="title"
          class="input title-input"
          type="text"
          maxlength="300"
          :placeholder="t('capture.titlePlaceholder')"
          required
        />
      </div>

      <div class="field editor-field">
        <label>{{ t('capture.bodyLabel') }}</label>
        <RichEditor
          v-model="bodyDocument"
          :item-id="isEditing ? itemId : undefined"
          @uploading="uploading = $event"
        />
      </div>

      <div class="field">
        <label for="knowledge-note">{{ t('capture.noteLabel') }}</label>
        <textarea
          id="knowledge-note"
          v-model="note"
          class="input textarea"
          rows="4"
          maxlength="20000"
          :placeholder="t('capture.notePlaceholder')"
        />
      </div>

      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

      <div class="form-actions">
        <button
          class="button primary save-button"
          type="submit"
          :disabled="saving || uploading || !title"
        >
          <LoaderCircle v-if="saving" class="spin" :size="18" />
          <Save v-else :size="18" />
          <span>
            {{
              saving
                ? t('capture.saving')
                : isEditing
                  ? t('capture.saveEdit')
                  : t('capture.save')
            }}
          </span>
        </button>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft, CircleAlert, Info, LoaderCircle, Save } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { createKnowledgeItem, getKnowledgeItem, updateKnowledgeItem } from '@/api/client'
import RichEditor from '@/components/RichEditor.vue'
import {
  validateRichTextDocument,
} from '@/lib/rich-text'
import { emptyRichTextDocument, type RichTextDocument } from '@/types/rich-text'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const itemId = computed(() => String(route.params.id || ''))
const isEditing = computed(() => route.name === 'edit')

const title = ref('')
const note = ref('')
const bodyDocument = ref<RichTextDocument>(emptyRichTextDocument())
const expectedVersion = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const loadError = ref('')
const formError = ref('')
const legacyPending = ref(false)

async function loadItem() {
  if (!isEditing.value) {
    return
  }

  loading.value = true
  loadError.value = ''
  legacyPending.value = false
  try {
    const item = await getKnowledgeItem(itemId.value)
    title.value = item.title
    note.value = item.note || ''
    expectedVersion.value = item.version

    if (!item.bodyDocument) {
      legacyPending.value = true
      return
    }

    const validation = validateRichTextDocument(item.bodyDocument)
    if (!validation.ok) {
      loadError.value = validation.message || t('errors.generic')
      return
    }
    bodyDocument.value = item.bodyDocument
  } catch (caught) {
    loadError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

async function save() {
  formError.value = ''
  if (!title.value.trim()) {
    formError.value = t('capture.titleRequired')
    return
  }

  const validation = validateRichTextDocument(bodyDocument.value)
  if (!validation.ok) {
    formError.value = validation.message || t('errors.generic')
    return
  }

  saving.value = true
  try {
    if (isEditing.value) {
      const updated = await updateKnowledgeItem(itemId.value, {
        expectedVersion: expectedVersion.value,
        title: title.value.trim(),
        bodyDocument: bodyDocument.value,
        note: note.value,
      })
      await router.replace(`/library/${updated.id}`)
    } else {
      const created = await createKnowledgeItem({
        title: title.value.trim(),
        bodyDocument: bodyDocument.value,
        note: note.value,
      })
      await router.replace(`/library/${created.id}`)
    }
  } catch (caught) {
    const status =
      typeof caught === 'object' && caught !== null && 'status' in caught
        ? Number(caught.status)
        : 0
    if (status === 409) {
      formError.value = t('errors.conflict')
    } else {
      formError.value = caught instanceof Error ? caught.message : t('capture.saveFailed')
    }
  } finally {
    saving.value = false
  }
}

onMounted(loadItem)
</script>

