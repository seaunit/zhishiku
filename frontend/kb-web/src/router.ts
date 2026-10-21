import { createRouter, createWebHistory } from 'vue-router'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录识海' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '创建账户' },
    },
    {
      path: '/',
      component: () => import('@/components/AppShell.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '搜索知识' },
        },
        {
          path: 'capture',
          name: 'capture',
          component: () => import('@/views/CaptureView.vue'),
          meta: { title: '新建知识' },
        },
        {
          path: 'library',
          name: 'library',
          component: () => import('@/views/LibraryView.vue'),
          meta: { title: '知识库' },
        },
        {
          path: 'processing',
          name: 'processing',
          component: () => import('@/views/ProcessingView.vue'),
          meta: { title: '待处理' },
        },
        {
          path: 'trash',
          name: 'trash',
          component: () => import('@/views/TrashView.vue'),
          meta: { title: '回收站' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { title: '设置' },
        },
        {
          path: 'library/:id',
          name: 'detail',
          component: () => import('@/views/DetailView.vue'),
          meta: { title: '知识详情' },
        },
        {
          path: 'library/:id/edit',
          name: 'edit',
          component: () => import('@/views/CaptureView.vue'),
          meta: { title: '编辑知识' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '识海')} · 识海`
})

