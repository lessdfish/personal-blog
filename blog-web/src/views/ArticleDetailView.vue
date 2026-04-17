<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { createComment, getCommentPage } from '../api/comment'
import {
  favoriteArticle,
  getArticleDetail,
  hasFavoritedArticle,
  hasLikedArticle,
  likeArticle,
  unfavoriteArticle,
  unlikeArticle,
} from '../api/article'
import type { ArticleDetail, CommentItem } from '../types/api'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const article = ref<ArticleDetail | null>(null)
const comments = ref<CommentItem[]>([])
const commentText = ref('')
const replyParentId = ref<number | undefined>(undefined)
const loading = ref(false)
const interactionLoading = ref(false)
const liked = ref(false)
const favorited = ref(false)

const articleId = computed(() => Number(route.params.id))
const renderedContent = computed(() => {
  const source = article.value?.content || ''
  const html = marked.parse(source, {
    async: false,
    breaks: true,
    gfm: true,
  }) as string
  return DOMPurify.sanitize(html)
})

async function loadDetail() {
  loading.value = true
  try {
    const detailPromise = getArticleDetail(articleId.value)
    const commentPromise = getCommentPage(articleId.value, 1, 20).catch(() => ({ list: [], total: 0 }))
    const [detail, page] = await Promise.all([detailPromise, commentPromise])
    article.value = detail
    comments.value = page.list
    if (authStore.isLoggedIn) {
      const [likedState, favoritedState] = await Promise.all([
        hasLikedArticle(articleId.value).catch(() => false),
        hasFavoritedArticle(articleId.value).catch(() => false),
      ])
      liked.value = likedState
      favorited.value = favoritedState
    } else {
      liked.value = false
      favorited.value = false
    }
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再评论')
    return
  }
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  await createComment({ articleId: articleId.value, parentId: replyParentId.value, content: commentText.value })
  ElMessage.success(replyParentId.value ? '回复成功' : '评论成功')
  commentText.value = ''
  replyParentId.value = undefined
  await loadDetail()
}

async function doLike() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再操作')
    return
  }
  interactionLoading.value = true
  try {
    const nextState = !liked.value
    if (nextState) {
      await likeArticle(articleId.value)
    } else {
      await unlikeArticle(articleId.value)
    }
    liked.value = nextState
    if (article.value) {
      article.value.likeCount = Math.max(0, (article.value.likeCount || 0) + (nextState ? 1 : -1))
    }
    const [detail, likedState] = await Promise.all([
      getArticleDetail(articleId.value),
      hasLikedArticle(articleId.value).catch(() => nextState),
    ])
    article.value = detail
    liked.value = likedState
    ElMessage.success(nextState ? '点赞成功' : '已取消点赞')
  } finally {
    interactionLoading.value = false
  }
}

async function doFavorite() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再操作')
    return
  }
  interactionLoading.value = true
  try {
    const nextState = !favorited.value
    if (nextState) {
      await favoriteArticle(articleId.value)
    } else {
      await unfavoriteArticle(articleId.value)
    }
    favorited.value = nextState
    if (article.value) {
      article.value.favoriteCount = Math.max(0, (article.value.favoriteCount || 0) + (nextState ? 1 : -1))
    }
    const [detail, favoritedState] = await Promise.all([
      getArticleDetail(articleId.value),
      hasFavoritedArticle(articleId.value).catch(() => nextState),
    ])
    article.value = detail
    favorited.value = favoritedState
    ElMessage.success(nextState ? '收藏成功' : '已取消收藏')
  } finally {
    interactionLoading.value = false
  }
}

function replyTo(comment: CommentItem) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再回复')
    return
  }
  replyParentId.value = comment.id
}

function formatAbsoluteTime(value?: string) {
  if (!value) {
    return '--'
  }
  return value.replace('T', ' ')
}

function formatRelativeTime(value?: string) {
  if (!value) {
    return ''
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return ''
  }
  const diff = Date.now() - parsed.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < hour) {
    return `${Math.max(1, Math.floor(diff / minute))}分钟前`
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  }
  return `${Math.floor(diff / day)}天前`
}

function commentAuthorName(comment: CommentItem) {
  return comment.userName || `用户${comment.userId}`
}

function commentAvatar(comment: CommentItem) {
  return comment.userAvatar || '/forum-fantasy-girl.jpg'
}

onMounted(loadDetail)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <section class="panel panel-detail" v-if="article">
      <div class="section-kicker">Article</div>
      <h1 class="detail-title">{{ article.title }}</h1>
      <div class="detail-stat-strip">
        <div class="detail-stat-chip">
          <span>板块</span>
          <strong>{{ article.boardName || '未分版块' }}</strong>
        </div>
        <div class="detail-stat-chip">
          <span>浏览</span>
          <strong>{{ article.viewCount }}</strong>
        </div>
        <div class="detail-stat-chip">
          <span>评论</span>
          <strong>{{ article.commentCount }}</strong>
        </div>
        <div class="detail-stat-chip">
          <span>点赞</span>
          <strong>{{ article.likeCount }}</strong>
        </div>
        <div class="detail-stat-chip">
          <span>收藏</span>
          <strong>{{ article.favoriteCount || 0 }}</strong>
        </div>
        <div class="detail-stat-chip">
          <span>热度</span>
          <strong>{{ article.heatScore || 0 }}</strong>
        </div>
      </div>
      <div class="article-content article-content--detail markdown-content" v-html="renderedContent"></div>
      <div class="interaction-bar">
        <button class="interaction-chip" :class="{ 'is-active': liked }" :disabled="interactionLoading" @click="doLike">
          <span class="interaction-chip__icon">👍</span>
          <span>点赞</span>
          <strong>{{ article.likeCount }}</strong>
        </button>
        <button class="interaction-chip" :class="{ 'is-active': favorited }" :disabled="interactionLoading" @click="doFavorite">
          <span class="interaction-chip__icon">★</span>
          <span>收藏</span>
          <strong>{{ article.favoriteCount || 0 }}</strong>
        </button>
      </div>
    </section>

    <section class="panel panel-comments">
      <div class="section-head">
        <div>
          <div class="section-kicker">Conversation</div>
          <h2>评论区</h2>
        </div>
      </div>
      <div class="comment-editor">
        <el-input v-model="commentText" type="textarea" :rows="4" :placeholder="authStore.isLoggedIn ? '输入评论内容' : '登录后可发表评论'" />
      </div>
      <div class="action-row action-row--comment">
        <span v-if="replyParentId" class="subtle">当前正在回复这条评论</span>
        <el-button type="primary" @click="submitComment">提交</el-button>
      </div>

      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-card">
          <div class="comment-head">
            <div class="comment-head__identity">
              <img :src="commentAvatar(comment)" :alt="commentAuthorName(comment)" class="comment-avatar" />
              <div class="comment-head__meta">
                <div class="comment-head__title">
                  <strong>{{ commentAuthorName(comment) }}</strong>
                  <span class="subtle">{{ formatRelativeTime(comment.createTime) }}</span>
                </div>
              </div>
            </div>
            <span class="subtle">{{ formatAbsoluteTime(comment.createTime) }}</span>
          </div>
          <div class="comment-body">{{ comment.content }}</div>
          <div class="comment-actions">
            <button class="link-like" @click="replyTo(comment)">回复</button>
          </div>
          <div v-if="comment.children?.length" class="comment-children">
            <div v-for="child in comment.children" :key="child.id" class="comment-card comment-card--child">
              <div class="comment-head">
                <div class="comment-head__identity">
                  <img :src="commentAvatar(child)" :alt="commentAuthorName(child)" class="comment-avatar" />
                  <div class="comment-head__meta">
                    <div class="comment-head__title">
                      <strong>{{ commentAuthorName(child) }}</strong>
                      <span class="subtle">{{ formatRelativeTime(child.createTime) }}</span>
                    </div>
                  </div>
                </div>
                <span class="subtle">{{ formatAbsoluteTime(child.createTime) }}</span>
              </div>
              <div class="comment-body">
                <template v-if="child.notifyUserName">
                  回复 {{ child.notifyUserName }}：
                </template>
                {{ child.content }}
              </div>
              <div class="comment-actions">
                <button class="link-like" @click="replyTo(child)">回复</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
