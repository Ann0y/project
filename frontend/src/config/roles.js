/**
 * 角色与导航配置（单一事实来源）
 * ---------------------------------------------------------------------------
 * 需求文档第 2 章「用户角色与入口」：
 *   student 学员  ： 我的培训进度 / 培训与约车 / 我的考试与成绩 / 个人中心
 *   coach   教练  ： 教练工作台 / 我的排课 / 预约处理 / 学时登记 / 我的学员
 *   admin   管理员 ： 工作台 + 全部后台功能 + 统计报表
 *
 * 关键设计（对应「首页默认面向学员」的需求）：
 *   1. 首页 `/` 恒定落在学员端「我的培训进度」，游客亦可查看；
 *   2. 顶部导航栏按角色分组，**只显示当前身份所属角色的那一组**：
 *        · 游客       → 只显示「学员端」分组（首页默认视角）
 *        · 学员/教练/管理员 → 只显示自己那一组
 *   3. 游客点击学员端菜单 → 路由守卫拦截 → 跳登录页并携带 redirect。
 */

export const ROLES = {
  admin: {
    key: 'admin',
    name: '管理员',
    short: '管',
    desc: '学员 / 教练 / 车辆 / 考试 / 报表全量管理',
  },
  coach: {
    key: 'coach',
    name: '教练',
    short: '教',
    desc: '排课、预约处理、学时登记与带教学员管理',
  },
  student: {
    key: 'student',
    name: '学员',
    short: '学',
    desc: '培训进度、在线约车、考试成绩自助查询',
  },
}

/** 首页默认面向的角色（游客未登录时据此展示导航） */
export const DEFAULT_ROLE = 'student'

export function roleName(role) {
  return ROLES[role]?.name ?? '访客'
}

/**
 * 演示账户（与后端 DataInitializer 写入的种子账号一致）。
 * 仅用于登录页「一键填入」，真实校验由后端 /api/auth/login 完成。
 * 三个角色各一个，密码统一 123456。
 */
export const DEMO_ACCOUNTS = [
  {
    username: 'student',
    password: '123456',
    role: 'student',
    name: '李小明',
    title: '学员 · C1 在训',
  },
  {
    username: 'coach',
    password: '123456',
    role: 'coach',
    name: '王建国',
    title: '教练 · 工号 C001',
  },
  {
    username: 'admin',
    password: '123456',
    role: 'admin',
    name: '系统管理员',
    title: '管理员 · 全站权限',
  },
]

/**
 * 顶部导航结构：按角色分组。
 * 每个分组自带 `role`，分组内的菜单项继承该角色。
 */
export const NAV_GROUPS = [
  {
    role: 'student',
    title: '学员端',
    items: [
      { name: 'progress', title: '我的培训进度' },
      { name: 'training', title: '培训与约车' },
      { name: 'exams', title: '我的考试与成绩' },
      { name: 'profile', title: '个人中心' },
    ],
  },
  {
    role: 'coach',
    title: '教练端',
    items: [
      { name: 'coach-workbench', title: '教练工作台' },
      { name: 'coach-schedule', title: '我的排课' },
      { name: 'coach-bookings', title: '预约处理' },
      { name: 'coach-hours', title: '学时登记' },
      { name: 'coach-students', title: '我的学员' },
    ],
  },
  {
    role: 'admin',
    title: '管理端',
    items: [
      { name: 'dashboard', title: '工作台' },
      { name: 'students', title: '学员管理' },
      { name: 'resources', title: '教练与车辆' },
      { name: 'admin-training', title: '培训与约车' },
      { name: 'admin-exams', title: '考试管理' },
      { name: 'reports', title: '统计报表' },
      { name: 'users', title: '用户管理' },
    ],
  },
]

/** 折叠后的全部导航项（每项带所属角色），便于按路由 name 反查角色 */
export const NAV_ITEMS = NAV_GROUPS.flatMap((group) =>
  group.items.map((item) => ({ ...item, role: group.role })),
)

/** 按路由 name 取所属角色键 */
export function roleOfRouteName(name) {
  return NAV_ITEMS.find((item) => item.name === name)?.role ?? null
}
