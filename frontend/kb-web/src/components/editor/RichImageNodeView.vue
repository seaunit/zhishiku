<template>
  <NodeViewWrapper
    as="figure"
    class="editor-image"
    :class="[`align-${node.attrs.alignment}`, { selected }]"
    :style="{ '--image-width': `${node.attrs.displayWidthPx}px` }"
  >
    <SafeImage
      :asset-id="node.attrs.assetId"
      :alt="node.attrs.alt"
      :decorative="node.attrs.decorative"
      :preview-url="media.getPreview(node.attrs.assetId)"
      :aspect-ratio="node.attrs.aspectRatio"
    />
    <figcaption v-if="node.attrs.caption">{{ node.attrs.caption }}</figcaption>
  </NodeViewWrapper>
</template>

<script setup lang="ts">
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { computed } from 'vue'
import { useMediaStore } from '@/stores/media'
import SafeImage from '@/components/SafeImage.vue'

const props = defineProps(nodeViewProps)
const media = useMediaStore()
const node = computed(() => props.node)
const selected = computed(() => props.selected)
</script>

