<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import { getArticlePage, getHotArticles } from '../api/article'
import type { ArticleItem } from '../types/api'

const articleList = ref<ArticleItem[]>([])
const hotList = ref<ArticleItem[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
let requestSeq = 0

/**
 * 加载当前页数据：根据页面状态请求列表接口并更新渲染数据。
 */
async function loadPage() {
  const seq = ++requestSeq
  loading.value = true
  try {
    const [pageResult, hotResult] = await Promise.allSettled([
      getArticlePage({ pageNum: pageNum.value, pageSize: pageSize.value }),
      getHotArticles(8),
    ])
    if (seq !== requestSeq) {
      return
    }
    if (pageResult.status === 'fulfilled') {
      articleList.value = pageResult.value.list
      total.value = pageResult.value.total
    }
    if (hotResult.status === 'fulfilled') {
      hotList.value = hotResult.value
    }
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

onMounted(loadPage)

function formatHeatScore(value?: number) {
  return (value ?? 0).toFixed(2)
}
</script>

<template>
  <div class="page-stack">
    <section class="home-summary-bar">
      <div class="home-summary-bar__label">FORUM INDEX</div>
      <div class="home-summary-bar__value">{{ total }}</div>
      <div class="home-summary-bar__suffix">总帖子数</div>
    </section>

    <div class="home-grid">
      <section class="panel panel-article-stream">
        <div class="section-head">
          <div>
            <div class="section-kicker">Posts</div>
            <h2>帖子</h2>
          </div>
        </div>
        <div v-loading="loading" class="article-stream">
          <router-link v-for="article in articleList" :key="article.id" :to="`/article/${article.id}`" class="article-card article-card--editorial">
            <div class="article-card__head">
              <div class="article-card__flag">
                <span v-if="article.isTop === 1" class="pill pill-top">置顶</span>
                <span v-if="article.isEssence === 1" class="pill pill-good">精华</span>
              </div>
            </div>
            <div class="article-card__title">{{ article.title }}</div>
            <div class="article-card__summary">{{ article.summary }}</div>
            <div class="article-card__meta">
              <span class="article-card__board">{{ article.boardName || '未分版块' }}</span>
              <span>浏览 {{ article.viewCount }}</span>
              <span>赞 {{ article.likeCount }}</span>
              <span>藏 {{ article.favoriteCount || 0 }}</span>
            </div>
          </router-link>
        </div>
        <div class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            @current-change="(value: number) => { pageNum = value; loadPage() }"
          />
        </div>
      </section>

      <aside class="panel side-panel">
        <div class="section-head section-head--compact">
          <div>
            <div class="section-kicker">Heat Ranking</div>
            <h2>热榜</h2>
          </div>
        </div>
        <ol class="rank-list rank-list--numbered">
          <li v-for="(item, index) in hotList" :key="item.id">
            <span class="rank-no">0{{ index + 1 }}</span>
            <router-link :to="`/article/${item.id}`" class="rank-link">{{ item.title }}</router-link>
            <span class="rank-heat"><el-icon><TrendCharts /></el-icon>{{ formatHeatScore(item.heatScore) }}</span>
          </li>
        </ol>
      </aside>
    </div>
  </div>
</template>
