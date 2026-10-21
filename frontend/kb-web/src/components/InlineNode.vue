<template>
  <template v-if="!mark">
    <template v-if="node.type === 'text'">{{ node.text }}</template>
    <br v-else-if="node.type === 'hardBreak'" />
  </template>
  <component :is="tag" v-else v-bind="attributes">
    <InlineNode
      v-if="nextMarkIndex < marks.length"
      :node="node"
      :mark-index="nextMarkIndex"
    />
    <template v-else>{{ node.text }}</template>
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RichTextNode } from '@/types/rich-text'

defineOptions({ name: 'InlineNode' })

const props = withDefaults(
  defineProps<{
    node: RichTextNode
    markIndex?: number
  }>(),
  {
    markIndex: 0,
  },
)

const marks = computed(() => props.node.marks ?? [])
const mark = computed(() => marks.value[props.markIndex])
const nextMarkIndex = computed(() => props.markIndex + 1)

const tag = computed(() => {
  switch (mark.value?.type) {
    case 'bold':
      return 'strong'
    case 'italic':
      return 'em'
    case 'underline':
      return 'u'
    case 'strike':
      return 's'
    case 'code':
      return 'code'
    case 'link':
      return 'a'
    default:
      return 'span'
  }
})

const attributes = computed<Record<string, string>>(() => {
  if (mark.value?.type !== 'link') {
    return {} as Record<string, string>
  }
  const href = String(mark.value.attrs?.href || '')
  if (!/^https?:\/\//i.test(href)) {
    return {} as Record<string, string>
  }
  return {
    href,
    target: '_blank',
    rel: 'noopener noreferrer',
  }
})
</script>

