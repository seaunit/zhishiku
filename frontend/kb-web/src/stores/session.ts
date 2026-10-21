import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as api from '@/api/client'
import type { SessionResponse } from '@/types/api'

export const useSessionStore = defineStore('session', () => {
  const session = ref<SessionResponse | null>(null)
  const submitting = ref(false)
  const error = ref<string | null>(null)
  const isAuthenticated = computed(() => session.value !== null)

  async function login(
    email: string,
    password: string,
    captchaId: string,
    captchaCode: string,
  ) {
    submitting.value = true
    error.value = null
    try {
      session.value = await api.login(email, password, captchaId, captchaCode)
      return session.value
    } catch (caught) {
      error.value = caught instanceof Error ? caught.message : 'LOGIN_FAILED'
      throw caught
    } finally {
      submitting.value = false
    }
  }

  async function logout() {
    try {
      await api.logout()
    } finally {
      session.value = null
      api.resetCsrfToken()
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    session,
    submitting,
    error,
    isAuthenticated,
    login,
    logout,
    clearError,
  }
})

