<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { publishArticle } from '../api/article'

const form = reactive({
  title: '',
  summary: '',
  content: '',
  boardId: undefined as number | undefined,
  tags: '',
})

async function submit() {
  await publishArticle(form)
  ElMessage.success('帖子发布成功')
  form.title = ''
  form.summary = ''
  form.content = ''
  form.boardId = undefined
  form.tags = ''
}
</script>

<template>
  <section class="panel page-narrow panel-publish">
    <div class="section-kicker">Publish</div>
    <h1>发布帖子</h1>
    <p class="subtle">界面尽量轻，重点放在标题、摘要和正文录入本身。</p>
    <el-form label-position="top">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持 Markdown，例如 # 标题、**加粗**、- 列表、```代码```" />
      </el-form-item>
      <el-form-item label="版块ID">
        <el-input-number v-model="form.boardId" :min="1" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="form.tags" placeholder="例如：redis,springcloud" />
      </el-form-item>
      <el-button type="primary" @click="submit">提交</el-button>
    </el-form>
  </section>
</template>
