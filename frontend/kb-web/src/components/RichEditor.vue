<template>
  <div
    class="editor-shell"
    :class="{ 'is-focused': focused }"
    @dragover.prevent
    @drop.prevent="handleNativeDrop"
  >
    <div
      class="editor-toolbar"
      role="toolbar"
      :aria-label="t('a11y.editorToolbar')"
      @pointerdown.prevent="rememberSelection"
    >
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('paragraph') }"
        :aria-pressed="editor?.isActive('paragraph') || false"
        :title="t('editor.paragraph')"
        :aria-label="t('editor.paragraph')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.setParagraph())"
      >
        <Pilcrow :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('heading', { level: 2 }) }"
        :aria-pressed="editor?.isActive('heading', { level: 2 }) || false"
        :title="t('editor.heading2')"
        :aria-label="t('editor.heading2')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleHeading({ level: 2 }))"
      >
        <Heading2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('heading', { level: 3 }) }"
        :aria-pressed="editor?.isActive('heading', { level: 3 }) || false"
        :title="t('editor.heading3')"
        :aria-label="t('editor.heading3')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleHeading({ level: 3 }))"
      >
        <Heading3 :size="18" />
      </button>
      <span class="toolbar-divider desktop-tool" aria-hidden="true" />
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('bold') }"
        :aria-pressed="editor?.isActive('bold') || false"
        :title="t('editor.bold')"
        :aria-label="t('editor.bold')"
        @pointerdown="rememberSelection"
        @click="toggleBold"
      >
        <Bold :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('italic') }"
        :aria-pressed="editor?.isActive('italic') || false"
        :title="t('editor.italic')"
        :aria-label="t('editor.italic')"
        @pointerdown="rememberSelection"
        @click="toggleItalic"
      >
        <Italic :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('underline') }"
        :aria-pressed="editor?.isActive('underline') || false"
        :title="t('editor.underline')"
        :aria-label="t('editor.underline')"
        @pointerdown="rememberSelection"
        @click="toggleUnderline"
      >
        <Underline :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('strike') }"
        :aria-pressed="editor?.isActive('strike') || false"
        :title="t('editor.strike')"
        :aria-label="t('editor.strike')"
        @pointerdown="rememberSelection"
        @click="toggleStrike"
      >
        <Strikethrough :size="18" />
      </button>
      <span class="toolbar-divider desktop-tool" aria-hidden="true" />
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('bulletList') }"
        :aria-pressed="editor?.isActive('bulletList') || false"
        :title="t('editor.bulletList')"
        :aria-label="t('editor.bulletList')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleBulletList())"
      >
        <List :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('orderedList') }"
        :aria-pressed="editor?.isActive('orderedList') || false"
        :title="t('editor.orderedList')"
        :aria-label="t('editor.orderedList')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleOrderedList())"
      >
        <ListOrdered :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('blockquote') }"
        :aria-pressed="editor?.isActive('blockquote') || false"
        :title="t('editor.blockquote')"
        :aria-label="t('editor.blockquote')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleBlockquote())"
      >
        <Quote :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('codeBlock') }"
        :aria-pressed="editor?.isActive('codeBlock') || false"
        :title="t('editor.codeBlock')"
        :aria-label="t('editor.codeBlock')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleCodeBlock())"
      >
        <Code2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :class="{ active: editor?.isActive('link') }"
        :aria-pressed="editor?.isActive('link') || false"
        :title="t('editor.insertLink')"
        :aria-label="t('editor.insertLink')"
        @pointerdown="rememberSelection"
        @click="openLinkComposer"
      >
        <Link2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button desktop-tool"
        :title="t('editor.horizontalRule')"
        :aria-label="t('editor.horizontalRule')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.setHorizontalRule())"
      >
        <Minus :size="18" />
      </button>
      <span class="toolbar-spacer desktop-tool" />
      <button
        type="button"
        class="tool-button image-tool"
        :disabled="uploading"
        :title="t('editor.insertImage')"
        :aria-label="t('editor.insertImage')"
        @pointerdown="rememberSelection"
        @click="imageInput?.click()"
      >
        <LoaderCircle v-if="uploading" class="spin" :size="18" />
        <ImagePlus v-else :size="18" />
      </button>
      <input
        ref="imageInput"
        class="sr-only"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/heic,image/heif"
        multiple
        :aria-label="t('editor.imageInput')"
        @change="handleImageSelection"
      />
      <button
        type="button"
        class="tool-button secondary-tool"
        :disabled="!editor?.can().undo()"
        :title="t('editor.undo')"
        :aria-label="t('editor.undo')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.undo())"
      >
        <Undo2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button secondary-tool"
        :disabled="!editor?.can().redo()"
        :title="t('editor.redo')"
        :aria-label="t('editor.redo')"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.redo())"
      >
        <Redo2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button mobile-more"
        :class="{ active: moreOpen }"
        :aria-expanded="moreOpen"
        aria-controls="editor-more-menu"
        :aria-label="t('editor.moreFormats')"
        @pointerdown="rememberSelection"
        @click="toggleMore"
      >
        <MoreHorizontal :size="20" />
      </button>
    </div>

    <div
      v-if="moreOpen"
      id="editor-more-menu"
      class="more-menu"
      role="menu"
      :aria-label="t('editor.moreFormats')"
      @pointerdown.prevent="rememberSelection"
    >
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.setParagraph())"
      >
        <Pilcrow :size="18" />
        <span>{{ t('editor.paragraph') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleHeading({ level: 3 }))"
      >
        <Heading3 :size="18" />
        <span>{{ t('editor.heading3') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleHeading({ level: 4 }))"
      >
        <Heading4 :size="18" />
        <span>{{ t('editor.heading4') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="toggleUnderline"
      >
        <Underline :size="18" />
        <span>{{ t('editor.underline') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="toggleStrike"
      >
        <Strikethrough :size="18" />
        <span>{{ t('editor.strike') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleBulletList())"
      >
        <List :size="18" />
        <span>{{ t('editor.bulletList') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleOrderedList())"
      >
        <ListOrdered :size="18" />
        <span>{{ t('editor.orderedList') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleBlockquote())"
      >
        <Quote :size="18" />
        <span>{{ t('editor.blockquote') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="toggleInlineCode"
      >
        <Code :size="18" />
        <span>{{ t('editor.inlineCode') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.toggleCodeBlock())"
      >
        <Code2 :size="18" />
        <span>{{ t('editor.codeBlock') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="openLinkComposer"
      >
        <Link2 :size="18" />
        <span>{{ t('editor.insertLink') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.setHorizontalRule())"
      >
        <Minus :size="18" />
        <span>{{ t('editor.horizontalRule') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.clearNodes().unsetAllMarks())"
      >
        <RemoveFormatting :size="18" />
        <span>{{ t('editor.clearFormatting') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        @pointerdown="rememberSelection"
        @click="insertTable"
      >
        <Table2 :size="18" />
        <span>{{ t('editor.insertTable') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :disabled="!isInsideTable"
        @pointerdown="rememberSelection"
        @click="changeTable('add-row')"
      >
        <Rows3 :size="18" />
        <span>{{ t('editor.addRow') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :disabled="!isInsideTable"
        @pointerdown="rememberSelection"
        @click="changeTable('delete-row')"
      >
        <Trash2 :size="18" />
        <span>{{ t('editor.deleteRow') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :disabled="!isInsideTable"
        @pointerdown="rememberSelection"
        @click="changeTable('add-column')"
      >
        <Columns3 :size="18" />
        <span>{{ t('editor.addColumn') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :disabled="!isInsideTable"
        @pointerdown="rememberSelection"
        @click="changeTable('delete-column')"
      >
        <Trash2 :size="18" />
        <span>{{ t('editor.deleteColumn') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :aria-pressed="pasteAsPlainText"
        @click="pasteAsPlainText = !pasteAsPlainText"
      >
        <Eraser :size="18" />
        <span>{{ t('editor.pastePlainText') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :disabled="!editor?.can().undo()"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.undo())"
      >
        <Undo2 :size="18" />
        <span>{{ t('editor.undo') }}</span>
      </button>
      <button
        type="button"
        class="more-menu-item"
        :disabled="!editor?.can().redo()"
        @pointerdown="rememberSelection"
        @click="runCommand((chain) => chain.redo())"
      >
        <Redo2 :size="18" />
        <span>{{ t('editor.redo') }}</span>
      </button>
    </div>

    <form
      v-if="linkOpen"
      class="link-composer"
      role="dialog"
      :aria-label="t('editor.linkDialog')"
      @submit.prevent="applyLink"
    >
      <label for="rich-text-link">{{ t('editor.linkAddress') }}</label>
      <div class="link-composer-row">
        <input
          id="rich-text-link"
          v-model.trim="linkInput"
          class="input"
          type="url"
          inputmode="url"
          autocomplete="url"
          :placeholder="t('editor.linkPlaceholder')"
        />
        <button type="submit" class="button primary">{{ t('editor.applyLink') }}</button>
        <button
          v-if="editor?.isActive('link')"
          type="button"
          class="button secondary"
          @click="removeLink"
        >
          {{ t('editor.removeLink') }}
        </button>
        <button type="button" class="text-button" @click="closeLinkComposer">
          {{ t('common.cancel') }}
        </button>
      </div>
      <p v-if="linkError" class="field-error" role="alert">{{ linkError }}</p>
    </form>

    <EditorContent :editor="editor" class="editor-content" />

    <div
      v-if="pasteReport"
      class="paste-report"
      role="status"
      data-testid="paste-report"
    >
      <div>
        <strong>{{ pasteReportHeading }}</strong>
        <p>{{ pasteReportSummary }}</p>
      </div>
      <button
        type="button"
        class="text-button"
        :aria-label="t('editor.dismissPasteReport')"
        @click="clearPasteReport"
      >
        {{ t('common.close') }}
      </button>
    </div>

    <p v-if="uploadMessage" class="editor-upload-message" aria-live="polite">
      <LoaderCircle v-if="uploading" class="spin" :size="15" />
      <span>{{ uploadMessage }}</span>
      <button
        v-if="failedImage"
        type="button"
        class="text-button"
        @click="retryFailedImage"
      >
        {{ t('common.retry') }}
      </button>
    </p>
  </div>
</template>

<script setup lang="ts">
import type { ChainedCommands } from '@tiptap/core'
import Link from '@tiptap/extension-link'
import TiptapUnderline from '@tiptap/extension-underline'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import {
  Bold,
  Code,
  Code2,
  Columns3,
  Eraser,
  Heading2,
  Heading3,
  Heading4,
  ImagePlus,
  Italic,
  Link2,
  List,
  ListOrdered,
  LoaderCircle,
  Minus,
  MoreHorizontal,
  Pilcrow,
  Quote,
  Redo2,
  RemoveFormatting,
  Rows3,
  Strikethrough,
  Table2,
  Trash2,
  Underline,
  Undo2,
} from 'lucide-vue-next'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getRichImageUpload, uploadRichImage } from '@/api/client'
import { RichImage } from '@/extensions/RichImage'
import { validateRichImageFile } from '@/features/editor/image/image-file'
import {
  convertClipboardHtml,
  plainTextToRichTextDocument,
  sanitizeExternalUrl,
  type PasteConversionReport,
} from '@/features/editor/paste/html-to-rich-text'
import {
  addTableColumn,
  addTableRow,
  createTableNode,
  deleteTableColumn,
  deleteTableRow,
} from '@/features/editor/table/table-model'
import { RichTableExtensions } from '@/features/editor/table/TableExtensions'
import { createNodeId } from '@/lib/id'
import { useMediaStore } from '@/stores/media'
import type { BodySourceRequest, RichImageUpload } from '@/types/api'
import type { RichTextDocument, RichTextNode } from '@/types/rich-text'

const props = defineProps<{
  modelValue: RichTextDocument
  itemId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: RichTextDocument]
  uploading: [value: boolean]
  'source-report': [value: BodySourceRequest | null]
}>()

const { t } = useI18n()
const media = useMediaStore()
const imageInput = ref<HTMLInputElement>()
const focused = ref(false)
const uploading = ref(false)
const uploadMessage = ref('')
const failedImage = ref<File>()
const failedPosition = ref<number>()
const moreOpen = ref(false)
const linkOpen = ref(false)
const linkInput = ref('')
const linkError = ref('')
const pasteAsPlainText = ref(false)
const pasteReport = ref<PasteConversionReport>()
const savedSelection = ref<{ from: number; to: number }>()
const savedMarks = ref({
  bold: false,
  italic: false,
  underline: false,
  strike: false,
  code: false,
})
const tableVersion = ref(0)

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [2, 3, 4] },
    }),
    Link.configure({
      openOnClick: false,
      autolink: false,
      linkOnPaste: false,
      HTMLAttributes: {
        rel: 'noopener noreferrer',
        target: '_blank',
      },
    }),
    TiptapUnderline,
    RichImage,
    ...RichTableExtensions,
  ],
  editorProps: {
    attributes: {
      class: 'rich-editor',
      'aria-label': t('capture.bodyLabel'),
      spellcheck: 'true',
    },
    handlePaste: (_view, event) => {
      void handlePaste(event)
      return true
    },
  },
  onFocus: () => {
    focused.value = true
  },
  onBlur: () => {
    focused.value = false
  },
  onUpdate: ({ editor: currentEditor }) => {
    tableVersion.value += 1
    emit('update:modelValue', currentEditor.getJSON() as RichTextDocument)
  },
})

const isInsideTable = computed(() => {
  void tableVersion.value
  return Boolean(tableContext())
})

const pasteReportHeading = computed(() => {
  if (!pasteReport.value) {
    return ''
  }
  return t('editor.pasteReportHeading', {
    source: t(`editor.pasteSource.${pasteReport.value.sourceKind}`),
  })
})

const pasteReportSummary = computed(() => {
  if (!pasteReport.value) {
    return ''
  }
  const preserved = pasteReport.value.preserved
    .slice(0, 6)
    .map((feature) => t(`editor.formatFeature.${feature}`))
    .join('、')
  const removed = pasteReport.value.removed.join('、')
  return t('editor.pasteReportSummary', {
    preserved: preserved || t('editor.formatFeature.paragraph'),
    removed: removed || t('editor.none'),
  })
})

function rememberSelection() {
  if (!editor.value) {
    return
  }
  const state = editor.value.state
  const markAtSelection = (name: string) =>
    editor.value?.isActive(name) ||
    Boolean(state.storedMarks?.some((mark) => mark.type.name === name)) ||
    Boolean(state.selection.$from.marks().some((mark) => mark.type.name === name))
  savedSelection.value = {
    from: state.selection.from,
    to: state.selection.to,
  }
  savedMarks.value = {
    bold: markAtSelection('bold'),
    italic: markAtSelection('italic'),
    underline: markAtSelection('underline'),
    strike: markAtSelection('strike'),
    code: markAtSelection('code'),
  }
}

function focusedChain(): ChainedCommands | null {
  if (!editor.value) {
    return null
  }
  const range = savedSelection.value
  const chain = editor.value.chain().focus()
  if (!range) {
    return chain
  }
  return chain.setTextSelection(range)
}

function runCommand(command: (chain: ChainedCommands) => void) {
  const chain = focusedChain()
  if (!chain) {
    return
  }
  command(chain)
  chain.run()
  closePanels()
}

function toggleBold() {
  const active = savedMarks.value.bold
  runCommand((chain) => (active ? chain.unsetBold() : chain.setBold()))
}

function toggleItalic() {
  const active = savedMarks.value.italic
  runCommand((chain) => (active ? chain.unsetItalic() : chain.setItalic()))
}

function toggleUnderline() {
  const active = savedMarks.value.underline
  runCommand((chain) => (active ? chain.unsetUnderline() : chain.setUnderline()))
}

function toggleStrike() {
  const active = savedMarks.value.strike
  runCommand((chain) => (active ? chain.unsetStrike() : chain.setStrike()))
}

function toggleInlineCode() {
  const active = savedMarks.value.code
  runCommand((chain) => (active ? chain.unsetCode() : chain.setCode()))
}

function closePanels() {
  moreOpen.value = false
}

function toggleMore() {
  rememberSelection()
  moreOpen.value = !moreOpen.value
}

function openLinkComposer() {
  rememberSelection()
  linkError.value = ''
  linkInput.value = String(editor.value?.getAttributes('link').href || '')
  linkOpen.value = true
  moreOpen.value = false
}

function closeLinkComposer() {
  linkOpen.value = false
  linkError.value = ''
  editor.value?.commands.focus()
}

function applyLink() {
  const href = sanitizeExternalUrl(linkInput.value)
  if (!href) {
    linkError.value = t('editor.linkInvalid')
    return
  }

  const chain = focusedChain()
  if (!chain) {
    return
  }
  const range = savedSelection.value
  if (range && range.from === range.to) {
    chain.insertContent({
      type: 'text',
      text: href,
      marks: [{ type: 'link', attrs: { href } }],
    })
  } else {
    chain.setLink({ href })
  }
  chain.run()
  linkOpen.value = false
}

function removeLink() {
  runCommand((chain) => chain.extendMarkRange('link').unsetLink())
  linkOpen.value = false
}

function clearPasteReport() {
  pasteReport.value = undefined
  emit('source-report', null)
}

async function handlePaste(event: ClipboardEvent) {
  const clipboard = event.clipboardData
  if (!clipboard) {
    return
  }
  event.preventDefault()

  const files = imageFiles(clipboard)
  if (files.length) {
    await uploadFiles(files)
    return
  }

  const text = clipboard.getData('text/plain')
  const html = clipboard.getData('text/html')
  const sourceUrl =
    clipboard.getData('text/uri-list').split(/\r?\n/).find(Boolean) || undefined

  if (!text && !html) {
    return
  }

  const target = focusedChain()
  if (!target) {
    return
  }

  if (pasteAsPlainText.value || !html) {
    target.insertContent(plainTextToRichTextDocument(text)).run()
    return
  }

  const conversion = convertClipboardHtml({ html, text, sourceUrl })
  target.insertContent(conversion.document).run()
  pasteReport.value = conversion.report
  emit('source-report', {
    kind: conversion.report.sourceKind,
    sourceUrl: conversion.report.sourceUrl,
    capturedAt: new Date().toISOString(),
  })
}

function imageFiles(data: DataTransfer | null): File[] {
  if (!data) {
    return []
  }
  const files = Array.from(data.files)
  if (!files.length) {
    for (const item of Array.from(data.items)) {
      if (item.kind === 'file') {
        const file = item.getAsFile()
        if (file) {
          files.push(file)
        }
      }
    }
  }
  return files.filter((file) => file.type.startsWith('image/'))
}

function handleNativeDrop(event: DragEvent) {
  const files = imageFiles(event.dataTransfer)
  if (!files.length || !editor.value) {
    return
  }
  const position = editor.value.view.posAtCoords({
    left: event.clientX,
    top: event.clientY,
  })?.pos
  void uploadFiles(files, position)
}

async function handleImageSelection(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  rememberSelection()
  await uploadFiles(files)
}

async function uploadFiles(files: File[], position?: number) {
  if (!editor.value || !files.length) {
    return
  }
  for (const file of files) {
    await uploadFile(file, position)
  }
}

async function uploadFile(file: File, position?: number) {
  const validation = validateRichImageFile(file)
  if (!validation.ok) {
    uploadMessage.value =
      validation.code === 'too-large'
        ? t('capture.uploadTooLarge')
        : t('capture.uploadUnsupported')
    return
  }

  failedImage.value = undefined
  failedPosition.value = position
  uploading.value = true
  emit('uploading', true)
  uploadMessage.value = t('capture.uploadScanning')
  try {
    const initial = await uploadRichImage(file, props.itemId)
    const upload = await waitForImage(initial)

    if (upload.status === 'rejected' || upload.scanState === 'rejected') {
      uploadMessage.value = t('capture.uploadRejected')
      failedImage.value = file
      return
    }
    if (upload.status === 'expired') {
      uploadMessage.value = t('capture.uploadExpired')
      failedImage.value = file
      return
    }
    if (upload.status !== 'ready' && upload.status !== 'attached') {
      uploadMessage.value = t('capture.uploadFailed')
      failedImage.value = file
      return
    }

    media.setPreview(upload.uploadId, file)
    if (!editor.value) {
      return
    }
    const chain = editor.value.chain().focus()
    if (position !== undefined) {
      chain.setTextSelection(position)
    } else if (savedSelection.value) {
      chain.setTextSelection(savedSelection.value)
    }
    chain
      .insertContent({
        type: 'image',
        attrs: {
          assetId: upload.uploadId,
          uploadId: upload.uploadId,
          nodeId: createNodeId(),
          alt: file.name || t('capture.imageAlt'),
          caption: '',
          displayWidthPx: 720,
          aspectRatio: Number(upload.aspectRatio) || 1.5,
          alignment: 'center',
          decorative: false,
        },
      })
      .run()
    uploadMessage.value = ''
    failedImage.value = undefined
  } catch (caught) {
    uploadMessage.value = caught instanceof Error ? caught.message : t('capture.uploadFailed')
    failedImage.value = file
  } finally {
    uploading.value = false
    emit('uploading', false)
  }
}

async function retryFailedImage() {
  if (!failedImage.value) {
    return
  }
  const file = failedImage.value
  const position = failedPosition.value
  failedImage.value = undefined
  await uploadFile(file, position)
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function waitForImage(upload: RichImageUpload): Promise<RichImageUpload> {
  let current = upload
  for (let attempt = 0; attempt < 30; attempt += 1) {
    if (current.status !== 'staged' && current.status !== 'scanning') {
      return current
    }
    uploadMessage.value = t('capture.uploadScanning')
    await wait(1000)
    current = await getRichImageUpload(current.uploadId)
  }
  return current
}

function insertTable() {
  rememberSelection()
  runCommand((chain) => chain.insertContent(createTableNode(3, 3)))
}

function tableContext():
  | {
      depth: number
      position: number
      node: { nodeSize: number; toJSON: () => RichTextNode }
      rowIndex: number
      columnIndex: number
    }
  | undefined {
  if (!editor.value) {
    return
  }
  const { $from } = editor.value.state.selection
  for (let depth = $from.depth; depth > 0; depth -= 1) {
    const node = $from.node(depth)
    if (node.type.name === 'table') {
      return {
        depth,
        position: $from.before(depth),
        node,
        rowIndex: Math.max(0, $from.index(depth)),
        columnIndex: Math.max(0, $from.index(depth + 1)),
      }
    }
  }
  return undefined
}

function changeTable(
  action: 'add-row' | 'delete-row' | 'add-column' | 'delete-column',
) {
  const context = tableContext()
  if (!editor.value || !context) {
    return
  }

  const source = context.node.toJSON()
  let updated = source
  if (action === 'add-row') {
    updated = addTableRow(source, context.rowIndex)
  } else if (action === 'delete-row') {
    updated = deleteTableRow(source, context.rowIndex)
  } else if (action === 'add-column') {
    updated = addTableColumn(source, context.columnIndex)
  } else {
    updated = deleteTableColumn(source, context.columnIndex)
  }

  const replacement = editor.value.schema.nodeFromJSON(updated)
  const transaction = editor.value.state.tr.replaceWith(
    context.position,
    context.position + context.node.nodeSize,
    replacement,
  )
  editor.value.view.dispatch(transaction.scrollIntoView())
  tableVersion.value += 1
  closePanels()
}

watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) {
      return
    }
    const current = JSON.stringify(editor.value.getJSON())
    const next = JSON.stringify(value)
    if (current !== next) {
      editor.value.commands.setContent(value, false)
    }
  },
  { deep: true },
)

onBeforeUnmount(() => {
  editor.value?.destroy()
  media.clearPreviews()
})
</script>
