<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Bowl, Brush, EditPen, House, SwitchButton, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useNotifyStore } from '../stores/notify'
import { useWallpaperRotation } from '../composables/useWallpaperRotation'

const router = useRouter()
const authStore = useAuthStore()
const notifyStore = useNotifyStore()
const { currentWallpaper, rotateWallpaper } = useWallpaperRotation()
const currentDate = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
}).format(new Date())
const forumVisual = '/forum-fantasy-girl.jpg'

const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || '游客')
const displayAvatar = computed(() => authStore.user?.avatar || forumVisual)

/**
 * 刷新未读通知数：从后端获取最新数量并更新页面状态。
 */
async function refreshUnread() {
  await notifyStore.refreshUnread()
}

/**
 * 处理退出登录：清理状态并跳转到登录页。
 */
async function handleLogout() {
  await authStore.logoutAction()
  ElMessage.success('已退出登录')
  router.push('/')
}

/**
 * 处理登录过期事件：统一清理会话并提示用户重新登录。
 */
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
          <div class="app-header__brand-mark" aria-hidden="true">
            <el-icon><Bowl /></el-icon>
          </div>
          <div>
            <div class="app-header__brand" @click="$router.push('/')">明向饭庄</div>
            <div class="app-header__subtitle">简洁、克制、以内容为核心的社区前端</div>
          </div>
        </div>
        <nav class="app-header__nav">
          <router-link to="/"><el-icon><House /></el-icon><span>首页</span></router-link>
          <router-link to="/publish"><el-icon><EditPen /></el-icon><span>发帖</span></router-link>
          <router-link v-if="authStore.isLoggedIn" to="/notify"><el-icon><Bell /></el-icon><span>通知</span><span v-if="notifyStore.unread > 0" class="badge">{{ notifyStore.unread }}</span></router-link>
          <router-link to="/profile"><el-icon><User /></el-icon><span>我的</span></router-link>
        </nav>
        <div class="app-header__user">
          <button class="icon-btn" type="button" :aria-label="`切换背景壁纸，当前 ${currentWallpaper.name}`" title="切换背景壁纸" @click="rotateWallpaper">
            <el-icon><Brush /></el-icon>
          </button>
          <img :src="displayAvatar" alt="avatar" class="avatar" />
          <div class="app-header__identity">
            <span class="app-header__user-name">{{ displayName }}</span>
            <span class="app-header__user-note">{{ authStore.isLoggedIn ? '当前在线' : '未登录' }}</span>
          </div>
          <button v-if="authStore.isLoggedIn" class="ghost-btn ghost-btn--icon" @click="handleLogout"><el-icon><SwitchButton /></el-icon><span>退出</span></button>
          <router-link v-else to="/login" class="ghost-btn link-like">登录</router-link>
        </div>
      </div>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>
