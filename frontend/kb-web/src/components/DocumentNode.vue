<template>
  <template v-if="node.type === 'doc'">
    <DocumentNode
      v-for="(child, index) in node.content"
      :key="index"
      :node="child"
      @open-image="$emit('open-image', $event)"
    />
  </template>

  <p
    v-else-if="node.type === 'paragraph'"
    :style="{ textAlign: textAlign(node) }"
  >
    <template v-if="node.content?.length">
      <InlineNode v-for="(child, index) in node.content" :key="index" :node="child" />
    </template>
    <br v-else />
  </p>

  <component
    :is="headingTag"
    v-else-if="node.type === 'heading'"
    :style="{ textAlign: textAlign(node) }"
  >
    <InlineNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </component>

  <ul v-else-if="node.type === 'bulletList'">
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </ul>

  <ol
    v-else-if="node.type === 'orderedList'"
    :start="Number(node.attrs?.start || 1)"
  >
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </ol>

  <li v-else-if="node.type === 'listItem'">
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </li>

  <blockquote v-else-if="node.type === 'blockquote'">
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </blockquote>

  <pre v-else-if="node.type === 'codeBlock'"><code><template
    v-for="(child, index) in node.content"
    :key="index"
  >{{ child.text }}</template></code></pre>

  <hr v-else-if="node.type === 'horizontalRule'" />

  <figure
    v-else-if="node.type === 'image'"
    class="document-image"
    :class="`align-${String(node.attrs?.alignment || 'center')}`"
    :style="{ '--image-width': `${Number(node.attrs?.displayWidthPx || 720)}px` }"
  >
    <SafeImage
      :asset-id="String(node.attrs?.assetId || '')"
      :alt="String(node.attrs?.alt || '')"
      :decorative="Boolean(node.attrs?.decorative)"
      :preview-url="media.getPreview(String(node.attrs?.assetId || ''))"
      :aspect-ratio="Number(node.attrs?.aspectRatio || 1.5)"
      interactive
      @open="$emit('open-image', node)"
    />
    <figcaption v-if="node.attrs?.caption">
      {{ node.attrs.caption }}
    </figcaption>
  </figure>

  <RichTable v-else-if="node.type === 'table'" :node="node" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RichTextNode } from '@/types/rich-text'
import { useMediaStore } from '@/stores/media'
import InlineNode from './InlineNode.vue'
import SafeImage from './SafeImage.vue'
import RichTable from '@/features/knowledge/components/RichTable.vue'

defineOptions({ name: 'DocumentNode' })

defineEmits<{
  'open-image': [node: RichTextNode]
}>()

const props = defineProps<{
  node: RichTextNode
}>()

const media = useMediaStore()
const headingTag = computed(() => `h${Number(props.node.attrs?.level || 2)}`)

function textAlign(node: RichTextNode) {
  const value = node.attrs?.textAlign
  return value === 'center' || value === 'right' ? value : 'left'
}
</script>

