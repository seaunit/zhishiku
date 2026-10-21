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

    <div v-else class="panel capture-workbench">
      <div
        v-if="!isEditing"
        class="capture-tabs"
        role="tablist"
        :aria-label="t('capture.createTitle')"
      >
        <button
          v-for="item in captureModes"
          :key="item.value"
          type="button"
          class="capture-tab"
          role="tab"
          :aria-selected="activeMode === item.value"
          :tabindex="activeMode === item.value ? 0 : -1"
          @click="selectMode(item.value)"
        >
          {{ item.label }}
        </button>
      </div>

      <form class="capture-form" @submit.prevent="save">
        <template v-if="activeMode === 'manual'">
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
        </template>

        <template v-else-if="activeMode === 'webpage'">
          <div class="field">
            <label for="capture-webpage-url">{{ t('capture.webpageUrl') }}</label>
            <input
              id="capture-webpage-url"
              v-model.trim="webpageUrl"
              class="input"
              type="url"
              inputmode="url"
              :placeholder="t('capture.webpageUrlPlaceholder')"
              required
              @input="clearError"
            />
            <p class="field-hint">{{ t('capture.webpageHint') }}</p>
          </div>
        </template>

        <template v-else-if="activeMode === 'document'">
          <div
            class="document-drop"
            @dragover.prevent
            @drop.prevent="handleDocumentDrop"
          >
            <FileUp :size="28" />
            <input
              id="capture-document-file"
              class="sr-only"
              type="file"
              aria-label="选择文档或图片"
              accept=".pdf,.doc,.docx,.txt,.md,.markdown,.jpg,.jpeg,.png,.webp,.heic,.heif,image/jpeg,image/png,image/webp,image/heic,image/heif,text/plain,text/markdown"
              @change="handleDocumentChange"
            />
            <label class="button secondary" for="capture-document-file">
              {{ t('capture.documentSelect') }}
            </label>
            <p>{{ t('capture.documentHint') }}</p>
          </div>

          <div v-if="documentFile" class="selected-source">
            <FileText :size="18" />
            <span>
              <strong>{{ t('capture.documentReady') }}</strong>
              <small>{{ documentFile.name }}</small>
            </span>
            <button type="button" class="text-button" @click="clearDocument">
              {{ t('capture.documentRemove') }}
            </button>
          </div>
        </template>

        <template v-else>
          <div
            class="paste-zone"
            :class="{ 'has-image': screenshotPreviewUrl }"
            data-testid="screenshot-paste-zone"
          >
            <textarea
              ref="screenshotPasteInput"
              class="paste-capture"
              :aria-label="t('capture.screenshotPasteAria')"
              readonly
              @paste="handleScreenshotPaste"
            />
            <div v-if="!screenshotPreviewUrl" class="paste-visual">
              <span class="paste-icon"><ClipboardPaste :size="22" /></span>
              <strong>{{ t('capture.screenshotTitle') }}</strong>
              <p>{{ t('capture.screenshotHint') }}</p>
              <span class="paste-shortcuts"><kbd>Ctrl</kbd> + <kbd>V</kbd> / <kbd>⌘</kbd> + <kbd>V</kbd></span>
            </div>
            <div v-else class="paste-preview">
              <img :src="screenshotPreviewUrl" :alt="t('capture.screenshotReady')" />
              <span>
                <strong>{{ t('capture.screenshotReady') }}</strong>
                <small>{{ screenshotFile?.name }}</small>
              </span>
              <button type="button" class="text-button" @click.stop="clearScreenshot">
                {{ t('capture.screenshotClear') }}
              </button>
            </div>
          </div>
        </template>

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

        <div v-if="activeMode === 'manual' && draftStatus" class="draft-bar">
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
            :disabled="submitDisabled"
          >
            <LoaderCircle v-if="saving || uploading" class="spin" :size="18" />
            <Save v-else :size="18" />
            <span>{{ saving || uploading ? t('capture.saving') : submitLabel }}</span>
          </button>
        </div>
      </form>
    </div>

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
import {
  ArrowLeft,
  CircleAlert,
  ClipboardPaste,
  FileText,
  FileUp,
  Info,
  LoaderCircle,
  Save,
} from 'lucide-vue-next'
import {
  computed,
  nextTick,
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
  uploadRichImage,
} from '@/api/client'
import RichEditor from '@/components/RichEditor.vue'
import ConflictDialog from '@/features/editor/conflict/ConflictDialog.vue'
import { isRichTextConflict } from '@/features/editor/conflict/conflict'
import { clearDraft, loadDraft, saveDraft } from '@/features/knowledge/drafts'
import { validateRichTextDocument } from '@/lib/rich-text'
import {
  emptyRichTextDocument,
  type RichTextDocument,
  type RichTextNode,
} from '@/types/rich-text'
import type { BodySourceRequest, KnowledgeItem } from '@/types/api'

type CaptureMode = 'manual' | 'webpage' | 'document' | 'screenshot'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const itemId = computed(() => String(route.params.id || ''))
const isEditing = computed(() => route.name === 'edit')
const mode = ref<CaptureMode>('manual')
const activeMode = computed<CaptureMode>(() => (isEditing.value ? 'manual' : mode.value))

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
const webpageUrl = ref('')
const documentFile = ref<File | null>(null)
const screenshotFile = ref<File | null>(null)
const screenshotPreviewUrl = ref('')
const screenshotPasteInput = ref<HTMLTextAreaElement | null>(null)
let draftTimer: number | undefined

const captureModes = computed(() => [
  { value: 'manual' as const, label: t('capture.modeManual') },
  { value: 'webpage' as const, label: t('capture.modeWebpage') },
  { value: 'document' as const, label: t('capture.modeDocument') },
  { value: 'screenshot' as const, label: t('capture.modeScreenshot') },
])

const submitLabel = computed(() => {
  if (isEditing.value) return t('capture.saveEdit')
  if (mode.value === 'webpage') return t('capture.saveLink')
  if (mode.value === 'document') return t('capture.saveDocument')
  if (mode.value === 'screenshot') return t('capture.saveScreenshot')
  return t('capture.save')
})

const submitDisabled = computed(() => {
  if (saving.value || uploading.value) return true
  if (activeMode.value === 'manual') return !title.value
  if (activeMode.value === 'document') return !documentFile.value
  if (activeMode.value === 'screenshot') return !screenshotFile.value
  return !webpageUrl.value
})

function activeStorage() {
  return window.localStorage
}

function draftItemId() {
  return isEditing.value ? itemId.value : undefined
}

function clearError() {
  formError.value = ''
}

async function selectMode(next: CaptureMode) {
  mode.value = next
  clearError()
  if (next === 'screenshot') {
    await nextTick()
    screenshotPasteInput.value?.focus()
  }
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
  if (!draftReady.value || activeMode.value !== 'manual') {
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

function isImageFile(file: File) {
  return /^image\/(jpeg|png|webp|heic|heif)$/i.test(file.type)
}

function isTextFile(file: File) {
  return (
    file.type === 'text/plain' ||
    file.type === 'text/markdown' ||
    /\.(txt|md|markdown)$/i.test(file.name)
  )
}

function setDocument(file: File | null) {
  formError.value = ''
  if (!file) {
    documentFile.value = null
    return
  }
  if (file.size > 25 * 1024 * 1024) {
    documentFile.value = null
    formError.value = t('capture.uploadTooLarge')
    return
  }
  documentFile.value = file
}

function handleDocumentChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  setDocument(file)
}

function handleDocumentDrop(event: DragEvent) {
  setDocument(event.dataTransfer?.files?.[0] ?? null)
}

function clearDocument() {
  documentFile.value = null
  const input = document.querySelector<HTMLInputElement>('#capture-document-file')
  if (input) input.value = ''
}

function setScreenshot(file: File | null) {
  if (screenshotPreviewUrl.value) {
    URL.revokeObjectURL(screenshotPreviewUrl.value)
  }
  screenshotFile.value = file
  screenshotPreviewUrl.value = file ? URL.createObjectURL(file) : ''
  clearError()
}

function clearScreenshot() {
  setScreenshot(null)
  screenshotPasteInput.value?.focus()
}

function handleScreenshotPaste(event: ClipboardEvent) {
  const image = [...(event.clipboardData?.items ?? [])].find((item) =>
    item.type.startsWith('image/'),
  )
  const file = image?.getAsFile()
  if (!file) {
    formError.value = t('capture.screenshotMissing')
    return
  }
  event.preventDefault()
  setScreenshot(file)
}

function textDocument(text: string): RichTextDocument {
  const paragraphs = text
    .replace(/\r\n/g, '\n')
    .split(/\n{2,}/)
    .map((part) => part.trim())
    .filter(Boolean)
    .map<RichTextNode>((part) => ({
      type: 'paragraph',
      content: [{ type: 'text', text: part }],
    }))
  return {
    type: 'doc',
    content: paragraphs.length ? paragraphs : [{ type: 'paragraph' }],
  }
}

function webpageDocument(url: string): RichTextDocument {
  return {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [
          { type: 'text', text: `${t('capture.webpagePending')} ` },
          {
            type: 'text',
            text: url,
            marks: [{ type: 'link', attrs: { href: url } }],
          },
        ],
      },
    ],
  }
}

async function imageDocument(file: File): Promise<RichTextDocument> {
  const upload = await uploadRichImage(file)
  if (upload.status !== 'ready' && upload.status !== 'attached') {
    throw new Error(t('capture.uploadFailed'))
  }
  return {
    type: 'doc',
    content: [
      {
        type: 'image',
        attrs: {
          assetId: upload.uploadId,
          uploadId: upload.uploadId,
          nodeId: `capture-${upload.uploadId}`,
          alt: file.name || t('capture.imageAlt'),
          caption: note.value.trim(),
          displayWidthPx: 720,
          aspectRatio: Number(upload.aspectRatio) || 1.5,
          alignment: 'center',
          decorative: false,
        },
      },
    ],
  }
}

function fallbackTitle(kind: CaptureMode, source?: string) {
  const stamp = new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date())
  if (source) return source
  if (kind === 'webpage') return `网页收藏 · ${stamp}`
  if (kind === 'document') return `文档知识 · ${stamp}`
  return `截图知识 · ${stamp}`
}

async function saveImportedCapture() {
  clearError()
  let document: RichTextDocument
  let source: BodySourceRequest
  let sourceTitle: string
  const capturedAt = new Date().toISOString()

  if (mode.value === 'webpage') {
    let parsed: URL
    try {
      parsed = new URL(webpageUrl.value)
    } catch {
      formError.value = t('capture.invalidUrl')
      return
    }
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      formError.value = t('capture.invalidUrl')
      return
    }
    sourceTitle = parsed.hostname
    document = webpageDocument(parsed.toString())
    source = {
      kind: 'webpage',
      sourceUrl: parsed.toString(),
      capturedAt,
    }
  } else if (mode.value === 'document') {
    const file = documentFile.value
    if (!file) {
      formError.value = t('capture.documentUnsupported')
      return
    }
    sourceTitle = file.name.replace(/\.[^.]+$/, '') || file.name
    if (isImageFile(file)) {
      source = { kind: 'document', capturedAt }
      uploading.value = true
      try {
        document = await imageDocument(file)
      } catch (caught) {
        formError.value =
          caught instanceof Error ? caught.message : t('capture.uploadFailed')
        return
      } finally {
        uploading.value = false
      }
    } else if (isTextFile(file)) {
      const content = await file.text()
      if (!content.trim()) {
        formError.value = t('capture.documentUnsupported')
        return
      }
      document = textDocument(content)
      source = { kind: 'document', capturedAt }
    } else {
      formError.value = t('capture.documentExtractionUnavailable')
      return
    }
  } else {
    const file = screenshotFile.value
    if (!file) {
      formError.value = t('capture.screenshotMissing')
      return
    }
    sourceTitle = fallbackTitle('screenshot')
    source = { kind: 'screenshot', capturedAt }
    uploading.value = true
    try {
      document = await imageDocument(file)
    } catch (caught) {
      formError.value =
        caught instanceof Error ? caught.message : t('capture.uploadFailed')
      return
    } finally {
      uploading.value = false
    }
  }

  saving.value = true
  try {
    const created = await createKnowledgeItem({
      title: title.value.trim() || sourceTitle,
      bodyDocument: document,
      note: note.value,
      bodySource: source,
    })
    await router.replace(`/library/${created.id}`)
  } catch (caught) {
    formError.value = caught instanceof Error ? caught.message : t('capture.saveFailed')
  } finally {
    saving.value = false
  }
}

async function save() {
  if (!isEditing.value && activeMode.value !== 'manual') {
    await saveImportedCapture()
    return
  }

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
  if (screenshotPreviewUrl.value) {
    URL.revokeObjectURL(screenshotPreviewUrl.value)
  }
})
</script>
