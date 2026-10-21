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
      <button type="button" class="text-button" @click="loadItem">
        {{ t('common.retry') }}
      </button>
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
          @source-report="sourceReport = $event"
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

      <div v-if="draftStatus" class="draft-bar">
        <span data-testid="draft-status">{{ draftStatus }}</span>
        <button
          v-if="draftAvailable"
          type="button"
          class="text-button"
          @click="discardDraft"
        >
          {{ t('capture.clearDraft') }}
        </button>
      </div>

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

    <ConflictDialog
      v-if="conflictOpen"
      :detail="conflictDetail"
      :busy="conflictBusy"
      @reload="reloadConflict"
      @save-copy="saveConflictCopy"
      @overwrite="overwriteConflict"
      @cancel="conflictOpen = false"
    />
  </section>
</template>

<script setup lang="ts">
import { ArrowLeft, CircleAlert, Info, LoaderCircle, Save } from 'lucide-vue-next'
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  createKnowledgeItem,
  getKnowledgeItem,
  updateKnowledgeItem,
} from '@/api/client'
import RichEditor from '@/components/RichEditor.vue'
import ConflictDialog from '@/features/editor/conflict/ConflictDialog.vue'
import { isRichTextConflict } from '@/features/editor/conflict/conflict'
import {
  clearDraft,
  loadDraft,
  saveDraft,
} from '@/features/knowledge/drafts'
import { validateRichTextDocument } from '@/lib/rich-text'
import {
  emptyRichTextDocument,
  type RichTextDocument,
} from '@/types/rich-text'
import type { BodySourceRequest, KnowledgeItem } from '@/types/api'

const { t, locale } = useI18n()
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
const sourceReport = ref<BodySourceRequest | null>(null)
const draftStatus = ref('')
const draftAvailable = ref(false)
const draftReady = ref(false)
const conflictOpen = ref(false)
const conflictBusy = ref(false)
const conflictDetail = ref('')
let draftTimer: number | undefined

function activeStorage() {
  return window.localStorage
}

function draftItemId() {
  return isEditing.value ? itemId.value : undefined
}

async function initialize() {
  if (isEditing.value) {
    await loadItem()
  } else {
    restoreDraft()
  }
  draftReady.value = true
}

async function loadItem() {
  if (!isEditing.value) {
    return
  }

  loading.value = true
  loadError.value = ''
  legacyPending.value = false
  try {
    const item = await getKnowledgeItem(itemId.value)
    applyItem(item)
  } catch (caught) {
    loadError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    restoreDraft()
    loading.value = false
  }
}

function applyItem(item: KnowledgeItem) {
  title.value = item.title
  note.value = item.note || ''
  expectedVersion.value = item.version

  if (!item.bodyDocument) {
    legacyPending.value = true
    bodyDocument.value = emptyRichTextDocument()
    return
  }

  const validation = validateRichTextDocument(item.bodyDocument)
  if (!validation.ok) {
    loadError.value = validation.message || t('errors.generic')
    return
  }
  bodyDocument.value = item.bodyDocument
}

function restoreDraft() {
  const result = loadDraft(activeStorage(), draftItemId())
  if (result.status === 'expired' || result.status === 'invalid') {
    clearDraft(activeStorage(), draftItemId())
    return
  }
  if (result.status !== 'restored') {
    return
  }

  title.value = result.draft.title
  note.value = result.draft.note
  bodyDocument.value = result.draft.bodyDocument
  if (!isEditing.value) {
    expectedVersion.value = 0
  }
  draftAvailable.value = true
  draftStatus.value = t('capture.draftRestored', {
    time: formatDraftTime(result.draft.savedAt),
  })
}

function scheduleDraft() {
  if (!draftReady.value) {
    return
  }
  window.clearTimeout(draftTimer)
  draftTimer = window.setTimeout(persistDraft, 550)
}

function persistDraft() {
  if (!draftReady.value || !validateRichTextDocument(bodyDocument.value).ok) {
    return
  }
  try {
    const draft = saveDraft(activeStorage(), {
      itemId: draftItemId(),
      expectedVersion: expectedVersion.value,
      title: title.value,
      note: note.value,
      bodyDocument: bodyDocument.value,
    })
    draftAvailable.value = true
    draftStatus.value = t('capture.draftSaved', {
      time: formatDraftTime(draft.savedAt),
    })
  } catch {
    draftStatus.value = t('capture.draftFailed')
  }
}

function discardDraft() {
  clearDraft(activeStorage(), draftItemId())
  draftAvailable.value = false
  draftStatus.value = t('capture.draftCleared')
}

function formatDraftTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '--:--'
  }
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
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
      clearDraft(activeStorage(), draftItemId())
      await router.replace(`/library/${updated.id}`)
    } else {
      const created = await createKnowledgeItem({
        title: title.value.trim(),
        bodyDocument: bodyDocument.value,
        note: note.value,
        bodySource: sourceReport.value,
      })
      clearDraft(activeStorage(), draftItemId())
      await router.replace(`/library/${created.id}`)
    }
  } catch (caught) {
    if (isRichTextConflict(caught)) {
      persistDraft()
      conflictDetail.value = caught.message
      conflictOpen.value = true
    } else {
      formError.value =
        caught instanceof Error ? caught.message : t('capture.saveFailed')
    }
  } finally {
    saving.value = false
  }
}

async function reloadConflict() {
  conflictBusy.value = true
  try {
    const latest = await getKnowledgeItem(itemId.value)
    applyItem(latest)
    clearDraft(activeStorage(), draftItemId())
    draftAvailable.value = false
    draftStatus.value = t('conflict.reloaded')
    conflictOpen.value = false
  } catch (caught) {
    formError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    conflictBusy.value = false
  }
}

async function saveConflictCopy() {
  conflictBusy.value = true
  try {
    const created = await createKnowledgeItem({
      title: t('conflict.copyTitle', { title: title.value.trim() }),
      bodyDocument: bodyDocument.value,
      note: note.value,
      bodySource: sourceReport.value,
    })
    clearDraft(activeStorage(), draftItemId())
    conflictOpen.value = false
    await router.replace(`/library/${created.id}`)
  } catch (caught) {
    formError.value = caught instanceof Error ? caught.message : t('capture.saveFailed')
  } finally {
    conflictBusy.value = false
  }
}

async function overwriteConflict() {
  conflictBusy.value = true
  try {
    const latest = await getKnowledgeItem(itemId.value)
    const updated = await updateKnowledgeItem(itemId.value, {
      expectedVersion: latest.version,
      title: title.value.trim(),
      bodyDocument: bodyDocument.value,
      note: note.value,
    })
    clearDraft(activeStorage(), draftItemId())
    conflictOpen.value = false
    await router.replace(`/library/${updated.id}`)
  } catch (caught) {
    formError.value = caught instanceof Error ? caught.message : t('capture.saveFailed')
  } finally {
    conflictBusy.value = false
  }
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!draftAvailable.value) {
    return
  }
  event.preventDefault()
}

watch([title, note, bodyDocument], scheduleDraft, { deep: true })

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  void initialize()
})

onBeforeUnmount(() => {
  window.clearTimeout(draftTimer)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>
