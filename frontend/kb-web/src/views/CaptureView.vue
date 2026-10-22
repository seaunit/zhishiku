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

        <template v-else-if="activeMode === 'ai-text'">
          <div class="field">
            <label for="capture-ai-text">{{ t('capture.aiTextLabel') }}</label>
            <textarea
              id="capture-ai-text"
              v-model="aiText"
              class="input textarea ai-text-input"
              rows="9"
              maxlength="30000"
              :placeholder="t('capture.aiTextPlaceholder')"
              required
              @input="quoteInputChanged"
            />
            <label class="authorization-row webpage-consent">
              <span>
                <strong>{{ t('capture.aiTextConsentTitle') }}</strong>
                <small>{{ t('capture.aiTextConsentHint') }}</small>
              </span>
              <input
                v-model="externalAiConsent"
                type="checkbox"
                :aria-label="t('capture.aiTextConsentTitle')"
              />
            </label>
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
            <p v-if="saving" class="field-hint" role="status">
              {{ t('capture.webpagePending') }}
            </p>
            <label class="authorization-row webpage-consent">
              <span>
                <strong>{{ t('capture.webpageConsentTitle') }}</strong>
                <small>{{ t('capture.webpageConsentHint') }}</small>
              </span>
              <input
                v-model="externalAiConsent"
                type="checkbox"
                :aria-label="t('capture.webpageConsentTitle')"
              />
            </label>
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
              :aria-label="t('capture.documentSelect')"
              accept=".pdf,.doc,.docx,.rtf,.txt,.md,.markdown,.jpg,.jpeg,.png,.webp,.heic,.heif,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/rtf,text/plain,text/markdown,image/jpeg,image/png,image/webp,image/heic,image/heif"
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
          <label
            v-if="documentFile && documentRequiresAiConsent"
            class="authorization-row webpage-consent"
          >
            <span>
              <strong>{{ t('capture.documentConsentTitle') }}</strong>
              <small>{{ t('capture.documentConsentHint') }}</small>
            </span>
            <input
              v-model="externalAiConsent"
              type="checkbox"
              :aria-label="t('capture.documentConsentTitle')"
            />
          </label>
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
          <label v-if="screenshotFile" class="authorization-row webpage-consent">
            <span>
              <strong>{{ t('capture.screenshotConsentTitle') }}</strong>
              <small>{{ t('capture.screenshotConsentHint') }}</small>
            </span>
            <input
              v-model="externalAiConsent"
              type="checkbox"
              :aria-label="t('capture.screenshotConsentTitle')"
            />
          </label>
        </template>

        <div v-if="activeMode !== 'webpage'" class="field">
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

        <section v-if="pendingQuote" class="billing-quote" aria-labelledby="capture-quote-title">
          <header>
            <div>
              <strong id="capture-quote-title">{{ t('capture.quoteTitle') }}</strong>
              <small>{{ t('capture.quoteHint') }}</small>
            </div>
            <span class="billing-quote-total num">
              {{ t('capture.pointsValue', { points: pendingQuote.maxPoints }) }}
            </span>
          </header>
          <ul>
            <li v-for="line in pendingQuote.lines" :key="line.feature">
              <span>{{ billingFeatureLabel(line.feature) }}</span>
              <span class="num">
                {{ line.quantity }} × {{ line.pointsPerUnit }} = {{ line.maxPoints }}
              </span>
            </li>
          </ul>
          <footer>
            <span>{{ t('capture.quoteBalance', {
              available: pendingQuote.availablePoints,
              remaining: pendingQuote.projectedRemainingPoints,
            }) }}</span>
            <button type="button" class="text-button" @click="pendingQuote = null">
              {{ t('common.cancel') }}
            </button>
          </footer>
        </section>

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
            <LoaderCircle v-if="saving || uploading || quoteBusy" class="spin" :size="18" />
            <Save v-else :size="18" />
            <span>
              {{
                quoteBusy
                  ? t('capture.quotePreparing')
                  : saving || uploading
                    ? t('capture.saving')
                    : submitLabel
              }}
            </span>
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
  ApiError,
  confirmBillingQuote,
  createBillingPreflight,
  createBillingQuote,
  createKnowledgeItem,
  getAiConsents,
  getKnowledgeItem,
  importDocument,
  importScreenshot,
  importText,
  importWebPage,
  updateKnowledgeItem,
  updateAiConsent,
} from '@/api/client'
import RichEditor from '@/components/RichEditor.vue'
import ConflictDialog from '@/features/editor/conflict/ConflictDialog.vue'
import { isRichTextConflict } from '@/features/editor/conflict/conflict'
import { clearDraft, loadDraft, saveDraft } from '@/features/knowledge/drafts'
import { createIdempotencyKey } from '@/lib/id'
import { validateRichTextDocument } from '@/lib/rich-text'
import {
  emptyRichTextDocument,
  type RichTextDocument,
} from '@/types/rich-text'
import type { BodySourceRequest, KnowledgeItem } from '@/types/api'
import type { AiConsent, AiConsentScope } from '@/types/api'
import type { BillingQuote } from '@/types/api'

type CaptureMode = 'manual' | 'ai-text' | 'webpage' | 'document' | 'screenshot'

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
const aiText = ref('')
const externalAiConsent = ref(false)
const consentByScope = ref<Partial<Record<AiConsentScope, AiConsent>>>({})
const consentPreferencesLoaded = ref(false)
const syncingConsent = ref(false)
const documentFile = ref<File | null>(null)
const documentRequiresAiConsent = computed(() => Boolean(documentFile.value))
const screenshotFile = ref<File | null>(null)
const screenshotPreviewUrl = ref('')
const screenshotPasteInput = ref<HTMLTextAreaElement | null>(null)
const pendingQuote = ref<BillingQuote | null>(null)
const quoteBusy = ref(false)
const captureRequestKey = ref('')
let draftTimer: number | undefined

const captureModes = computed(() => [
  { value: 'manual' as const, label: t('capture.modeManual') },
  { value: 'ai-text' as const, label: t('capture.modeAiText') },
  { value: 'webpage' as const, label: t('capture.modeWebpage') },
  { value: 'document' as const, label: t('capture.modeDocument') },
  { value: 'screenshot' as const, label: t('capture.modeScreenshot') },
])

const submitLabel = computed(() => {
  if (isEditing.value) return t('capture.saveEdit')
  if (pendingQuote.value) {
    return t('capture.confirmPoints', { points: pendingQuote.value.maxPoints })
  }
  if (mode.value === 'ai-text') return t('capture.saveAiText')
  if (mode.value === 'webpage') return t('capture.saveWebpageImport')
  if (mode.value === 'document') return t('capture.saveDocument')
  if (mode.value === 'screenshot') return t('capture.saveScreenshot')
  return t('capture.save')
})

const submitDisabled = computed(() => {
  if (saving.value || uploading.value || quoteBusy.value) return true
  if (activeMode.value === 'manual') return !title.value
  if (activeMode.value === 'ai-text') {
    return !aiText.value.trim() || !externalAiConsent.value
  }
  if (activeMode.value === 'document') {
    if (!documentFile.value) return true
    return documentRequiresAiConsent.value && !externalAiConsent.value
  }
  if (activeMode.value === 'screenshot') {
    return !screenshotFile.value || !externalAiConsent.value
  }
  return !webpageUrl.value || !externalAiConsent.value
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

function quoteInputChanged() {
  pendingQuote.value = null
  captureRequestKey.value = ''
  clearError()
}

async function selectMode(next: CaptureMode) {
  pendingQuote.value = null
  captureRequestKey.value = ''
  if (mode.value !== next) {
    externalAiConsent.value = false
  }
  mode.value = next
  applyScopeConsent(next)
  clearError()
  if (next === 'screenshot') {
    await nextTick()
    screenshotPasteInput.value?.focus()
  }
}

async function initialize() {
  void loadConsentPreferences()
  if (isEditing.value) {
    await loadItem()
  } else {
    restoreDraft()
  }
  draftReady.value = true
}

async function loadConsentPreferences() {
  try {
    const values = await getAiConsents()
    consentByScope.value = Object.fromEntries(values.map((value) => [value.scope, value]))
    consentPreferencesLoaded.value = true
    applyScopeConsent(mode.value)
  } catch {
    consentPreferencesLoaded.value = false
  }
}

function scopeForMode(value: CaptureMode): AiConsentScope | null {
  if (value === 'ai-text') return 'text'
  if (value === 'webpage') return 'webpage'
  if (value === 'document') return 'document'
  if (value === 'screenshot') return 'screenshot'
  return null
}

function applyScopeConsent(value: CaptureMode) {
  const scope = scopeForMode(value)
  if (!scope || !consentPreferencesLoaded.value) return
  syncingConsent.value = true
  externalAiConsent.value = consentByScope.value[scope]?.granted ?? false
  nextTick(() => {
    syncingConsent.value = false
  })
}

async function persistScopeConsent(granted: boolean) {
  const scope = scopeForMode(mode.value)
  if (!scope || !consentPreferencesLoaded.value || syncingConsent.value) return
  if ((consentByScope.value[scope]?.granted ?? false) === granted) return
  try {
    const updated = await updateAiConsent(scope, granted)
    consentByScope.value = { ...consentByScope.value, [scope]: updated }
  } catch {
    formError.value = t('capture.consentSaveFailed')
    syncingConsent.value = true
    externalAiConsent.value = !granted
    nextTick(() => {
      syncingConsent.value = false
    })
  }
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

function setDocument(file: File | null) {
  formError.value = ''
  pendingQuote.value = null
  captureRequestKey.value = ''
  if (!file) {
    documentFile.value = null
    return
  }
  if (file.size > 25 * 1024 * 1024) {
    documentFile.value = null
    formError.value = t('capture.documentTooLarge')
    return
  }
  documentFile.value = file
  externalAiConsent.value = false
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
  externalAiConsent.value = false
  const input = document.querySelector<HTMLInputElement>('#capture-document-file')
  if (input) input.value = ''
}

function setScreenshot(file: File | null) {
  pendingQuote.value = null
  captureRequestKey.value = ''
  if (screenshotPreviewUrl.value) {
    URL.revokeObjectURL(screenshotPreviewUrl.value)
  }
  screenshotFile.value = file
  screenshotPreviewUrl.value = file ? URL.createObjectURL(file) : ''
  externalAiConsent.value = false
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

async function saveImportedCapture(billingTaskId?: string) {
  clearError()
  const requestKey =
    captureRequestKey.value || (captureRequestKey.value = createIdempotencyKey('capture'))

  if (mode.value === 'ai-text') {
    if (!aiText.value.trim()) {
      formError.value = t('capture.aiTextRequired')
      return
    }
    saving.value = true
    try {
      await importText(
        aiText.value,
        externalAiConsent.value,
        note.value,
        billingTaskId,
        requestKey,
      )
      await router.replace('/library')
    } catch (caught) {
      formError.value = textImportError(caught)
    } finally {
      saving.value = false
    }
    return
  } else if (mode.value === 'webpage') {
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
    saving.value = true
    try {
      await importWebPage(
        parsed.toString(),
        externalAiConsent.value,
        billingTaskId,
        requestKey,
      )
      await router.replace('/library')
    } catch (caught) {
      formError.value = webpageImportError(caught)
    } finally {
      saving.value = false
    }
    return
  } else if (mode.value === 'document') {
    const file = documentFile.value
    if (!file) {
      formError.value = t('capture.documentUnsupported')
      return
    }
    saving.value = true
    try {
      await importDocument(
        file,
        externalAiConsent.value,
        note.value,
        billingTaskId,
        requestKey,
      )
      await router.replace('/library')
    } catch (caught) {
      formError.value = documentImportError(caught)
    } finally {
      saving.value = false
    }
    return
  } else {
    const file = screenshotFile.value
    if (!file) {
      formError.value = t('capture.screenshotMissing')
      return
    }
    saving.value = true
    try {
      await importScreenshot(
        file,
        externalAiConsent.value,
        note.value,
        billingTaskId,
        requestKey,
      )
      await router.replace('/library')
    } catch (caught) {
      formError.value = screenshotImportError(caught)
    } finally {
      saving.value = false
    }
    return
  }
}

async function prepareBillingQuote() {
  clearError()
  quoteBusy.value = true
  try {
    let preflightId: string | null = null
    let operation = ''
    let lines: Array<{ feature: string; quantity: number }> = []
    if (activeMode.value === 'ai-text') {
      if (!aiText.value.trim()) {
        formError.value = t('capture.aiTextRequired')
        return
      }
      operation = 'text_import'
      lines = [{ feature: 'ai_text', quantity: 1 }]
    } else if (activeMode.value === 'webpage') {
      operation = 'webpage_import'
      const preflight = await createBillingPreflight({
        kind: 'webpage',
        mediaType: 'text/html',
        sizeBytes: Math.max(1, webpageUrl.value.length),
        viewCount: 1,
      })
      preflightId = preflight.preflightId
      lines = [
        { feature: 'webpage_vision', quantity: 1 },
        { feature: 'classify', quantity: 1 },
      ]
    } else if (activeMode.value === 'document') {
      const file = documentFile.value
      if (!file) {
        formError.value = t('capture.documentUnsupported')
        return
      }
      operation = 'document_import'
      const preflight = await createBillingPreflight({
        kind: 'document',
        mediaType: file.type || 'application/octet-stream',
        sizeBytes: Math.max(1, file.size),
      })
      preflightId = preflight.preflightId
      lines = [
        { feature: 'document_extract', quantity: 1 },
        { feature: 'classify', quantity: 1 },
      ]
    } else {
      const file = screenshotFile.value
      if (!file) {
        formError.value = t('capture.screenshotMissing')
        return
      }
      operation = 'screenshot_import'
      const preflight = await createBillingPreflight({
        kind: 'screenshot',
        mediaType: file.type || 'image/png',
        sizeBytes: Math.max(1, file.size),
      })
      preflightId = preflight.preflightId
      lines = [
        { feature: 'screenshot_ocr', quantity: 1 },
        { feature: 'classify', quantity: 1 },
      ]
    }
    pendingQuote.value = await createBillingQuote({
      operation,
      preflightId,
      itemId: isEditing.value ? itemId.value : null,
      lines,
      useAutoCharge: true,
    })
  } catch (caught) {
    formError.value = billingError(caught)
  } finally {
    quoteBusy.value = false
  }
}

async function confirmQuoteAndSave() {
  const quote = pendingQuote.value
  if (!quote) return
  quoteBusy.value = true
  try {
    const task = await confirmBillingQuote(quote.quoteId, quote.maxPoints)
    pendingQuote.value = null
    await saveImportedCapture(task.id)
  } catch (caught) {
    formError.value = billingError(caught)
  } finally {
    quoteBusy.value = false
  }
}

function billingFeatureLabel(feature: string) {
  return t(`billing.feature.${feature}`)
}

function billingError(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'INSUFFICIENT_POINTS') return t('capture.insufficientPoints')
    if (caught.code === 'QUOTE_EXPIRED' || caught.code === 'BILLING_TASK_EXPIRED') {
      return t('capture.quoteExpired')
    }
    if (caught.code === 'BILLING_DISABLED' || caught.code === 'BILLING_BRIDGE_UNAVAILABLE') {
      return t('capture.billingUnavailable')
    }
  }
  return caught instanceof Error ? caught.message : t('capture.billingUnavailable')
}

function webpageImportError(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'WEB_PAGE_INVALID_URL') return t('capture.invalidUrl')
    if (caught.code === 'AI_EXTERNAL_CONSENT_REQUIRED') {
      return t('capture.webpageConsentRequired')
    }
    if (caught.code === 'AI_BILLING_REQUIRED') {
      return t('capture.webpageBillingUnavailable')
    }
    if (caught.code === 'WEB_PAGE_NO_TEXT') return t('capture.webpageNoText')
    if (caught.code === 'WEB_PAGE_ORGANIZATION_FAILED') {
      return t('capture.webpageOrganizationFailed')
    }
    if (caught.code === 'WEB_IMPORT_UNAVAILABLE') return t('capture.webpageUnavailable')
  }
  return caught instanceof Error ? caught.message : t('capture.webpageUnavailable')
}

function textImportError(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'TEXT_NO_CONTENT') return t('capture.aiTextNoContent')
    if (caught.code === 'TEXT_ORGANIZATION_FAILED') {
      return t('capture.aiTextOrganizationFailed')
    }
    if (caught.code === 'TEXT_IMPORT_UNAVAILABLE') {
      return t('capture.aiTextUnavailable')
    }
    if (caught.code === 'AI_EXTERNAL_CONSENT_REQUIRED') {
      return t('capture.aiTextConsentRequired')
    }
    if (caught.code === 'AI_BILLING_REQUIRED') {
      return t('capture.aiTextBillingUnavailable')
    }
  }
  return caught instanceof Error ? caught.message : t('capture.aiTextUnavailable')
}

function documentImportError(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'DOCUMENT_UNSUPPORTED') return t('capture.documentUnsupported')
    if (caught.code === 'DOCUMENT_TOO_LARGE') return t('capture.documentTooLarge')
    if (caught.code === 'UPLOAD_TOO_LARGE') return t('capture.documentTooLarge')
    if (caught.code === 'DOCUMENT_TOO_LONG') return t('capture.documentTooLong')
    if (caught.code === 'DOCUMENT_EMPTY' || caught.code === 'DOCUMENT_NO_TEXT') {
      return t('capture.documentNoText')
    }
    if (caught.code === 'DOCUMENT_PARSE_FAILED') return t('capture.documentParseFailed')
    if (caught.code === 'DOCUMENT_IMAGE_INVALID') {
      return t('capture.documentImageInvalid')
    }
    if (caught.code === 'DOCUMENT_ORGANIZATION_FAILED') {
      return t('capture.documentOrganizationFailed')
    }
    if (caught.code === 'DOCUMENT_IMPORT_UNAVAILABLE') {
      return t('capture.documentUnavailable')
    }
    if (caught.code === 'AI_EXTERNAL_CONSENT_REQUIRED') {
      return t('capture.documentConsentRequired')
    }
    if (caught.code === 'AI_BILLING_REQUIRED') {
      return t('capture.webpageBillingUnavailable')
    }
  }
  return caught instanceof Error ? caught.message : t('capture.documentUnavailable')
}

function screenshotImportError(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'SCREENSHOT_EMPTY' || caught.code === 'SCREENSHOT_NO_TEXT') {
      return t('capture.screenshotNoText')
    }
    if (caught.code === 'SCREENSHOT_TOO_LARGE' || caught.code === 'UPLOAD_TOO_LARGE') {
      return t('capture.screenshotTooLarge')
    }
    if (caught.code === 'SCREENSHOT_TOO_LONG') return t('capture.screenshotTooLong')
    if (caught.code === 'SCREENSHOT_UNSUPPORTED' || caught.code === 'SCREENSHOT_IMAGE_INVALID') {
      return t('capture.screenshotImageInvalid')
    }
    if (caught.code === 'SCREENSHOT_ORGANIZATION_FAILED') {
      return t('capture.screenshotOrganizationFailed')
    }
    if (caught.code === 'SCREENSHOT_IMPORT_UNAVAILABLE') {
      return t('capture.screenshotUnavailable')
    }
    if (caught.code === 'AI_EXTERNAL_CONSENT_REQUIRED') {
      return t('capture.screenshotConsentRequired')
    }
    if (caught.code === 'AI_BILLING_REQUIRED') {
      return t('capture.screenshotBillingUnavailable')
    }
  }
  return caught instanceof Error ? caught.message : t('capture.screenshotUnavailable')
}

async function save() {
  if (!isEditing.value && activeMode.value !== 'manual') {
    if (pendingQuote.value) {
      await confirmQuoteAndSave()
    } else {
      await prepareBillingQuote()
    }
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
watch(externalAiConsent, (granted) => {
  if (!syncingConsent.value) captureRequestKey.value = ''
  void persistScopeConsent(granted)
})
watch([webpageUrl, note], () => {
  captureRequestKey.value = ''
})

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
