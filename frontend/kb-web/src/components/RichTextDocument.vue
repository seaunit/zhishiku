<template>
  <article class="rich-text-view">
    <DocumentNode :node="document" @open-image="openImage" />
    <ImageLightbox
      v-if="selectedImage"
      :node="selectedImage"
      @close="selectedImage = undefined"
    />
  </article>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { RichTextDocument } from '@/types/rich-text'
import type { RichTextNode } from '@/types/rich-text'
import DocumentNode from './DocumentNode.vue'
import ImageLightbox from '@/features/knowledge/components/ImageLightbox.vue'

defineProps<{
  document: RichTextDocument
}>()

const selectedImage = ref<RichTextNode>()

function openImage(node: RichTextNode) {
  selectedImage.value = node
}
</script>

