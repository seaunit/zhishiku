<template>
  <NodeViewWrapper
    as="figure"
    class="editor-image"
    :class="[`align-${node.attrs.alignment}`, { selected }]"
    :style="{ '--image-width': `${node.attrs.displayWidthPx}px` }"
    contenteditable="false"
    @pointerdown.stop
  >
    <SafeImage
      :asset-id="node.attrs.assetId"
      :alt="node.attrs.alt"
      :decorative="node.attrs.decorative"
      :preview-url="media.getPreview(node.attrs.assetId)"
      :aspect-ratio="node.attrs.aspectRatio"
    />

    <div class="image-node-controls">
      <label>
        <span>{{ t('editor.imageAlt') }}</span>
        <input
          class="input"
          type="text"
          maxlength="500"
          :value="node.attrs.alt"
          @input="updateText('alt', $event)"
        />
      </label>
      <label>
        <span>{{ t('editor.imageCaption') }}</span>
        <input
          class="input"
          type="text"
          maxlength="500"
          :value="node.attrs.caption"
          @input="updateText('caption', $event)"
        />
      </label>
      <label>
        <span>{{ t('editor.imageWidth') }}</span>
        <input
          class="input"
          type="number"
          min="80"
          max="1600"
          step="20"
          :value="node.attrs.displayWidthPx"
          @input="updateNumber('displayWidthPx', $event)"
        />
      </label>
      <label>
        <span>{{ t('editor.imageAlignment') }}</span>
        <select class="input" :value="node.attrs.alignment" @change="updateAlignment">
          <option value="left">{{ t('editor.alignLeft') }}</option>
          <option value="center">{{ t('editor.alignCenter') }}</option>
          <option value="right">{{ t('editor.alignRight') }}</option>
        </select>
      </label>
      <label class="checkbox-row">
        <input
          type="checkbox"
          :checked="node.attrs.decorative"
          @change="updateDecorative"
        />
        <span>{{ t('editor.imageDecorative') }}</span>
      </label>
      <button
        type="button"
        class="button secondary image-remove"
        :aria-label="t('editor.removeImage')"
        @click="deleteNode"
      >
        <Trash2 :size="17" />
        <span>{{ t('editor.removeImage') }}</span>
      </button>
    </div>

    <figcaption v-if="node.attrs.caption">{{ node.attrs.caption }}</figcaption>
  </NodeViewWrapper>
</template>

<script setup lang="ts">
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { Trash2 } from 'lucide-vue-next'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SafeImage from '@/components/SafeImage.vue'
import { useMediaStore } from '@/stores/media'

const props = defineProps(nodeViewProps)
const { t } = useI18n()
const media = useMediaStore()
const node = computed(() => props.node)
const selected = computed(() => props.selected)

function updateText(attribute: 'alt' | 'caption', event: Event) {
  props.updateAttributes({
    [attribute]: (event.target as HTMLInputElement).value,
  })
}

function updateNumber(attribute: 'displayWidthPx', event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  props.updateAttributes({
    [attribute]: Math.max(80, Math.min(1600, value || 720)),
  })
}

function updateAlignment(event: Event) {
  props.updateAttributes({
    alignment: (event.target as HTMLSelectElement).value,
  })
}

function updateDecorative(event: Event) {
  props.updateAttributes({
    decorative: (event.target as HTMLInputElement).checked,
  })
}
</script>
