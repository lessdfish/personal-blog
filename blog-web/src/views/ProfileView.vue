<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { uploadAvatar } from '../api/user'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const saving = ref(false)
const avatarUploading = ref(false)
const forumVisual = '/forum-fantasy-girl.jpg'
const editableFields = reactive<Record<'nickname' | 'email' | 'phone' | 'avatar', boolean>>({
  nickname: false,
  email: false,
  phone: false,
  avatar: false,
})
const form = reactive({
  username: '',
  nickname: '',
  avatar: '',
  email: '',
  phone: '',
})
const errors = reactive({
  nickname: '',
  avatar: '',
  email: '',
  phone: '',
  form: '',
})

const displayAvatar = computed(() => form.avatar || authStore.user?.avatar || forumVisual)
const uploadRef = ref()

function maskPhone(phone?: string) {
  if (!phone) {
    return '--'
  }
  return phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

const profileRows = computed(() => [
  { key: 'username', label: '用户名', value: form.username || '--', editable: false },
  { key: 'nickname', label: '昵称', value: authStore.user?.nickname || '--', editable: true },
  { key: 'email', label: '邮箱', value: authStore.user?.email || '--', editable: true },
  { key: 'phone', label: '手机号', value: maskPhone(authStore.user?.phone), editable: true },
  { key: 'avatar', label: '头像地址', value: form.avatar || '未上传头像', editable: true },
  { key: 'loginIp', label: '登录 IP', value: authStore.user?.sessionInfo?.loginIp || '--', editable: false },
  { key: 'location', label: '地理位置', value: authStore.user?.sessionInfo?.location || '--', editable: false },
  { key: 'device', label: '设备型号', value: authStore.user?.sessionInfo?.device || '--', editable: false },
  { key: 'browser', label: '登录浏览器', value: authStore.user?.sessionInfo?.browser || '--', editable: false },
  { key: 'loginTime', label: '登录时间', value: authStore.user?.sessionInfo?.loginTime || '--', editable: false },
])

function syncForm() {
  form.username = authStore.user?.username || ''
  form.nickname = authStore.user?.nickname || ''
  form.avatar = authStore.user?.avatar || ''
  form.email = authStore.user?.email || ''
  form.phone = authStore.user?.phone || ''
}

function validateProfile() {
  errors.nickname = ''
  errors.avatar = ''
  errors.email = ''
  errors.phone = ''
  errors.form = ''

  let valid = true
  if (form.nickname && form.nickname.length > 20) {
    errors.nickname = '当前昵称长度不能超过20位'
    valid = false
  }
  if (form.avatar && form.avatar.length > 255) {
    errors.avatar = '当前头像地址长度不能超过255位'
    valid = false
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '当前邮箱格式不正确'
    valid = false
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    errors.phone = '当前手机号格式不正确'
    valid = false
  }
  return valid
}

async function submitProfile() {
  if (!validateProfile()) {
    return
  }
  saving.value = true
  errors.form = ''
  try {
    const currentUser = authStore.user
    await authStore.updateCurrentUserAction({
      nickname: form.nickname.trim() || currentUser?.nickname || '',
      avatar: form.avatar,
      email: form.email.trim() || currentUser?.email || '',
      phone: form.phone.trim() || currentUser?.phone || '',
    })
    Object.keys(editableFields).forEach((key) => {
      editableFields[key as keyof typeof editableFields] = false
    })
    syncForm()
    ElMessage.success('个人信息已更新')
  } catch (error) {
    const message = error instanceof Error ? error.message : '修改失败'
    if (message.includes('昵称已存在')) {
      errors.nickname = message
    } else if (message.includes('邮箱已存在')) {
      errors.email = message
    } else if (message.includes('手机号已存在')) {
      errors.phone = message
    } else {
      errors.form = message
    }
  } finally {
    saving.value = false
  }
}

function toggleEdit(field: keyof typeof editableFields) {
  const nextState = !editableFields[field]
  editableFields[field] = nextState
  if (nextState && field !== 'avatar') {
    form[field] = ''
    errors[field] = ''
    return
  }
  if (!nextState) {
    syncForm()
  }
}

function isEditableField(key: string): key is keyof typeof editableFields {
  return key === 'nickname' || key === 'email' || key === 'phone' || key === 'avatar'
}

function isFieldEditing(key: string) {
  return isEditableField(key) ? editableFields[key] : false
}

async function handleAvatarChange(rawFile: File) {
  avatarUploading.value = true
  try {
    const croppedAvatar = await createCircularAvatar(rawFile)
    form.avatar = await uploadAvatar(croppedAvatar)
    editableFields.avatar = true
    errors.avatar = ''
    ElMessage.success('头像上传成功')
  } catch (error) {
    const message = error instanceof Error ? error.message : '头像上传失败'
    errors.form = message === '未登录' ? '登录已失效，请重新登录后再上传头像' : message
  } finally {
    avatarUploading.value = false
    uploadRef.value?.clearFiles?.()
  }
}

function handleAvatarSelect(uploadFile: UploadFile) {
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.warning('仅支持 jpg、jpeg、png、webp、gif 格式头像')
    uploadRef.value?.clearFiles?.()
    return
  }
  if (rawFile.size > 10 * 1024 * 1024) {
    ElMessage.warning('原始图片大小不能超过 10MB')
    uploadRef.value?.clearFiles?.()
    return
  }

  void handleAvatarChange(rawFile)
}

async function createCircularAvatar(file: File) {
  const image = await readImage(file)
  const cropSize = Math.min(image.width, image.height)
  const outputSize = Math.min(cropSize, 512)
  const sx = (image.width - cropSize) / 2
  const sy = (image.height - cropSize) / 2
  const canvas = document.createElement('canvas')
  canvas.width = outputSize
  canvas.height = outputSize
  const context = canvas.getContext('2d')

  if (!context) {
    throw new Error('头像裁剪失败')
  }

  context.clearRect(0, 0, outputSize, outputSize)
  context.beginPath()
  context.arc(outputSize / 2, outputSize / 2, outputSize / 2, 0, Math.PI * 2)
  context.closePath()
  context.clip()
  context.drawImage(image, sx, sy, cropSize, cropSize, 0, 0, outputSize, outputSize)

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob((result) => resolve(result), 'image/jpeg', 0.82)
  })
  if (!blob) {
    throw new Error('头像裁剪失败')
  }

  return new File([blob], `${file.name.replace(/\.\w+$/, '') || 'avatar'}.jpg`, { type: 'image/jpeg' })
}

async function readImage(file: File) {
  const objectUrl = URL.createObjectURL(file)
  try {
    const image = new Image()
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve()
      image.onerror = () => reject(new Error('头像读取失败'))
      image.src = objectUrl
    })
    return image
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

onMounted(async () => {
  if (authStore.isLoggedIn) {
    await authStore.fetchCurrentUser().catch(() => null)
  } else if (!authStore.user) {
    await authStore.fetchCurrentUser().catch(() => null)
  }
  syncForm()
})

watch(() => authStore.user, syncForm)

watch(() => form.nickname, (value) => {
  if (!value || value.length <= 20) {
    errors.nickname = ''
  }
})

watch(() => form.avatar, (value) => {
  if (!value || value.length <= 255) {
    errors.avatar = ''
  }
})

watch(() => form.email, (value) => {
  if (!value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    errors.email = ''
  }
})

watch(() => form.phone, (value) => {
  if (!value || /^1\d{10}$/.test(value)) {
    errors.phone = ''
  }
})
</script>

<template>
  <section class="panel page-narrow panel-profile">
    <div class="section-kicker">Profile</div>
    <h1>个人中心</h1>
    <div v-if="authStore.user" class="profile-card">
      <div class="profile-card__top">
        <img :src="displayAvatar" alt="avatar" class="profile-avatar" />
        <div>
          <div class="profile-name">{{ authStore.user.nickname || authStore.user.username }}</div>
          <div class="subtle">@{{ authStore.user.username }}</div>
        </div>
      </div>

      <div class="profile-detail-list">
        <div v-for="item in profileRows" :key="item.key" class="profile-detail-row">
          <div class="profile-detail-row__main">
            <span class="profile-detail-row__label">{{ item.label }}</span>

            <template v-if="item.key === 'nickname' && editableFields.nickname">
              <el-input v-model="form.nickname" maxlength="20" :placeholder="authStore.user?.nickname || '请输入昵称'" />
              <em v-if="errors.nickname" class="field-error">{{ errors.nickname }}</em>
            </template>

            <template v-else-if="item.key === 'email' && editableFields.email">
              <el-input v-model="form.email" :placeholder="authStore.user?.email || '请输入邮箱'" />
              <em v-if="errors.email" class="field-error">{{ errors.email }}</em>
            </template>

            <template v-else-if="item.key === 'phone' && editableFields.phone">
              <el-input v-model="form.phone" :placeholder="authStore.user?.phone || '请输入手机号'" />
              <em v-if="errors.phone" class="field-error">{{ errors.phone }}</em>
            </template>

            <template v-else-if="item.key === 'avatar'">
              <div class="profile-avatar-row">
                <span class="profile-detail-row__value">{{ item.value }}</span>
                <el-upload
                  ref="uploadRef"
                  :show-file-list="false"
                  :auto-upload="false"
                  accept=".jpg,.jpeg,.png,.webp,.gif"
                  :on-change="handleAvatarSelect"
                >
                  <el-button size="small" :loading="avatarUploading">上传头像</el-button>
                </el-upload>
              </div>
              <em v-if="errors.avatar" class="field-error">{{ errors.avatar }}</em>
            </template>

            <span v-else class="profile-detail-row__value">{{ item.value }}</span>
          </div>

          <button
            v-if="item.editable"
            type="button"
            class="ghost-btn profile-edit-trigger"
            @click="isEditableField(item.key) && toggleEdit(item.key)"
          >
            {{ isFieldEditing(item.key) ? '完成' : '修改' }}
          </button>
        </div>
      </div>

      <div class="action-row action-row--profile">
        <em v-if="errors.form" class="field-error">{{ errors.form }}</em>
        <el-button type="primary" :loading="saving || authStore.loading" @click="submitProfile">保存资料</el-button>
      </div>
    </div>
  </section>
</template>
