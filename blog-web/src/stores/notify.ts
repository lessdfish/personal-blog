import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getNotifyUnreadCount } from '../api/notify'
import { useAuthStore } from './auth'

export const useNotifyStore = defineStore('notify', () => {
  const unread = ref(0)
  const loading = ref(false)

  async function refreshUnread() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      unread.value = 0
      return 0
    }
    loading.value = true
    try {
      unread.value = await getNotifyUnreadCount()
    } catch {
      unread.value = 0
    } finally {
      loading.value = false
    }
    return unread.value
  }

  function decreaseUnread(amount = 1) {
    unread.value = Math.max(0, unread.value - amount)
  }

  function clearUnread() {
    unread.value = 0
  }

  return { unread, loading, refreshUnread, decreaseUnread, clearUnread }
})
