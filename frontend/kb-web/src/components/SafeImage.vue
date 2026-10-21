<template>
  <div class="safe-image" :style="aspectStyle">
    <button
      v-if="src && interactive && !decorative"
      type="button"
      class="safe-image-open"
      :aria-label="t('detail.openImage', { name: accessibleAlt })"
      @click="$emit('open')"
    >
      <img
        :src="src"
        :alt="alt"
        loading="lazy"
        decoding="async"
        @error="handleImageError"
      />
    </button>
    <img
      v-else-if="src"
      :src="src"
      :alt="decorative ? '' : alt"
      :aria-hidden="decorative ? 'true' : undefined"
      loading="lazy"
      decoding="async"
      @error="handleImageError"
    />
    <div v-else-if="loading" class="image-state" role="status">
      <LoaderCircle class="spin" :size="22" />
      <span>{{ t('common.loading') }}</span>
    </div>
    <div v-else class="image-state">
      <ImageOff :size="22" />
      <span>{{ t('common.imageUnavailable') }}</span>
      <button type="button" class="text-button" @click.stop="loadImage">
        {{ t('common.retry') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ImageOff, LoaderCircle } from 'lucide-vue-next'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { imageContentUrl } from '@/api/client'
import { isUuid } from '@/lib/id'

const props = withDefaults(
  defineProps<{
    assetId: string
    alt?: string
    decorative?: boolean
    previewUrl?: string
    aspectRatio?: number
    interactive?: boolean
  }>(),
  {
    alt: '',
    decorative: false,
    previewUrl: undefined,
    aspectRatio: 1.5,
    interactive: false,
  },
)

defineEmits<{
  open: []
}>()

const { t } = useI18n()
const src = ref('')
const loading = ref(false)
let generatedUrl = ''

const accessibleAlt = computed(() => props.alt.trim() || t('common.image'))

const aspectStyle = computed(() => ({
  aspectRatio: String(Math.max(0.01, Math.min(100, props.aspectRatio))),
}))

function releaseGeneratedUrl() {
  if (generatedUrl) {
    URL.revokeObjectURL(generatedUrl)
    generatedUrl = ''
  }
}

async function loadImage() {
  releaseGeneratedUrl()
  src.value = ''

  if (props.previewUrl?.startsWith('blob:')) {
    src.value = props.previewUrl
    return
  }

  if (!isUuid(props.assetId)) {
    return
  }

  loading.value = true
  try {
    const response = await fetch(imageContentUrl(props.assetId), {
      credentials: 'include',
      headers: { Accept: 'image/*' },
    })
    if (!response.ok) {
      return
    }

    const mediaType = response.headers.get('content-type') || ''
    if (!mediaType.startsWith('image/')) {
      return
    }

    const blob = await response.blob()
    if (!blob.type.startsWith('image/') || blob.size > 25 * 1024 * 1024) {
      return
    }

    generatedUrl = URL.createObjectURL(blob)
    src.value = generatedUrl
  } catch {
    src.value = ''
  } finally {
    loading.value = false
  }
}

function handleImageError() {
  releaseGeneratedUrl()
  src.value = ''
}

watch(
  () => [props.assetId, props.previewUrl],
  () => {
    void loadImage()
  },
  { immediate: true },
)

onBeforeUnmount(releaseGeneratedUrl)
</script>

