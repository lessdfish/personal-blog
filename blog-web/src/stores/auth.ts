import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login, logout, updateCurrentUser } from '../api/user'
import type { CurrentUser } from '../types/api'
import { useNotifyStore } from './notify'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const loading = ref(false)
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!user.value)

  async function fetchCurrentUser() {
    loading.value = true
    try {
      user.value = await getCurrentUser()
    } finally {
      initialized.value = true
      loading.value = false
    }
    return user.value
  }

  async function loginAction(payload: { username: string; password: string }) {
    loading.value = true
    try {
      const result = await login(payload)
      user.value = result.user as CurrentUser
      initialized.value = true
      return result
    } finally {
      loading.value = false
    }
  }

  async function logoutAction() {
    await logout()
    user.value = null
    initialized.value = true
    useNotifyStore().clearUnread()
  }

  async function updateCurrentUserAction(payload: { nickname?: string; avatar?: string; email?: string; phone?: string }) {
    loading.value = true
    try {
      await updateCurrentUser(payload)
      await fetchCurrentUser()
    } finally {
      loading.value = false
    }
  }

  function clearSession() {
    user.value = null
    initialized.value = true
    useNotifyStore().clearUnread()
  }

  return { user, loading, initialized, isLoggedIn, fetchCurrentUser, loginAction, logoutAction, updateCurrentUserAction, clearSession }
})
