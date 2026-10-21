<template>
  <section class="page profile-page">
    <header class="page-heading">
      <div>
        <h1>{{ t('profile.title') }}</h1>
        <p>{{ t('profile.subtitle') }}</p>
      </div>
    </header>

    <div v-if="loading" class="panel profile-loading" role="status">
      <LoaderCircle class="spin" :size="18" />
      {{ t('profile.loading') }}
    </div>

    <div v-else-if="loadError" class="panel profile-loading" role="alert">
      <CircleAlert :size="18" />
      <span>{{ loadError }}</span>
      <button type="button" class="text-button" @click="loadProfile">
        {{ t('common.retry') }}
      </button>
    </div>

    <section v-else class="profile-layout">
      <aside class="profile-summary" aria-labelledby="profile-summary-title">
        <span class="profile-avatar" aria-hidden="true">{{ initial }}</span>
        <div>
          <h2 id="profile-summary-title">{{ savedName }}</h2>
          <p>{{ profile?.email }}</p>
        </div>
      </aside>

      <form class="panel profile-form" @submit.prevent="saveProfile">
        <div class="profile-form-heading">
          <div>
            <h2>{{ t('profile.accountTitle') }}</h2>
            <p>{{ t('profile.accountDescription') }}</p>
          </div>
          <span class="profile-account-badge">{{ t('profile.privateAccount') }}</span>
        </div>

        <label class="field">
          <span>{{ t('profile.displayName') }}</span>
          <input
            v-model="displayName"
            class="input"
            name="displayName"
            autocomplete="name"
            :aria-describedby="displayNameHintId"
            :disabled="saving"
            @input="onDisplayNameInput"
          />
        </label>
        <p :id="displayNameHintId" class="field-hint">
          {{ t('profile.displayNameHint') }}
        </p>

        <label class="field">
          <span>{{ t('profile.email') }}</span>
          <input
            class="input"
            :value="profile?.email"
            readonly
            aria-readonly="true"
          />
        </label>
        <p class="field-hint">{{ t('profile.emailHint') }}</p>

        <div class="profile-form-actions">
          <button type="submit" class="button primary" :disabled="saving || !dirty">
            <LoaderCircle v-if="saving" class="spin" :size="16" />
            {{ saving ? t('common.saving') : t('profile.save') }}
          </button>
          <button
            type="button"
            class="text-button"
            :disabled="saving || !dirty"
            @click="resetForm"
          >
            {{ t('common.cancel') }}
          </button>
          <span v-if="statusMessage" class="profile-save-status" role="status">
            {{ statusMessage }}
          </span>
        </div>
        <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>
      </form>

      <section class="profile-boundaries" :aria-labelledby="boundariesTitleId">
        <h2 :id="boundariesTitleId">{{ t('profile.boundariesTitle') }}</h2>
        <dl class="profile-boundary-list">
          <div>
            <dt>{{ t('profile.boundaryIdentity') }}</dt>
            <dd>{{ t('profile.boundaryIdentityValue') }}</dd>
          </div>
          <div>
            <dt>{{ t('profile.boundaryIsolation') }}</dt>
            <dd>{{ t('profile.boundaryIsolationValue') }}</dd>
          </div>
          <div>
            <dt>{{ t('profile.boundaryDisplayName') }}</dt>
            <dd>{{ t('profile.boundaryDisplayNameValue') }}</dd>
          </div>
        </dl>
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { CircleAlert, LoaderCircle } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ApiError, getProfile } from '@/api/client'
import { useSessionStore } from '@/stores/session'
import type { ProfileResponse } from '@/types/api'

const { t } = useI18n()
const session = useSessionStore()
const displayNameHintId = `profile-name-hint-${Math.random().toString(36).slice(2)}`
const boundariesTitleId = `profile-boundaries-${Math.random().toString(36).slice(2)}`
const profile = ref<ProfileResponse>()
const displayName = ref('')
const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const errorMessage = ref('')
const statusMessage = ref('')

const savedName = computed(() => profile.value?.displayName || t('account.defaultName'))
const initial = computed(() => Array.from(displayName.value.trim())[0]?.toUpperCase() || '识')
const dirty = computed(() => normalizeName(displayName.value) !== profile.value?.displayName)

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  try {
    profile.value = await getProfile()
    session.applyProfile(profile.value)
    displayName.value = profile.value.displayName
  } catch (caught) {
    loadError.value = errorLabel(caught, t('profile.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  errorMessage.value = ''
  statusMessage.value = ''
  const normalized = normalizeName(displayName.value)
  const validationError = validateName(normalized)
  if (validationError) {
    errorMessage.value = t(`profile.errors.${validationError}`)
    return
  }

  saving.value = true
  try {
    const updated = await session.updateProfile(normalized)
    profile.value = updated
    displayName.value = updated.displayName
    statusMessage.value = t('profile.saved')
  } catch (caught) {
    errorMessage.value = errorLabel(caught, t('profile.saveFailed'))
  } finally {
    saving.value = false
  }
}

function resetForm() {
  displayName.value = profile.value?.displayName || ''
  errorMessage.value = ''
  statusMessage.value = ''
}

function onDisplayNameInput() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function normalizeName(value: string) {
  return value.trim().replace(/\s+/gu, ' ')
}

function validateName(value: string) {
  const length = Array.from(value).length
  if (length < 1) return 'required'
  if (length > 40) return 'tooLong'
  if (hasUnsafeControl(value)) return 'invalidCharacters'
  return ''
}

function hasUnsafeControl(value: string) {
  return Array.from(value).some((character) => {
    const codePoint = character.codePointAt(0)
    return (
      character === '<' ||
      character === '>' ||
      ((/\p{Cc}|\p{Cf}/u.test(character)) &&
        codePoint !== 0x200c &&
        codePoint !== 0x200d)
    )
  })
}

function errorLabel(caught: unknown, fallback: string) {
  if (caught instanceof ApiError) {
    if (caught.status === 401) return t('errors.unauthorized')
    if (caught.code === 'DISPLAY_NAME_INVALID') return t('profile.errors.invalidCharacters')
    if (caught.status === 422) return t('profile.errors.invalid')
  }
  return caught instanceof Error && caught.message ? caught.message : fallback
}

onMounted(loadProfile)
</script>
