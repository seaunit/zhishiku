<template>
  <section class="page settings-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('settings.title') }}</h1>
        <p>{{ t('settings.subtitle') }}</p>
      </div>
    </header>

    <section class="settings-layout">
      <aside class="settings-index" :aria-label="t('settings.sectionsAria')">
        <button class="settings-link is-active" type="button">{{ t('settings.aiNav') }}</button>
        <button class="settings-link" type="button">{{ t('settings.languageNav') }}</button>
        <button class="settings-link" type="button">{{ t('settings.privacyNav') }}</button>
      </aside>

      <section class="panel settings-panel">
        <div class="settings-section">
          <div class="settings-copy">
            <h2>{{ t('settings.authTitle') }}</h2>
            <p>{{ t('settings.authDescription') }}</p>
          </div>
          <div v-if="loading" class="authorization-loading" aria-busy="true">
            {{ t('settings.authLoading') }}
          </div>
          <div v-else class="authorization-list">
            <label v-for="row in consentRows" :key="row.scope" class="authorization-row">
              <span>
                <strong>{{ t(row.title) }}</strong>
                <small>{{ t(row.description) }}</small>
              </span>
              <input
                type="checkbox"
                :checked="consents[row.scope]?.granted ?? false"
                :disabled="busyScope === row.scope"
                :aria-label="t(row.title)"
                @change="toggleConsent(row.scope, $event)"
              />
            </label>
          </div>
          <p v-if="consentError" class="form-error" role="alert">{{ consentError }}</p>
          <p v-else-if="consentNotice" class="success-message" role="status">
            {{ consentNotice }}
          </p>
        </div>

        <div class="settings-section compact-setting">
          <div class="settings-copy">
            <h2>{{ t('settings.languageTitle') }}</h2>
            <p>{{ t('settings.languageDescription') }}</p>
          </div>
          <div class="setting-control">
            <select :value="locale" @change="changeLocale">
              <option value="zh-CN">中文</option>
              <option value="en">English</option>
            </select>
          </div>
        </div>

        <div class="settings-section compact-setting">
          <div class="settings-copy">
            <h2>{{ t('settings.retentionTitle') }}</h2>
            <p>{{ t('settings.retentionDescription') }}</p>
          </div>
          <div class="setting-value">{{ t('settings.retentionValue') }}</div>
        </div>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getAiConsents, updateAiConsent } from '@/api/client'
import { persistLocale } from '@/i18n'
import type { AiConsent, AiConsentScope } from '@/types/api'

const { t, locale } = useI18n()
const loading = ref(true)
const busyScope = ref<AiConsentScope | ''>('')
const consentError = ref('')
const consentNotice = ref('')
const consents = ref<Partial<Record<AiConsentScope, AiConsent>>>({})

const consentRows: Array<{
  scope: AiConsentScope
  title: string
  description: string
}> = [
  {
    scope: 'text',
    title: 'settings.textTitle',
    description: 'settings.textDescription',
  },
  {
    scope: 'webpage',
    title: 'settings.webpageTitle',
    description: 'settings.webpageDescription',
  },
  {
    scope: 'document',
    title: 'settings.documentTitle',
    description: 'settings.documentDescription',
  },
  {
    scope: 'screenshot',
    title: 'settings.screenshotTitle',
    description: 'settings.screenshotDescription',
  },
  {
    scope: 'search',
    title: 'settings.searchTitle',
    description: 'settings.searchDescription',
  },
]

async function load() {
  loading.value = true
  consentError.value = ''
  try {
    const values = await getAiConsents()
    consents.value = Object.fromEntries(values.map((value) => [value.scope, value]))
  } catch (caught) {
    consentError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    loading.value = false
  }
}

async function toggleConsent(scope: AiConsentScope, event: Event) {
  const input = event.target as HTMLInputElement
  const previous = consents.value[scope]?.granted ?? false
  busyScope.value = scope
  consentError.value = ''
  consentNotice.value = ''
  try {
    const updated = await updateAiConsent(scope, input.checked)
    consents.value = { ...consents.value, [scope]: updated }
    consentNotice.value = input.checked
      ? t('settings.consentGranted')
      : t('settings.consentRevoked')
  } catch (caught) {
    input.checked = previous
    consentError.value = caught instanceof Error ? caught.message : t('errors.generic')
  } finally {
    busyScope.value = ''
  }
}

function changeLocale(event: Event) {
  const next = (event.target as HTMLSelectElement).value === 'en' ? 'en' : 'zh-CN'
  persistLocale(next)
}

onMounted(load)
</script>
