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
        <p
          v-if="route.query.registered === '1'"
          class="success-message"
          role="status"
        >
          {{ t('login.registeredSuccess') }}
        </p>

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
            minlength="8"
            maxlength="256"
            :placeholder="t('login.passwordPlaceholder')"
            required
            @input="clearError"
          />
        </div>

        <div class="field">
          <label for="login-captcha">{{ t('login.captcha') }}</label>
          <div class="captcha-control">
            <input
              id="login-captcha"
              v-model.trim="captchaCode"
              class="input"
              type="text"
              name="captchaCode"
              autocomplete="off"
              autocapitalize="characters"
              spellcheck="false"
              maxlength="8"
              :placeholder="t('login.captchaPlaceholder')"
              required
              @input="captchaCode = captchaCode.toUpperCase(); clearError()"
            />
            <button
              class="captcha-image-button"
              type="button"
              :aria-label="t('login.captchaRefresh')"
              :title="t('login.captchaRefresh')"
              :disabled="captchaLoading"
              @click="loadCaptcha()"
            >
              <img
                v-if="captchaImage"
                :src="captchaImage"
                :alt="t('login.captchaAlt')"
              />
              <LoaderCircle v-else class="spin" :size="20" />
            </button>
          </div>
        </div>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <button class="button primary login-submit" type="submit" :disabled="submitting">
          <LoaderCircle v-if="submitting" class="spin" :size="18" />
          <span>{{ submitting ? t('login.submitting') : t('login.submit') }}</span>
        </button>

        <RouterLink class="auth-switch-link" to="/register">
          {{ t('login.registerLink') }}
        </RouterLink>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { LoaderCircle } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, getCaptcha } from '@/api/client'
import BrandMark from '@/components/BrandMark.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const email = ref('')
const password = ref('')
const captchaId = ref('')
const captchaCode = ref('')
const captchaImage = ref('')
const captchaLoading = ref(false)
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

async function loadCaptcha(preserveError = false) {
  captchaLoading.value = true
  captchaCode.value = ''
  try {
    const challenge = await getCaptcha()
    captchaId.value = challenge.captchaId
    captchaImage.value = challenge.image
    if (!preserveError) {
      clearError()
    }
  } catch {
    captchaId.value = ''
    captchaImage.value = ''
    formError.value = t('login.captchaLoadFailed')
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  clearError()
  if (!email.value || !password.value) {
    formError.value = t('login.required')
    return
  }
  if (password.value.length < 8) {
    formError.value = t('login.passwordLength')
    return
  }
  if (!captchaId.value || !captchaCode.value) {
    formError.value = t('login.captchaRequired')
    return
  }

  try {
    await session.login(email.value, password.value, captchaId.value, captchaCode.value)
    await router.replace(nextPath())
  } catch (caught) {
    formError.value =
      caught instanceof ApiError && caught.code === 'CAPTCHA_INVALID'
        ? t('login.captchaInvalid')
        : caught instanceof Error
          ? caught.message
          : t('errors.generic')
    await loadCaptcha(true)
  }
}

onMounted(loadCaptcha)
</script>

