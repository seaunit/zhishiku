import { mergeAttributes, Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import RichImageNodeView from '@/components/editor/RichImageNodeView.vue'

export const RichImage = Node.create({
  name: 'image',
  group: 'block',
  atom: true,
  draggable: true,
  selectable: true,

  addAttributes() {
    return {
      assetId: { default: null },
      uploadId: { default: null },
      nodeId: { default: null },
      alt: { default: '' },
      caption: { default: '' },
      displayWidthPx: { default: 720 },
      aspectRatio: { default: 1.5 },
      alignment: { default: 'center' },
      decorative: { default: false },
    }
  },

  parseHTML() {
    return [{ tag: 'img[data-asset-id]' }]
  },

  renderHTML({ HTMLAttributes }) {
    const { assetId, alt, ...rest } = HTMLAttributes
    return [
      'img',
      mergeAttributes(rest, {
        'data-asset-id': assetId,
        alt: alt || '',
      }),
    ]
  },

  renderText({ node }) {
    return String(node.attrs.alt || node.attrs.caption || '')
  },

  addNodeView() {
    return VueNodeViewRenderer(RichImageNodeView)
  },
})

