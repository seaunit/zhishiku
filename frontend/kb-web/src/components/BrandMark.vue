<template>
  <div class="brand-mark" :class="{ compact }" aria-label="识海">
    <span class="brand-symbol" aria-hidden="true">
      <Waves :size="compact ? 22 : 20" :stroke-width="1.6" />
    </span>
    <span v-if="!compact" class="brand-copy">
      <strong>{{ t('common.appName') }}</strong>
      <small>{{ t('common.appTagline') }}</small>
    </span>
  </div>
</template>

<script setup lang="ts">
import { Waves } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })
const { t } = useI18n()
</script>

