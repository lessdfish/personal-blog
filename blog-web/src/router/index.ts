import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import ArticleDetailView from '../views/ArticleDetailView.vue'
import NotifyView from '../views/NotifyView.vue'
import PublishView from '../views/PublishView.vue'
import ProfileView from '../views/ProfileView.vue'
import AppLayout from '../views/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'article/:id', name: 'article-detail', component: ArticleDetailView, props: true },
        { path: 'notify', name: 'notify', component: NotifyView, meta: { requiresAuth: true } },
        { path: 'publish', name: 'publish', component: PublishView, meta: { requiresAuth: true } },
        { path: 'profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.initialized) {
    await authStore.fetchCurrentUser().catch(() => null)
  }
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
