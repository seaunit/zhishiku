<template>
  <main class="login-page">
    <section class="login-brand-panel" aria-labelledby="login-brand-title">
      <BrandMark />
      <div class="login-brand-copy">
        <h1 id="login-brand-title">{{ t('login.panelTitle') }}</h1>
        <p>{{ t('login.panelBody') }}</p>
      </div>
      <p class="login-version">zhishiku / 0.1</p>
    </section>

    <section class="login-form-panel">
      <div class="login-form-head">
        <LocaleSwitch />
      </div>
      <form class="login-form" @submit.prevent="submit">
        <div>
          <h2>{{ t('login.title') }}</h2>
          <p>{{ t('login.subtitle') }}</p>
        </div>

        <div class="field">
          <label for="login-email">{{ t('login.email') }}</label>
          <input
            id="login-email"
            v-model.trim="email"
            class="input"
            type="email"
            name="email"
            autocomplete="username"
            inputmode="email"
            :placeholder="t('login.emailPlaceholder')"
            required
            @input="clearError"
          />
        </div>

        <div class="field">
          <label for="login-password">{{ t('login.password') }}</label>
          <input
            id="login-password"
            v-model="password"
            class="input"
            type="password"
            name="password"
            autocomplete="current-password"
            minlength="12"
            :placeholder="t('login.passwordPlaceholder')"
            required
            @input="clearError"
          />
        </div>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <button class="button primary login-submit" type="submit" :disabled="submitting">
          <LoaderCircle v-if="submitting" class="spin" :size="18" />
          <span>{{ submitting ? t('login.submitting') : t('login.submit') }}</span>
        </button>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { LoaderCircle } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import BrandMark from '@/components/BrandMark.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const email = ref('')
const password = ref('')
const formError = ref('')
const submitting = computed(() => session.submitting)

function clearError() {
  formError.value = ''
}

function nextPath() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/'
}

async function submit() {
  clearError()
  if (!email.value || !password.value) {
    formError.value = t('login.required')
    return
  }
  if (password.value.length < 12) {
    formError.value = t('login.passwordLength')
    return
  }

  try {
    await session.login(email.value, password.value)
    await router.replace(nextPath())
  } catch (caught) {
    formError.value = caught instanceof Error ? caught.message : t('errors.generic')
  }
}
</script>

