<template>
  <template v-if="node.type === 'doc'">
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
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
    />
    <figcaption v-if="node.attrs?.caption">
      {{ node.attrs.caption }}
    </figcaption>
  </figure>

  <table v-else-if="node.type === 'table'" class="document-table">
    <thead v-if="node.content?.[0]">
      <DocumentNode :node="node.content[0]" />
    </thead>
    <tbody>
      <DocumentNode
        v-for="(child, index) in node.content?.slice(1) || []"
        :key="index"
        :node="child"
      />
    </tbody>
  </table>

  <tr v-else-if="node.type === 'tableHeaderRow' || node.type === 'tableBodyRow'">
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </tr>

  <th
    v-else-if="node.type === 'tableHeader'"
    :colspan="Number(node.attrs?.colspan || 1)"
    :rowspan="Number(node.attrs?.rowspan || 1)"
  >
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </th>

  <td
    v-else-if="node.type === 'tableCell'"
    :colspan="Number(node.attrs?.colspan || 1)"
    :rowspan="Number(node.attrs?.rowspan || 1)"
  >
    <DocumentNode v-for="(child, index) in node.content" :key="index" :node="child" />
  </td>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RichTextNode } from '@/types/rich-text'
import { useMediaStore } from '@/stores/media'
import InlineNode from './InlineNode.vue'
import SafeImage from './SafeImage.vue'

defineOptions({ name: 'DocumentNode' })

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

