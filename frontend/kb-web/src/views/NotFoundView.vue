<template>
  <main class="standalone-message">
    <h1>404</h1>
    <p>{{ t('detail.notFound') }}</p>
    <RouterLink class="button primary" to="/">{{ t('detail.backToSearch') }}</RouterLink>
  </main>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

