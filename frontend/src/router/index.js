import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { roleOfRouteName, roleName } from '../config/roles'

/**
 * 路由表
 * ---------------------------------------------------------------------------
 * meta 约定：
 *   public    免登录页面（登录 / 注册）
 *   blank     不渲染顶部导航栏
 *   title     页面标题（用于 document.title 与导航文案）
 *   role      该页面所属角色；访问者角色不符时会被守卫拦到登录页
 *
 * 需求要点：首页 `/` 默认面向学员，恒定重定向到「我的培训进度」。
 */
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
    meta: { title: '注册', public: true, blank: true },
  },

  /* 首页：默认面向学员的「我的培训进度」 */
  {
    path: '/',
    name: 'home',
    redirect: { name: 'progress' },
  },

  // ---------------------------- 学员端 F01–F04 ----------------------------
  {
    path: '/progress',
    name: 'progress',
    component: () => import('../views/student/ProgressView.vue'),
    // guest 表示游客也能查看（首页默认面向学员），仅拦截「已登录但角色不符」
    meta: { title: '我的培训进度', role: 'student', guest: true },
  },
  {
    path: '/training',
    name: 'training',
    component: () => import('../views/student/TrainingView.vue'),
    meta: { title: '培训与约车', role: 'student' },
  },
  {
    path: '/exams',
    name: 'exams',
    component: () => import('../views/student/ExamView.vue'),
    meta: { title: '我的考试与成绩', role: 'student' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/student/ProfileView.vue'),
    meta: { title: '个人中心', role: 'student' },
  },

  // ---------------------------- 教练端 F05–F09 ----------------------------
  {
    path: '/coach/workbench',
    name: 'coach-workbench',
    component: () => import('../views/coach/WorkbenchView.vue'),
    meta: { title: '教练工作台', role: 'coach' },
  },
  {
    path: '/coach/schedule',
    name: 'coach-schedule',
    component: () => import('../views/coach/ScheduleView.vue'),
    meta: { title: '我的排课', role: 'coach' },
  },
  {
    path: '/coach/bookings',
    name: 'coach-bookings',
    component: () => import('../views/coach/BookingView.vue'),
    meta: { title: '预约处理', role: 'coach' },
  },
  {
    path: '/coach/hours',
    name: 'coach-hours',
    component: () => import('../views/coach/HoursView.vue'),
    meta: { title: '学时登记', role: 'coach' },
  },
  {
    path: '/coach/students',
    name: 'coach-students',
    component: () => import('../views/coach/StudentsView.vue'),
    meta: { title: '我的学员', role: 'coach' },
  },

  // --------------------------- 管理端 F10–F16 ----------------------------
  {
    path: '/admin/dashboard',
    name: 'dashboard',
    component: () => import('../views/admin/DashboardView.vue'),
    meta: { title: '工作台', role: 'admin' },
  },
  {
    path: '/admin/students',
    name: 'students',
    component: () => import('../views/admin/StudentView.vue'),
    meta: { title: '学员管理', role: 'admin' },
  },
  {
    path: '/admin/resources',
    name: 'resources',
    component: () => import('../views/admin/ResourceView.vue'),
    meta: { title: '教练与车辆', role: 'admin' },
  },
  {
    path: '/admin/training',
    name: 'admin-training',
    component: () => import('../views/admin/TrainingView.vue'),
    meta: { title: '培训与约车', role: 'admin' },
  },
  {
    path: '/admin/exams',
    name: 'admin-exams',
    component: () => import('../views/admin/ExamView.vue'),
    meta: { title: '考试管理', role: 'admin' },
  },
  {
    path: '/admin/reports',
    name: 'reports',
    component: () => import('../views/admin/ReportView.vue'),
    meta: { title: '统计报表', role: 'admin' },
  },
  {
    path: '/admin/users',
    name: 'users',
    component: () => import('../views/admin/UserView.vue'),
    meta: { title: '用户管理', role: 'admin' },
  },

  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, saved) {
    return saved ?? { top: 0 }
  },
})

const APP_NAME = '驾校学员培训与考试管理系统'

/** 各角色登录后的落地页 */
export function homeFor(role) {
  if (role === 'admin') return { name: 'dashboard' }
  if (role === 'coach') return { name: 'coach-workbench' }
  return { name: 'progress' }
}

/**
 * 全局前置守卫 ： 按角色拦截
 * ---------------------------------------------------------------------------
 * 1. 首页 `/` 对所有人开放（默认学员视角）；
 * 2. 免登录页（登录 / 注册）已登录时直接回到本角色首页；
 * 3. 需要角色 `meta.role` 的页面：
 *      · 未登录      → 跳登录页，带 redirect（登录后回跳）
 *      · 角色不匹配  → 跳登录页，提示需切换对应角色账号
 */
router.beforeEach((to) => {
  const auth = useAuthStore()

  // 已登录用户访问登录 / 注册页 → 回到本角色首页
  if (to.meta.public) {
    return auth.isAuthenticated ? homeFor(auth.role) : true
  }

  const needRole = to.meta.role ?? roleOfRouteName(to.name)

  // 首页与无角色要求的页面放行
  if (!needRole) return true

  // 游客可访问的页面（首页默认面向学员）：未登录时直接放行
  if (!auth.isAuthenticated && to.meta.guest) return true

  // 未登录：拦截到登录页，并记住原本想去哪
  if (!auth.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath, need: needRole },
    }
  }

  // 已登录但角色不符：提示后回到本角色首页
  // （不跳登录页，否则会被「已登录不再进登录页」的规则再弹回，形成来回跳转）
  if (auth.role !== needRole) {
    ElMessage.warning(
      `「${to.meta.title}」属于${roleName(needRole)}端，当前登录角色为${roleName(auth.role)}`,
    )
    return homeFor(auth.role)
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · ${APP_NAME}` : APP_NAME
})

export default router
