<template>
  <article class="rich-text-view">
    <DocumentNode :node="document" />
  </article>
</template>

<script setup lang="ts">
import type { RichTextDocument } from '@/types/rich-text'
import DocumentNode from './DocumentNode.vue'

defineProps<{
  document: RichTextDocument
}>()
</script>

