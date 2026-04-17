<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { checkUserFieldAvailability, register, resetPasswordByPhone } from '../api/user'
import { useAuthStore } from '../stores/auth'

type AuthMode = 'login' | 'register' | 'reset'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeMode = ref<AuthMode>('login')

const submitState = reactive({
  loginError: '',
  loginSuccess: '',
  registerError: '',
  registerSuccess: '',
  resetError: '',
  resetSuccess: '',
})

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
})

const resetForm = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: '',
})

const loginErrors = reactive({
  username: '',
  password: '',
})

const registerErrors = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
})

const resetErrors = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: '',
})

const loginSubmitting = ref(false)
const registerSubmitting = ref(false)
const resetSubmitting = ref(false)
const registerAvailabilityTimers = new Map<'username' | 'nickname' | 'email' | 'phone', ReturnType<typeof setTimeout>>()
const registerAvailabilityLoading = reactive<Record<'username' | 'nickname' | 'email' | 'phone', boolean>>({
  username: false,
  nickname: false,
  email: false,
  phone: false,
})

function switchMode(mode: AuthMode) {
  activeMode.value = mode
}

function clearLoginMessages() {
  submitState.loginError = ''
  submitState.loginSuccess = ''
}

function clearRegisterMessages() {
  submitState.registerError = ''
  submitState.registerSuccess = ''
}

function clearResetMessages() {
  submitState.resetError = ''
  submitState.resetSuccess = ''
}

function validateLogin() {
  loginErrors.username = ''
  loginErrors.password = ''
  clearLoginMessages()
  let valid = true

  if (!loginForm.username.trim()) {
    loginErrors.username = '当前用户名不能为空'
    valid = false
  }
  if (!loginForm.password.trim()) {
    loginErrors.password = '当前密码不能为空'
    valid = false
  }

  return valid
}

function validateRegister() {
  registerErrors.username = ''
  registerErrors.password = ''
  registerErrors.nickname = ''
  registerErrors.email = ''
  registerErrors.phone = ''
  clearRegisterMessages()
  let valid = true

  if (!registerForm.username.trim()) {
    registerErrors.username = '当前用户名不能为空'
    valid = false
  } else if (registerForm.username.trim().length < 4 || registerForm.username.trim().length > 20) {
    registerErrors.username = '当前用户名长度必须在4到20位之间'
    valid = false
  }

  if (!registerForm.password.trim()) {
    registerErrors.password = '当前密码不能为空'
    valid = false
  } else if (registerForm.password.trim().length < 6 || registerForm.password.trim().length > 20) {
    registerErrors.password = '当前密码长度必须在6到20位之间'
    valid = false
  }

  if (!registerForm.nickname.trim()) {
    registerErrors.nickname = '当前昵称不能为空'
    valid = false
  } else if (registerForm.nickname.trim().length > 20) {
    registerErrors.nickname = '当前昵称长度不能超过20位'
    valid = false
  }

  if (registerForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    registerErrors.email = '当前邮箱格式不正确'
    valid = false
  }

  if (registerForm.phone && !/^1\d{10}$/.test(registerForm.phone)) {
    registerErrors.phone = '当前手机号格式不正确'
    valid = false
  }

  return valid
}

function scheduleRegisterAvailabilityCheck(field: 'username' | 'nickname' | 'email' | 'phone', value: string) {
  const existingTimer = registerAvailabilityTimers.get(field)
  if (existingTimer) {
    clearTimeout(existingTimer)
  }
  registerAvailabilityLoading[field] = true
  const timer = setTimeout(async () => {
    try {
      const available = await checkUserFieldAvailability(field, value.trim())
      if (!available) {
        const message = field === 'username'
          ? '当前用户名已存在，请重新填写'
          : field === 'nickname'
            ? '当前昵称已存在，请重新填写'
            : field === 'email'
              ? '当前邮箱已存在，请重新填写'
              : '当前手机号已存在，请重新填写'
        registerErrors[field] = message
      } else if ((registerForm[field] || '').trim() === value.trim()) {
        registerErrors[field] = ''
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : ''
      if ((registerForm[field] || '').trim() === value.trim() && message) {
        registerErrors[field] = message
      }
    } finally {
      if ((registerForm[field] || '').trim() === value.trim()) {
        registerAvailabilityLoading[field] = false
      }
    }
  }, 350)
  registerAvailabilityTimers.set(field, timer)
}

function validateReset() {
  resetErrors.username = ''
  resetErrors.phone = ''
  resetErrors.newPassword = ''
  resetErrors.confirmPassword = ''
  clearResetMessages()
  let valid = true

  if (!resetForm.username.trim()) {
    resetErrors.username = '当前用户名不能为空'
    valid = false
  }
  if (!/^1\d{10}$/.test(resetForm.phone)) {
    resetErrors.phone = '当前手机号格式不正确'
    valid = false
  }
  if (!resetForm.newPassword.trim()) {
    resetErrors.newPassword = '当前新密码不能为空'
    valid = false
  } else if (resetForm.newPassword.trim().length < 6 || resetForm.newPassword.trim().length > 20) {
    resetErrors.newPassword = '当前新密码长度必须在6到20位之间'
    valid = false
  }
  if (!resetForm.confirmPassword.trim()) {
    resetErrors.confirmPassword = '当前确认密码不能为空'
    valid = false
  } else if (resetForm.confirmPassword !== resetForm.newPassword) {
    resetErrors.confirmPassword = '两次输入的新密码不一致'
    valid = false
  }

  return valid
}

async function handleLogin() {
  if (!validateLogin()) {
    return
  }
  loginSubmitting.value = true
  try {
    await authStore.loginAction(loginForm)
    submitState.loginSuccess = '登录成功，正在进入首页'
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } catch (error) {
    submitState.loginError = error instanceof Error ? error.message : '登录失败'
  } finally {
    loginSubmitting.value = false
  }
}

async function handleRegister() {
  if (!validateRegister()) {
    return
  }
  registerSubmitting.value = true
  try {
    await register(registerForm)
    await authStore.loginAction({
      username: registerForm.username.trim(),
      password: registerForm.password,
    })
    submitState.registerSuccess = '注册成功，已自动登录并返回首页'
    ElMessage.success('注册成功，已自动登录')
    router.push('/')
  } catch (error) {
    submitState.registerError = error instanceof Error ? error.message : '注册失败'
  } finally {
    registerSubmitting.value = false
  }
}

async function handleResetPassword() {
  if (!validateReset()) {
    return
  }
  resetSubmitting.value = true
  try {
    await resetPasswordByPhone({
      username: resetForm.username.trim(),
      phone: resetForm.phone.trim(),
      newPassword: resetForm.newPassword,
    })
    submitState.resetSuccess = '修改成功，正在返回登录界面'
    ElMessage.success('密码修改成功')
    loginForm.username = resetForm.username.trim()
    loginForm.password = ''
    resetForm.username = ''
    resetForm.phone = ''
    resetForm.newPassword = ''
    resetForm.confirmPassword = ''
    activeMode.value = 'login'
    router.push('/login')
  } catch (error) {
    submitState.resetError = error instanceof Error ? error.message : '密码重置失败'
  } finally {
    resetSubmitting.value = false
  }
}

watch(() => loginForm.username, (value) => {
  if (value.trim()) {
    loginErrors.username = ''
  }
})

watch(() => loginForm.password, (value) => {
  if (value.trim()) {
    loginErrors.password = ''
  }
})

watch(() => registerForm.username, (value) => {
  const trimmed = value.trim()
  if (!trimmed) {
    registerAvailabilityLoading.username = false
    registerErrors.username = ''
    return
  }
  if (trimmed.length >= 4 && trimmed.length <= 20) {
    registerErrors.username = ''
    scheduleRegisterAvailabilityCheck('username', trimmed)
  }
})

watch(() => registerForm.password, (value) => {
  if (!value.trim()) {
    return
  }
  if (value.trim().length >= 6 && value.trim().length <= 20) {
    registerErrors.password = ''
  }
})

watch(() => registerForm.nickname, (value) => {
  const trimmed = value.trim()
  if (!trimmed) {
    registerAvailabilityLoading.nickname = false
    registerErrors.nickname = ''
    return
  }
  if (trimmed.length <= 20) {
    registerErrors.nickname = ''
    scheduleRegisterAvailabilityCheck('nickname', trimmed)
  }
})

watch(() => registerForm.email, (value) => {
  if (!value) {
    registerErrors.email = ''
    registerAvailabilityLoading.email = false
    return
  }
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    registerErrors.email = ''
    scheduleRegisterAvailabilityCheck('email', value)
  }
})

watch(() => registerForm.phone, (value) => {
  if (!value) {
    registerErrors.phone = ''
    registerAvailabilityLoading.phone = false
    return
  }
  if (/^1\d{10}$/.test(value)) {
    registerErrors.phone = ''
    scheduleRegisterAvailabilityCheck('phone', value)
  }
})

watch(() => resetForm.username, (value) => {
  if (value.trim()) {
    resetErrors.username = ''
  }
})

watch(() => resetForm.phone, (value) => {
  if (!value) {
    resetErrors.phone = ''
    return
  }
  if (/^1\d{10}$/.test(value)) {
    resetErrors.phone = ''
  }
})

watch(() => resetForm.newPassword, (value) => {
  if (!value) {
    resetErrors.newPassword = ''
    return
  }
  if (value.trim().length >= 6 && value.trim().length <= 20) {
    resetErrors.newPassword = ''
  }
  if (resetForm.confirmPassword && resetForm.confirmPassword === value) {
    resetErrors.confirmPassword = ''
  }
})

watch(() => resetForm.confirmPassword, (value) => {
  if (!value) {
    resetErrors.confirmPassword = ''
    return
  }
  if (value === resetForm.newPassword) {
    resetErrors.confirmPassword = ''
  }
})
</script>

<template>
  <div class="auth-stage">
    <div class="auth-stage__atmosphere">
      <div class="auth-stage__sun"></div>
      <div class="auth-stage__cloud auth-stage__cloud--one"></div>
      <div class="auth-stage__cloud auth-stage__cloud--two"></div>
      <div class="auth-stage__tree"></div>
      <div class="auth-stage__box auth-stage__box--left"></div>
      <div class="auth-stage__box auth-stage__box--right"></div>
      <div class="auth-stage__halo"></div>
      <div class="auth-stage__ground"></div>
    </div>

    <section class="auth-card auth-card--single">
      <div class="auth-card__topbar">
        <button type="button" class="ghost-btn link-like auth-back-link" @click="router.push('/')">
          返回主页
        </button>
      </div>
      <div class="auth-card__badge">明</div>
      <h1>明向饭庄</h1>
      <p class="auth-card__subtitle">内容社区账号中心</p>

      <div class="auth-switch">
        <button class="auth-switch__item" :class="{ 'is-active': activeMode === 'login' }" @click="switchMode('login')">
          密码登录
        </button>
        <button class="auth-switch__item" :class="{ 'is-active': activeMode === 'register' }" @click="switchMode('register')">
          注册
        </button>
        <button class="auth-switch__item" :class="{ 'is-active': activeMode === 'reset' }" @click="switchMode('reset')">
          忘记密码
        </button>
      </div>

      <div v-if="activeMode === 'login'" class="auth-form-wrap">
        <p v-if="submitState.loginError" class="form-feedback form-feedback--error">{{ submitState.loginError }}</p>
        <p v-if="submitState.loginSuccess" class="form-feedback form-feedback--success">{{ submitState.loginSuccess }}</p>
        <el-form label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="用户名 / 邮箱">
            <el-input v-model="loginForm.username" />
            <em v-if="loginErrors.username" class="field-error">{{ loginErrors.username }}</em>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password />
            <em v-if="loginErrors.password" class="field-error">{{ loginErrors.password }}</em>
          </el-form-item>
          <div class="auth-form__assist">
            <span>未登录也可以浏览帖子</span>
            <button type="button" class="text-link" @click="switchMode('reset')">忘记密码?</button>
          </div>
          <el-button type="primary" class="auth-submit" :loading="authStore.loading || loginSubmitting" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
      </div>

      <div v-else-if="activeMode === 'register'" class="auth-form-wrap">
        <p v-if="submitState.registerError" class="form-feedback form-feedback--error">{{ submitState.registerError }}</p>
        <p v-if="submitState.registerSuccess" class="form-feedback form-feedback--success">{{ submitState.registerSuccess }}</p>
        <el-form label-position="top" @submit.prevent="handleRegister">
          <el-form-item label="用户名">
            <el-input v-model="registerForm.username" />
            <em v-if="registerAvailabilityLoading.username" class="field-hint">正在检查用户名是否可用...</em>
            <em v-if="registerErrors.username" class="field-error">{{ registerErrors.username }}</em>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" show-password />
            <em v-if="registerErrors.password" class="field-error">{{ registerErrors.password }}</em>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="registerForm.nickname" />
            <em v-if="registerAvailabilityLoading.nickname" class="field-hint">正在检查昵称是否可用...</em>
            <em v-if="registerErrors.nickname" class="field-error">{{ registerErrors.nickname }}</em>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="registerForm.email" />
            <em v-if="registerAvailabilityLoading.email" class="field-hint">正在检查邮箱是否可用...</em>
            <em v-if="registerErrors.email" class="field-error">{{ registerErrors.email }}</em>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="registerForm.phone" />
            <em v-if="registerAvailabilityLoading.phone" class="field-hint">正在检查手机号是否可用...</em>
            <em v-if="registerErrors.phone" class="field-error">{{ registerErrors.phone }}</em>
          </el-form-item>
          <el-button class="auth-submit" :loading="registerSubmitting" @click="handleRegister">注 册</el-button>
        </el-form>
      </div>

      <div v-else class="auth-form-wrap">
        <p v-if="submitState.resetError" class="form-feedback form-feedback--error">{{ submitState.resetError }}</p>
        <p v-if="submitState.resetSuccess" class="form-feedback form-feedback--success">{{ submitState.resetSuccess }}</p>
        <el-form label-position="top" @submit.prevent="handleResetPassword">
          <el-form-item label="用户名">
            <el-input v-model="resetForm.username" />
            <em v-if="resetErrors.username" class="field-error">{{ resetErrors.username }}</em>
          </el-form-item>
          <el-form-item label="完整手机号">
            <el-input v-model="resetForm.phone" />
            <em v-if="resetErrors.phone" class="field-error">{{ resetErrors.phone }}</em>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="resetForm.newPassword" type="password" show-password />
            <em v-if="resetErrors.newPassword" class="field-error">{{ resetErrors.newPassword }}</em>
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="resetForm.confirmPassword" type="password" show-password />
            <em v-if="resetErrors.confirmPassword" class="field-error">{{ resetErrors.confirmPassword }}</em>
          </el-form-item>
          <el-button class="auth-submit" :loading="resetSubmitting" @click="handleResetPassword">确 认 修 改</el-button>
        </el-form>
      </div>

      <div class="auth-card__footer">
        <span>游客可直接浏览帖子</span>
        <button type="button" class="text-link" @click="switchMode(activeMode === 'login' ? 'register' : 'login')">
          {{ activeMode === 'login' ? '立即注册' : '返回登录' }}
        </button>
      </div>
    </section>
  </div>
</template>
