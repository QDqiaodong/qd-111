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
      },
      {
        path: 'sets',
        name: 'Sets',
        component: () => import('@/views/Sets.vue'),
        meta: { title: '套装耗材档案' }
      },
      {
        path: 'spec-comparison',
        name: 'SpecComparison',
        component: () => import('@/views/SpecComparison.vue'),
        meta: { title: '耗材规格对照' }
      },
      {
        path: 'group-display',
        name: 'GroupDisplay',
        component: () => import('@/views/GroupDisplay.vue'),
        meta: { title: '分组陈列看板' }
      },
      {
        path: 'replacement-calendar',
        name: 'ReplacementCalendar',
        component: () => import('@/views/ReplacementCalendar.vue'),
        meta: { title: '更换节奏日历' }
      },
      {
        path: 'maintenance-plan',
        name: 'MaintenancePlan',
        component: () => import('@/views/MaintenancePlan.vue'),
        meta: { title: '乐器保养计划' }
      },
      {
        path: 'preparation-checklists',
        name: 'PreparationChecklists',
        component: () => import('@/views/PreparationChecklists.vue'),
        meta: { title: '更换前准备清单' }
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
