<template>
  <div class="editor-shell" :class="{ 'is-focused': focused }">
    <div class="editor-toolbar" role="toolbar" :aria-label="t('a11y.editorToolbar')">
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('heading', { level: 2 }) }"
        :aria-pressed="editor?.isActive('heading', { level: 2 }) || false"
        :title="t('editor.heading2')"
        :aria-label="t('editor.heading2')"
        @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        <Heading2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('heading', { level: 3 }) }"
        :aria-pressed="editor?.isActive('heading', { level: 3 }) || false"
        :title="t('editor.heading3')"
        :aria-label="t('editor.heading3')"
        @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
      >
        <Heading3 :size="18" />
      </button>
      <span class="toolbar-divider" aria-hidden="true" />
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('bold') }"
        :aria-pressed="editor?.isActive('bold') || false"
        :title="t('editor.bold')"
        :aria-label="t('editor.bold')"
        @click="editor?.chain().focus().toggleBold().run()"
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
        @click="editor?.chain().focus().toggleItalic().run()"
      >
        <Italic :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('underline') }"
        :aria-pressed="editor?.isActive('underline') || false"
        :title="t('editor.underline')"
        :aria-label="t('editor.underline')"
        @click="editor?.chain().focus().toggleUnderline().run()"
      >
        <Underline :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('strike') }"
        :aria-pressed="editor?.isActive('strike') || false"
        :title="t('editor.strike')"
        :aria-label="t('editor.strike')"
        @click="editor?.chain().focus().toggleStrike().run()"
      >
        <Strikethrough :size="18" />
      </button>
      <span class="toolbar-divider" aria-hidden="true" />
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('bulletList') }"
        :aria-pressed="editor?.isActive('bulletList') || false"
        :title="t('editor.bulletList')"
        :aria-label="t('editor.bulletList')"
        @click="editor?.chain().focus().toggleBulletList().run()"
      >
        <List :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('orderedList') }"
        :aria-pressed="editor?.isActive('orderedList') || false"
        :title="t('editor.orderedList')"
        :aria-label="t('editor.orderedList')"
        @click="editor?.chain().focus().toggleOrderedList().run()"
      >
        <ListOrdered :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('blockquote') }"
        :aria-pressed="editor?.isActive('blockquote') || false"
        :title="t('editor.blockquote')"
        :aria-label="t('editor.blockquote')"
        @click="editor?.chain().focus().toggleBlockquote().run()"
      >
        <Quote :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ active: editor?.isActive('codeBlock') }"
        :aria-pressed="editor?.isActive('codeBlock') || false"
        :title="t('editor.codeBlock')"
        :aria-label="t('editor.codeBlock')"
        @click="editor?.chain().focus().toggleCodeBlock().run()"
      >
        <Code2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :title="t('editor.horizontalRule')"
        :aria-label="t('editor.horizontalRule')"
        @click="editor?.chain().focus().setHorizontalRule().run()"
      >
        <Minus :size="18" />
      </button>
      <span class="toolbar-spacer" />
      <button
        type="button"
        class="tool-button image-tool"
        :disabled="uploading"
        :title="t('editor.insertImage')"
        :aria-label="t('editor.insertImage')"
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
        :aria-label="t('editor.imageInput')"
        @change="handleImageSelection"
      />
      <button
        type="button"
        class="tool-button"
        :disabled="!editor?.can().undo()"
        :title="t('editor.undo')"
        :aria-label="t('editor.undo')"
        @click="editor?.chain().focus().undo().run()"
      >
        <Undo2 :size="18" />
      </button>
      <button
        type="button"
        class="tool-button"
        :disabled="!editor?.can().redo()"
        :title="t('editor.redo')"
        :aria-label="t('editor.redo')"
        @click="editor?.chain().focus().redo().run()"
      >
        <Redo2 :size="18" />
      </button>
    </div>

    <EditorContent :editor="editor" class="editor-content" />

    <p v-if="uploadMessage" class="editor-upload-message" aria-live="polite">
      <LoaderCircle v-if="uploading" class="spin" :size="15" />
      <span>{{ uploadMessage }}</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import {
  Bold,
  Code2,
  Heading2,
  Heading3,
  ImagePlus,
  Italic,
  List,
  ListOrdered,
  LoaderCircle,
  Minus,
  Quote,
  Redo2,
  Strikethrough,
  Undo2,
} from 'lucide-vue-next'
import { onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getRichImageUpload, uploadRichImage } from '@/api/client'
import { createNodeId } from '@/lib/id'
import { useMediaStore } from '@/stores/media'
import type { RichImageUpload } from '@/types/api'
import type { RichTextDocument } from '@/types/rich-text'
import { RichImage } from '@/extensions/RichImage'

const props = defineProps<{
  modelValue: RichTextDocument
  itemId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: RichTextDocument]
  uploading: [value: boolean]
}>()

const { t } = useI18n()
const media = useMediaStore()
const imageInput = ref<HTMLInputElement>()
const focused = ref(false)
const uploading = ref(false)
const uploadMessage = ref('')

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [2, 3, 4] },
    }),
    Link.configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
      HTMLAttributes: {
        rel: 'noopener noreferrer',
        target: '_blank',
      },
    }),
    Underline,
    RichImage,
  ],
  editorProps: {
    attributes: {
      class: 'rich-editor',
      'aria-label': t('capture.bodyLabel'),
      spellcheck: 'true',
    },
  },
  onFocus: () => {
    focused.value = true
  },
  onBlur: () => {
    focused.value = false
  },
  onUpdate: ({ editor: currentEditor }) => {
    emit('update:modelValue', currentEditor.getJSON() as RichTextDocument)
  },
})

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

async function handleImageSelection(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !editor.value) {
    return
  }

  const allowed = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif'])
  if (!allowed.has(file.type.toLowerCase())) {
    uploadMessage.value = t('capture.uploadUnsupported')
    return
  }
  if (file.size > 25 * 1024 * 1024) {
    uploadMessage.value = t('capture.uploadTooLarge')
    return
  }

  uploading.value = true
  emit('uploading', true)
  uploadMessage.value = t('capture.uploadScanning')
  try {
    const initial = await uploadRichImage(file, props.itemId)
    const upload = await waitForImage(initial)

    if (upload.status === 'rejected' || upload.scanState === 'rejected') {
      uploadMessage.value = t('capture.uploadRejected')
      return
    }
    if (upload.status === 'expired') {
      uploadMessage.value = t('capture.uploadExpired')
      return
    }
    if (upload.status !== 'ready' && upload.status !== 'attached') {
      uploadMessage.value = t('capture.uploadFailed')
      return
    }

    media.setPreview(upload.uploadId, file)
    editor.value
      .chain()
      .focus()
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
  } catch (caught) {
    uploadMessage.value = caught instanceof Error ? caught.message : t('capture.uploadFailed')
  } finally {
    uploading.value = false
    emit('uploading', false)
  }
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

