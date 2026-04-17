<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotifyDetail, getNotifyPage, markAllNotifyRead, markNotifyRead } from '../api/notify'
import type { NotifyDetail, NotifyItem } from '../types/api'
import { useAuthStore } from '../stores/auth'
import { useNotifyStore } from '../stores/notify'

const router = useRouter()
const authStore = useAuthStore()
const notifyStore = useNotifyStore()
const list = ref<NotifyItem[]>([])
const selected = ref<NotifyDetail | null>(null)
const unread = computed(() => notifyStore.unread)

async function loadData() {
  if (!authStore.isLoggedIn) {
    router.replace('/login')
    return
  }
  const page = await getNotifyPage(1, 20)
  list.value = page.list
  await notifyStore.refreshUnread()
}

async function selectItem(item: NotifyItem) {
  selected.value = await getNotifyDetail(item.id)
  if (!item.isRead) {
    await markNotifyRead(item.id)
    item.isRead = 1
    notifyStore.decreaseUnread(1)
  }
  if (item.articleId) {
    await router.push(`/article/${item.articleId}`)
    return
  }
  ElMessage.info('该通知没有关联文章，已在右侧展示详情')
  await loadData()
}

async function readAll() {
  await markAllNotifyRead()
  list.value = list.value.map((item) => ({ ...item, isRead: 1 }))
  notifyStore.clearUnread()
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="notify-grid">
    <section class="panel panel-notify-list">
      <div class="section-head">
        <div>
          <div class="section-kicker">Inbox</div>
          <h2>通知列表</h2>
          <p class="subtle">未读 {{ unread }}</p>
        </div>
        <el-button @click="readAll">全部已读</el-button>
      </div>
      <div class="notify-list">
        <button v-for="item in list" :key="item.id" class="notify-item" :class="{ 'is-unread': !item.isRead }" @click="selectItem(item)">
          <strong class="notify-item__title">{{ item.title }}</strong>
          <span class="subtle">{{ item.createTime?.replace('T', ' ') || '刚刚' }}</span>
        </button>
      </div>
    </section>
    <section class="panel panel-notify-detail">
      <div class="section-kicker">Preview</div>
      <h2>通知详情</h2>
      <template v-if="selected">
        <h3 class="notify-detail__title">{{ selected.title }}</h3>
        <p class="subtle">{{ selected.createTime?.replace('T', ' ') || '刚刚' }}</p>
        <div class="article-content">{{ selected.content }}</div>
      </template>
      <p v-else class="subtle">点击左侧通知查看详情</p>
    </section>
  </div>
</template>
