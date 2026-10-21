<template>
  <div class="brand-mark" aria-label="识海">
    <span class="brand-symbol" aria-hidden="true">
      <Waves :size="20" :stroke-width="1.8" />
    </span>
    <span class="brand-copy">
      <strong>{{ t('common.appName') }}</strong>
      <small>{{ t('common.appTagline') }}</small>
    </span>
  </div>
</template>

<script setup lang="ts">
import { Waves } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

