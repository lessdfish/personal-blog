import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/**
 * 懒加载页面组件 AppLayout：路由进入时再加载对应视图。
 */
/**
 * 懒加载页面组件 ProfileView：路由进入时再加载对应视图。
 */
/**
 * 懒加载页面组件 PublishView：路由进入时再加载对应视图。
 */
/**
 * 懒加载页面组件 NotifyView：路由进入时再加载对应视图。
 */
/**
 * 懒加载页面组件 ArticleDetailView：路由进入时再加载对应视图。
 */
/**
 * 懒加载页面组件 HomeView：路由进入时再加载对应视图。
 */
/**
 * 懒加载页面组件 LoginView：路由进入时再加载对应视图。
 */
const LoginView = () => import('../views/LoginView.vue')
const HomeView = () => import('../views/HomeView.vue')
const ArticleDetailView = () => import('../views/ArticleDetailView.vue')
const NotifyView = () => import('../views/NotifyView.vue')
const PublishView = () => import('../views/PublishView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const AppLayout = () => import('../views/AppLayout.vue')

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
