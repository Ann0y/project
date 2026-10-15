import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

// 导航项与路由一一对应，`meta.nav` 标记的条目会出现在顶部导航栏中。
// 模块划分依据《开题报告》中“三、主要内容”的系统功能模块。
// `meta.public`       无需登录即可访问（登录 / 注册页）
// `meta.blank`        不显示顶部导航栏
// `meta.requiresAdmin` 仅管理员可访问
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录', public: true, blank: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { title: '创建账户', public: true, blank: true },
  },
  {
    // 站点首页：管理员进入工作台，其他人进入我的培训进度
    path: '/',
    name: 'home',
    redirect: () => (useAuthStore().isAdmin ? { name: 'dashboard' } : { name: 'progress' }),
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { nav: true, order: 1, title: '工作台', requiresAdmin: true },
  },
  {
    path: '/students',
    name: 'students',
    component: () => import('../views/StudentView.vue'),
    meta: { nav: true, order: 2, title: '学员管理', requiresAdmin: true },
  },
  {
    path: '/resources',
    name: 'resources',
    component: () => import('../views/CoachVehicleView.vue'),
    meta: { nav: true, order: 3, title: '教练与车辆' },
  },
  {
    path: '/training',
    name: 'training',
    component: () => import('../views/TrainingView.vue'),
    meta: { nav: true, order: 4, title: '培训与约车' },
  },
  {
    path: '/progress',
    name: 'progress',
    component: () => import('../views/ProgressView.vue'),
    meta: { nav: true, order: 0, title: '我的培训进度' },
  },
  {
    path: '/exams',
    name: 'exams',
    component: () => import('../views/ExamView.vue'),
    meta: { nav: true, order: 7, title: '考试管理', requiresAdmin: true },
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('../views/ReportView.vue'),
    meta: { nav: true, order: 8, title: '统计报表', requiresAdmin: true },
  },
  {
    path: '/system',
    name: 'system',
    component: () => import('../views/SystemView.vue'),
    meta: { nav: true, order: 9, title: '系统管理' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

const APP_NAME = '驾校学员培训与考试管理系统'

router.beforeEach((to) => {
  const auth = useAuthStore()

  // 已登录用户访问登录 / 注册页时直接进入首页
  if (to.meta.public && auth.isAuthenticated) {
    return auth.isAdmin ? { name: 'dashboard' } : { name: 'progress' }
  }

  // 管理员专属页面
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    if (!auth.isAuthenticated) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    ElMessage.warning('该页面仅管理员可访问')
    return { name: 'progress' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - ${APP_NAME}` : APP_NAME
})

export default router
