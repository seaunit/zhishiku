<template>
  <main class="login-page">
    <section class="login-brand-panel" aria-labelledby="register-brand-title">
      <BrandMark />
      <div class="login-brand-copy">
        <h1 id="register-brand-title">{{ t('login.panelTitle') }}</h1>
        <p>{{ t('login.panelBody') }}</p>
      </div>
      <p class="login-version">zhishiku / 0.1</p>
    </section>

    <section class="login-form-panel">
      <div class="login-form-head">
        <LocaleSwitch />
      </div>

      <form class="login-form register-form" @submit.prevent="submit">
        <div>
          <h2>{{ t('register.title') }}</h2>
          <p>{{ t('register.subtitle') }}</p>
        </div>

        <div class="field">
          <label for="register-email">{{ t('register.email') }}</label>
          <input
            id="register-email"
            v-model.trim="email"
            class="input"
            type="email"
            name="email"
            autocomplete="email"
            inputmode="email"
            :placeholder="t('register.emailPlaceholder')"
            required
            @input="clearError"
          />
        </div>

        <div class="field">
          <label for="register-email-code">{{ t('register.emailCode') }}</label>
          <div class="verification-row">
            <input
              id="register-email-code"
              v-model.trim="emailCode"
              class="input"
              type="text"
              name="emailCode"
              autocomplete="one-time-code"
              inputmode="numeric"
              maxlength="6"
              :placeholder="t('register.emailCodePlaceholder')"
              required
              @input="emailCode = emailCode.replace(/\D/g, ''); clearError()"
            />
            <button
              class="button secondary code-send"
              type="button"
              :disabled="sendCodeDisabled"
              @click="sendCode"
            >
              {{ sendCodeLabel }}
            </button>
          </div>
          <p v-if="codeNotice" class="field-note" role="status">{{ codeNotice }}</p>
        </div>

        <div class="field">
          <label for="register-password">{{ t('register.password') }}</label>
          <input
            id="register-password"
            v-model="password"
            class="input"
            type="password"
            name="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="256"
            :placeholder="t('register.passwordPlaceholder')"
            required
            @input="clearError"
          />
        </div>

        <div class="field">
          <label for="register-confirm-password">{{ t('register.confirmPassword') }}</label>
          <input
            id="register-confirm-password"
            v-model="confirmPassword"
            class="input"
            type="password"
            name="confirmPassword"
            autocomplete="new-password"
            minlength="8"
            maxlength="256"
            :placeholder="t('register.confirmPasswordPlaceholder')"
            required
            @input="clearError"
          />
        </div>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <button class="button primary login-submit" type="submit" :disabled="submitting">
          <LoaderCircle v-if="submitting" class="spin" :size="18" />
          <span>{{ submitting ? t('register.creating') : t('register.createAccount') }}</span>
        </button>

        <RouterLink class="auth-switch-link" to="/login">
          {{ t('register.haveAccount') }}
        </RouterLink>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { LoaderCircle } from 'lucide-vue-next'
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  ApiError,
  registerAccount,
  sendRegistrationEmailCode,
} from '@/api/client'
import BrandMark from '@/components/BrandMark.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'

const { t, locale } = useI18n()
const router = useRouter()
const email = ref('')
const emailCode = ref('')
const emailCodeSending = ref(false)
const resendSeconds = ref(0)
const password = ref('')
const confirmPassword = ref('')
const formError = ref('')
const codeNotice = ref('')
const submitting = ref(false)

let resendTimer: number | undefined

const sendCodeDisabled = computed(
  () => emailCodeSending.value || resendSeconds.value > 0,
)
const sendCodeLabel = computed(() => {
  if (emailCodeSending.value) {
    return t('register.sendingCode')
  }
  if (resendSeconds.value > 0) {
    return t('register.resendIn', { seconds: resendSeconds.value })
  }
  return t('register.sendCode')
})

function clearError() {
  formError.value = ''
}

function validEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function errorMessage(caught: unknown) {
  if (caught instanceof ApiError) {
    switch (caught.code) {
      case 'EMAIL_DOMAIN_NOT_ALLOWED':
        return t('register.disposableEmail')
      case 'INVALID_EMAIL':
        return t('register.invalidEmail')
      case 'EMAIL_CODE_INVALID':
        return t('register.emailCodeInvalid')
      case 'EMAIL_DELIVERY_UNAVAILABLE':
        return t('register.deliveryUnavailable')
      case 'RATE_LIMITED':
        return t('register.rateLimited')
      case 'REGISTRATION_CLOSED':
        return t('register.registrationClosed')
      case 'EMAIL_ALREADY_REGISTERED':
        return t('register.emailAlreadyRegistered')
      case 'PASSWORD_CONFIRMATION_MISMATCH':
        return t('register.passwordMismatch')
      default:
        return caught.message
    }
  }
  return caught instanceof Error ? caught.message : t('errors.generic')
}

function startCooldown(seconds: number) {
  if (resendTimer !== undefined) {
    window.clearInterval(resendTimer)
  }
  resendSeconds.value = Math.max(1, seconds)
  resendTimer = window.setInterval(() => {
    resendSeconds.value = Math.max(0, resendSeconds.value - 1)
    if (resendSeconds.value === 0 && resendTimer !== undefined) {
      window.clearInterval(resendTimer)
      resendTimer = undefined
    }
  }, 1000)
}

async function sendCode() {
  clearError()
  codeNotice.value = ''
  if (!validEmail(email.value)) {
    formError.value = t('register.invalidEmail')
    return
  }
  emailCodeSending.value = true
  try {
    const response = await sendRegistrationEmailCode(email.value)
    if (response.debugCode) {
      emailCode.value = response.debugCode
    }
    codeNotice.value = t('register.emailCodeSent')
    startCooldown(response.resendAfterSeconds)
  } catch (caught) {
    formError.value = errorMessage(caught)
  } finally {
    emailCodeSending.value = false
  }
}

async function submit() {
  clearError()
  if (!validEmail(email.value)) {
    formError.value = t('register.invalidEmail')
    return
  }
  if (!emailCode.value || !password.value || !confirmPassword.value) {
    formError.value = t('register.required')
    return
  }
  if (password.value.length < 8) {
    formError.value = t('register.passwordLength')
    return
  }
  if (password.value !== confirmPassword.value) {
    formError.value = t('register.passwordMismatch')
    return
  }

  submitting.value = true
  try {
    await registerAccount({
      email: email.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
      emailCode: emailCode.value,
      locale: locale.value === 'en' ? 'en' : 'zh-CN',
    })
    await router.replace({ name: 'login', query: { registered: '1' } })
  } catch (caught) {
    formError.value = errorMessage(caught)
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  if (resendTimer !== undefined) {
    window.clearInterval(resendTimer)
  }
})
</script>
