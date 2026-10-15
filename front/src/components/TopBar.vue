<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Calendar,
  DataLine,
  EditPen,
  Odometer,
  Setting,
  TrendCharts,
  User,
  Van,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 路由 name -> 图标，导航项由路由表 meta.nav 派生。
const iconMap = {
  dashboard: Odometer,
  students: User,
  resources: Van,
  training: Calendar,
  progress: TrendCharts,
  exams: EditPen,
  reports: DataLine,
  system: Setting,
}

const navItems = computed(() =>
  router
    .getRoutes()
    .filter((record) => record.meta?.nav)
    // 管理员专属页面不对其他角色展示
    .filter((record) => !record.meta.requiresAdmin || auth.isAdmin)
    .map((record) => ({
      index: record.path,
      title: record.meta.title,
      icon: iconMap[record.name],
      order: record.meta.order ?? 0,
    }))
    .sort((a, b) => a.order - b.order),
)

// 支持子路由高亮：取与当前路径前缀匹配最长的导航项。
const activeIndex = computed(() => {
  const matched = navItems.value
    .filter((item) =>
      item.index === '/' ? route.path === '/' : route.path.startsWith(item.index),
    )
    .sort((a, b) => b.index.length - a.index.length)
  return matched[0]?.index ?? route.path
})

const displayName = computed(() => auth.user?.name || auth.user?.username || '未登录')
const userInitial = computed(() => displayName.value.charAt(0))

function onUserCommand(command) {
  if (command === 'logout') {
    auth.logout()
    ElMessage.success('已退出登录')
    router.push({ name: 'home' })
  } else {
    ElMessage.info('个人中心开发中')
  }
}

function goLogin() {
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="topbar">
    <div class="topbar__inner">
      <RouterLink class="topbar__brand" to="/">
        <span class="topbar__logo">驾</span>
        <span class="topbar__title">驾校学员培训与考试管理系统</span>
      </RouterLink>

      <el-menu
        class="topbar__menu"
        mode="horizontal"
        router
        :default-active="activeIndex"
        :ellipsis="false"
        aria-label="主导航"
      >
        <el-menu-item v-for="item in navItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <el-dropdown
        v-if="auth.isAuthenticated"
        class="topbar__user"
        trigger="click"
        @command="onUserCommand"
      >
        <span class="topbar__user-trigger">
          <el-avatar :size="30" class="topbar__avatar">{{ userInitial }}</el-avatar>
          <span class="topbar__userinfo">
            <span class="topbar__name">{{ displayName }}</span>
            <span class="topbar__role">{{ auth.roleName }}</span>
          </span>
          <el-icon class="topbar__caret"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-button v-else class="topbar__login" type="primary" plain size="small" @click="goLogin">
        登录 / 注册
      </el-button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.topbar__inner {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 60px;
  padding: 0 24px;
  max-width: 1440px;
  margin: 0 auto;
}

.topbar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  color: var(--color-text);
  text-decoration: none;
}

.topbar__logo {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.topbar__title {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.topbar__menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}

.topbar__user,
.topbar__login {
  flex-shrink: 0;
}

.topbar__user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
  border-left: 1px solid var(--color-border);
  cursor: pointer;
  outline: none;
}

.topbar__avatar {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}

.topbar__userinfo {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.topbar__name {
  font-size: 14px;
  color: var(--color-text);
}

.topbar__role {
  font-size: 12px;
  color: var(--color-text-muted);
}

.topbar__caret {
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 720px) {
  .topbar__title,
  .topbar__userinfo {
    display: none;
  }

  .topbar__inner {
    gap: 12px;
    padding: 0 12px;
  }
}
</style>
