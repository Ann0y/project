<script setup>
/**
 * 顶部导航栏
 * ---------------------------------------------------------------------------
 * 需求：不同角色 router 有不同的拦截，navbar 隐藏非当前角色的标题；
 *      首页默认面向学员，游客只看到学员端导航。
 *
 * 可见性规则：
 *   游客              → 只显示「学员端」分组
 *   学员/教练/管理员  → 只显示自己所属角色的分组
 * 点击越权路由由 router 守卫兜底拦截并跳转登录页。
 */
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { DEFAULT_ROLE, NAV_GROUPS } from '../config/roles'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mobileOpen = ref(false)
const activeName = computed(() => route.name)

/** 当前身份对应的角色：已登录取自身角色，游客取默认角色（学员） */
const currentRole = computed(() =>
  auth.isAuthenticated ? auth.role : DEFAULT_ROLE,
)

/** 只保留当前角色的分组 —— 其余角色的标签直接隐藏 */
const visibleGroups = computed(() =>
  NAV_GROUPS.filter((group) => group.role === currentRole.value),
)

const roleLabel = computed(() =>
  auth.isAuthenticated ? auth.roleName : '学员端（演示）',
)

function closeMobile() {
  mobileOpen.value = false
}

function onNavClick() {
  closeMobile()
}

async function onUserCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
    auth.logout()
    ElMessage.success('已退出登录')
    router.push({ name: 'progress' })
    return
  }

  if (command === 'switch') {
    auth.logout()
    router.push({ name: 'login', query: { switch: '1' } })
  }
}
</script>

<template>
  <header class="nav">
    <div class="nav__inner">
      <RouterLink class="nav__brand" to="/" @click="closeMobile">
        <span class="nav__logo">驾</span>
        <span class="nav__title">驾校学员培训与考试管理系统</span>
      </RouterLink>

      <!-- 仅当前角色的导航分组 -->
      <nav class="nav__links" aria-label="主导航">
        <span class="nav__role-tag">{{ roleLabel }}</span>
        <RouterLink
          v-for="item in visibleGroups.flatMap((g) => g.items)"
          :key="item.name"
          class="nav-link"
          :class="{ 'nav-link--active': activeName === item.name }"
          :to="{ name: item.name }"
          @click="onNavClick"
        >
          {{ item.title }}
        </RouterLink>
      </nav>

      <div class="nav__aside">
        <el-dropdown v-if="auth.isAuthenticated" trigger="click" @command="onUserCommand">
          <span class="nav__user">
            <span class="nav__avatar">{{ auth.initial }}</span>
            <span class="nav__userinfo">
              <span class="nav__name">{{ auth.displayName }}</span>
              <span class="nav__role">{{ auth.roleName }}</span>
            </span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="switch">切换账号</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <RouterLink v-else class="nav__login" :to="{ name: 'login' }">登录 / 注册</RouterLink>

        <button class="nav__btn nav__burger" type="button" @click="mobileOpen = !mobileOpen">
          {{ mobileOpen ? '收起' : '菜单' }}
        </button>
      </div>
    </div>

    <!-- 移动端展开面板 -->
    <div v-if="mobileOpen" class="nav__drawer">
      <RouterLink
        v-for="item in visibleGroups.flatMap((g) => g.items)"
        :key="item.name"
        class="drawer-link"
        :class="{ 'drawer-link--active': activeName === item.name }"
        :to="{ name: item.name }"
        @click="closeMobile"
      >
        {{ item.title }}
      </RouterLink>
    </div>
  </header>
</template>

<style scoped>
/* 顶部导航：白底 + 下边框区分 */
.nav {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: var(--bg-raised);
  border-bottom: 1px solid var(--border-base);
}

.nav__inner {
  display: flex;
  align-items: center;
  gap: 20px;
  height: var(--nav-height);
  max-width: var(--page-max);
  margin: 0 auto;
  padding: 0 24px;
}

/* 品牌 */
.nav__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  text-decoration: none;
  color: var(--text-primary);
}

.nav__logo {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-size: 14px;
  font-weight: 700;
}

.nav__title {
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
}

/* 导航项 */
.nav__links {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.nav__links::-webkit-scrollbar {
  display: none;
}

.nav__role-tag {
  flex-shrink: 0;
  margin-right: 6px;
  padding: 2px 9px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-base);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.nav-link {
  padding: 6px 11px;
  border-radius: var(--radius-sm);
  font-size: 12.5px;
  font-weight: 550;
  color: var(--text-secondary);
  text-decoration: none;
  white-space: nowrap;
  transition: background 0.18s var(--ease-out), color 0.18s var(--ease-out);
}

.nav-link:hover {
  background: var(--bg-sunken);
  color: var(--text-primary);
}

.nav-link--active {
  background: var(--accent);
  color: var(--text-inverse);
}

/* 右侧 */
.nav__aside {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.nav__btn {
  padding: 6px 11px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-base);
  background: var(--bg-raised);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-family: inherit;
  transition: border-color 0.18s var(--ease-out), color 0.18s var(--ease-out);
}

.nav__btn:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.nav__burger {
  display: none;
}

.nav__user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 6px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-base);
  cursor: pointer;
  outline: none;
  transition: border-color 0.18s var(--ease-out);
}

.nav__user:hover {
  border-color: var(--border-strong);
}

.nav__avatar {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent);
  color: var(--text-inverse);
  font-size: 11px;
  font-weight: 650;
}

.nav__userinfo {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.nav__name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.nav__role {
  font-size: 10px;
  color: var(--text-tertiary);
}

.nav__login {
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  background: var(--accent);
  color: var(--text-inverse);
  font-size: 12.5px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
}

.nav__login:hover {
  background: var(--accent-hover);
}

/* 移动端抽屉 */
.nav__drawer {
  display: none;
  padding: 10px 14px 14px;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-raised);
}

.drawer-link {
  display: block;
  padding: 9px 11px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  text-decoration: none;
}

.drawer-link--active {
  background: var(--accent);
  color: var(--text-inverse);
}

/* 响应式 */
@media (max-width: 1200px) {
  .nav__title {
    font-size: 12px;
  }
}

@media (max-width: 1080px) {
  .nav__links {
    display: none;
  }

  .nav__burger {
    display: block;
  }

  .nav__drawer {
    display: block;
  }
}

@media (max-width: 720px) {
  .nav__inner {
    gap: 10px;
    padding: 0 14px;
  }

  .nav__title,
  .nav__userinfo {
    display: none;
  }
}
</style>
