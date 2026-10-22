import { createRouter, createWebHistory } from 'vue-router'
import { watch } from 'vue'
import { i18n } from '@/i18n'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { titleKey: 'route.login' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { titleKey: 'route.register' },
    },
    {
      path: '/',
      component: () => import('@/components/AppShell.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { titleKey: 'route.home' },
        },
        {
          path: 'capture',
          name: 'capture',
          component: () => import('@/views/CaptureView.vue'),
          meta: { titleKey: 'route.capture' },
        },
        {
          path: 'library',
          name: 'library',
          component: () => import('@/views/LibraryView.vue'),
          meta: { titleKey: 'route.library' },
        },
        {
          path: 'processing',
          name: 'processing',
          component: () => import('@/views/ProcessingView.vue'),
          meta: { titleKey: 'route.processing' },
        },
        {
          path: 'trash',
          name: 'trash',
          component: () => import('@/views/TrashView.vue'),
          meta: { titleKey: 'route.trash' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { titleKey: 'route.settings' },
        },
        {
          path: 'billing',
          name: 'billing',
          component: () => import('@/views/BillingView.vue'),
          meta: { titleKey: 'route.billing' },
        },
        {
          path: 'billing/return',
          name: 'billing-return',
          component: () => import('@/views/BillingView.vue'),
          meta: { titleKey: 'route.billingReturn' },
        },
        {
          path: 'pricing',
          redirect: '/billing',
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { titleKey: 'route.profile' },
        },
        {
          path: 'library/:id',
          name: 'detail',
          component: () => import('@/views/DetailView.vue'),
          meta: { titleKey: 'route.detail' },
        },
        {
          path: 'library/:id/edit',
          name: 'edit',
          component: () => import('@/views/CaptureView.vue'),
          meta: { titleKey: 'route.edit' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { titleKey: 'route.notFound' },
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

function updateTitle() {
  const key = String(router.currentRoute.value.meta.titleKey || 'route.home')
  document.title = `${i18n.global.t(key)} · ${i18n.global.t('common.appName')}`
}

router.afterEach(updateTitle)
watch(i18n.global.locale, updateTitle)

