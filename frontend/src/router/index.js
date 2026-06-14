import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '总览' }
      },
      {
        path: 'accessories',
        name: 'Accessories',
        component: () => import('@/views/Accessories.vue'),
        meta: { title: '配件耗材建档' }
      },
      {
        path: 'replacements',
        name: 'Replacements',
        component: () => import('@/views/Replacements.vue'),
        meta: { title: '更换周期登记' }
      },
      {
        path: 'worn-status',
        name: 'WornStatus',
        component: () => import('@/views/WornStatus.vue'),
        meta: { title: '损耗状态标注' }
      },
      {
        path: 'groups',
        name: 'Groups',
        component: () => import('@/views/Groups.vue'),
        meta: { title: '物资分组归类' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 乐器配件管理` : '乐器配件管理'
  next()
})

export default router
