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

  /**
   * 拉取当前用户：应用启动或刷新时恢复登录用户信息。
   */
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

  /**
   * 执行登录流程：调用接口成功后保存用户信息。
   */
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

  /**
   * 执行退出流程：调用后端退出接口并清理前端登录状态。
   */
  async function logoutAction() {
    await logout()
    user.value = null
    initialized.value = true
    useNotifyStore().clearUnread()
  }

  /**
   * 执行资料更新流程：保存后端返回的新用户信息。
   */
  async function updateCurrentUserAction(payload: { nickname?: string; avatar?: string; email?: string; phone?: string }) {
    loading.value = true
    try {
      await updateCurrentUser(payload)
      await fetchCurrentUser()
    } finally {
      loading.value = false
    }
  }

  /**
   * 清理前端会话：移除用户信息并标记为未登录。
   */
  function clearSession() {
    user.value = null
    initialized.value = true
    useNotifyStore().clearUnread()
  }

  return { user, loading, initialized, isLoggedIn, fetchCurrentUser, loginAction, logoutAction, updateCurrentUserAction, clearSession }
})
