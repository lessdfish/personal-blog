<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useNotifyStore } from '../stores/notify'

const router = useRouter()
const authStore = useAuthStore()
const notifyStore = useNotifyStore()
const currentDate = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
}).format(new Date())
const forumVisual = '/forum-fantasy-girl.jpg'

const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || '游客')
const displayAvatar = computed(() => authStore.user?.avatar || forumVisual)

async function refreshUnread() {
  await notifyStore.refreshUnread()
}

async function handleLogout() {
  await authStore.logoutAction()
  ElMessage.success('已退出登录')
  router.push('/')
}

function handleAuthExpired(event: Event) {
  const message = event instanceof CustomEvent && typeof event.detail === 'string'
    ? event.detail
    : '登录已失效，请重新登录'
  authStore.clearSession()
  ElMessage.warning(message === '未登录' ? '登录已失效，请重新登录' : message)
  if (router.currentRoute.value.name !== 'login') {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

onMounted(async () => {
  if (!authStore.initialized) {
    await authStore.fetchCurrentUser().catch(() => null)
  }
  await refreshUnread()
  window.addEventListener('auth:expired', handleAuthExpired)
})

onBeforeUnmount(() => {
  window.removeEventListener('auth:expired', handleAuthExpired)
})

watch(() => authStore.isLoggedIn, async (loggedIn) => {
  if (loggedIn) {
    await refreshUnread()
    return
  }
  notifyStore.clearUnread()
})
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__meta">论坛系统 / {{ currentDate }}</div>
      <div class="app-header__main">
        <div class="app-header__brand-wrap">
          <div class="app-header__brand-mark">明</div>
          <div>
            <div class="app-header__brand" @click="$router.push('/')">明向饭庄</div>
            <div class="app-header__subtitle">简洁、克制、以内容为核心的社区前端</div>
          </div>
        </div>
        <nav class="app-header__nav">
          <router-link to="/">首页</router-link>
          <router-link to="/publish">发帖</router-link>
          <router-link v-if="authStore.isLoggedIn" to="/notify">通知 <span v-if="notifyStore.unread > 0" class="badge">{{ notifyStore.unread }}</span></router-link>
          <router-link to="/profile">我的</router-link>
        </nav>
        <div class="app-header__user">
          <img :src="displayAvatar" alt="avatar" class="avatar" />
          <div class="app-header__identity">
            <span class="app-header__user-name">{{ displayName }}</span>
            <span class="app-header__user-note">{{ authStore.isLoggedIn ? '当前在线' : '未登录' }}</span>
          </div>
          <button v-if="authStore.isLoggedIn" class="ghost-btn" @click="handleLogout">退出</button>
          <router-link v-else to="/login" class="ghost-btn link-like">登录</router-link>
        </div>
      </div>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>
