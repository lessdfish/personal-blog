import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getNotifyUnreadCount } from '../api/notify'
import { useAuthStore } from './auth'

export const useNotifyStore = defineStore('notify', () => {
  const unread = ref(0)
  const loading = ref(false)

  /**
   * 刷新未读通知数：从后端获取最新数量并更新页面状态。
   */
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

  /**
   * 减少未读数：本地先扣减通知角标，让界面立即响应。
   */
  function decreaseUnread(amount = 1) {
    unread.value = Math.max(0, unread.value - amount)
  }

  /**
   * 清空未读数：全部已读后把本地角标归零。
   */
  function clearUnread() {
    unread.value = 0
  }

  return { unread, loading, refreshUnread, decreaseUnread, clearUnread }
})
