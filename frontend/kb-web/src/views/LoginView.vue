<template>
  <main class="login-page">
    <section class="login-brand-panel" aria-labelledby="login-brand-title">
      <div class="login-ocean-map" aria-hidden="true">
        <svg viewBox="0 0 1600 1100" preserveAspectRatio="xMidYMid slice">
          <g class="ocean-contours ocean-contours--far">
            <path d="M-120 112C108 67 253 145 430 122S740 31 925 89s330 86 795 -4" />
            <path d="M-140 176C88 118 268 210 454 178S760 72 956 137s346 97 792 4" />
            <path d="M-110 246C113 178 305 274 508 234S816 126 1006 194s322 110 742 36" />
            <path d="M-150 326C96 246 286 356 514 304S824 205 1034 268s306 126 744 36" />
            <path d="M-100 406C127 336 335 428 533 392S858 286 1050 350s282 122 682 54" />
            <path d="M-130 492C122 405 338 522 562 462S876 363 1073 427s280 130 674 48" />
            <path d="M-120 588C133 495 362 604 574 554S908 450 1100 516s258 128 640 50" />
            <path d="M-100 700C144 606 367 724 600 665S932 566 1127 631s254 135 618 45" />
          </g>
          <g class="ocean-contours ocean-contours--near">
            <path d="M-180 368C118 276 298 424 538 364S904 192 1154 286s356 142 740 14" />
            <path d="M-150 462C156 360 338 516 594 446S950 284 1206 372s306 142 694 12" />
            <path d="M-170 566C140 454 342 622 608 544S978 382 1236 470s272 138 648 12" />
            <path d="M-160 684C154 566 356 742 628 655S1002 486 1264 578s260 142 620 14" />
            <path d="M-140 812C168 684 376 868 652 778S1028 602 1292 694s244 146 596 14" />
          </g>
        </svg>
      </div>
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
          <label for="login-email">{{ t('login.loginName') }}</label>
          <input
            id="login-email"
            v-model.trim="loginName"
            class="input"
            type="text"
            name="email"
            autocomplete="username"
            :placeholder="t('login.loginNamePlaceholder')"
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
const loginName = ref('')
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
  if (!loginName.value || !password.value) {
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
    await session.login(loginName.value, password.value, captchaId.value, captchaCode.value)
    await router.replace(nextPath())
  } catch (caught) {
    formError.value =
      caught instanceof ApiError && caught.code === 'CAPTCHA_INVALID'
        ? t('login.captchaInvalid')
        : caught instanceof ApiError && caught.code === 'AUTHENTICATION_FAILED'
          ? t('login.authenticationFailed')
          : caught instanceof Error
            ? caught.message
            : t('errors.generic')
    await loadCaptcha(true)
  }
}

onMounted(loadCaptcha)
</script>

