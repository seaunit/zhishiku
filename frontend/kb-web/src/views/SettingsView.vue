<template>
  <section class="page settings-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('settings.title') }}</h1>
        <p>{{ t('settings.subtitle') }}</p>
      </div>
    </header>

    <section class="settings-layout">
      <aside class="settings-index" aria-label="设置分类">
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
          <div class="authorization-list">
            <label class="authorization-row">
              <span>
                <strong>{{ t('settings.textTitle') }}</strong>
                <small>{{ t('settings.textDescription') }}</small>
              </span>
              <input v-model="authorization.text" type="checkbox" />
            </label>
            <label class="authorization-row">
              <span>
                <strong>{{ t('settings.visionTitle') }}</strong>
                <small>{{ t('settings.visionDescription') }}</small>
              </span>
              <input v-model="authorization.vision" type="checkbox" />
            </label>
            <label class="authorization-row">
              <span>
                <strong>{{ t('settings.searchTitle') }}</strong>
                <small>{{ t('settings.searchDescription') }}</small>
              </span>
              <input v-model="authorization.search" type="checkbox" />
            </label>
          </div>
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
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { persistLocale } from '@/i18n'

const { t, locale } = useI18n()
const STORAGE_KEY = 'zhishiku.visual-authorizations'
const saved = readAuthorization()
const authorization = reactive(saved)

function readAuthorization() {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY)
    if (value) {
      return {
        text: true,
        vision: true,
        search: false,
        ...(JSON.parse(value) as Record<string, boolean>),
      }
    }
  } catch {
    // A malformed local preference falls back to the safe defaults below.
  }
  return { text: true, vision: true, search: false }
}

watch(
  authorization,
  (value) => window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value)),
  { deep: true },
)

function changeLocale(event: Event) {
  const next = (event.target as HTMLSelectElement).value === 'en' ? 'en' : 'zh-CN'
  persistLocale(next)
}
</script>
