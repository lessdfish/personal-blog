import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElPagination,
  ElUpload,
} from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const elementComponents = [
  ElButton,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElPagination,
  ElUpload,
]

elementComponents.forEach((component) => {
  if (component.name) {
    app.component(component.name, component)
  }
})

app.use(createPinia())
app.use(router)
app.use(ElLoading)
app.mount('#app')
