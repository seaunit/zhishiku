<template>
  <button
    type="button"
    class="locale-switch"
    :aria-label="locale === 'zh-CN' ? t('common.switchToEnglish') : t('common.switchToChinese')"
    @click="toggleLocale"
  >
    {{ locale === 'zh-CN' ? 'EN' : '中文' }}
  </button>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { persistLocale } from '@/i18n'

const { t, locale } = useI18n()

function toggleLocale() {
  persistLocale(locale.value === 'zh-CN' ? 'en' : 'zh-CN')
}
</script>

