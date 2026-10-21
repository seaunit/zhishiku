<template>
  <section class="page trash-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('trash.title') }}</h1>
        <p>{{ t('trash.subtitle') }}</p>
      </div>
    </header>

    <section class="panel trash-panel">
      <div class="empty-state">
        <Trash2 :size="22" />
        <h3>{{ t('trash.emptyTitle') }}</h3>
        <p>{{ t('trash.emptyBody') }}</p>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { Trash2 } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>
