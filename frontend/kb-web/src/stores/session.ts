import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as api from '@/api/client'
import type { ProfileResponse, SessionResponse } from '@/types/api'

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

  async function restore() {
    if (session.value) {
      return session.value
    }
    session.value = await api.getCurrentSession()
    return session.value
  }

  async function logout() {
    await api.logout()
    session.value = null
    api.resetCsrfToken()
  }

  async function updateProfile(displayName: string) {
    submitting.value = true
    error.value = null
    try {
      const updated = await api.updateProfile({ displayName })
      applyProfile(updated)
      return updated
    } catch (caught) {
      error.value = caught instanceof Error ? caught.message : 'PROFILE_UPDATE_FAILED'
      throw caught
    } finally {
      submitting.value = false
    }
  }

  function applyProfile(profile: ProfileResponse) {
    if (!session.value) {
      return
    }
    session.value = {
      ...session.value,
      ownerId: profile.ownerId,
      email: profile.email,
      displayName: profile.displayName,
      locale: profile.locale,
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
    restore,
    logout,
    updateProfile,
    applyProfile,
    clearError,
  }
})

