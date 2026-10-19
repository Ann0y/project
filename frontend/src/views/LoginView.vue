<script setup>
/**
 * 登录页
 * ---------------------------------------------------------------------------
 * 需求：点击非本角色的 navbar 标题 → 先跳转到登录页，并设立三个角色的账户。
 * 因此本页：
 *   1. 从 query.need 判断是哪个角色触发的拦截，自动预选并填入该角色账号；
 *   2. 提供三个演示账号的一键填入；
 *   3. 登录成功后按角色跳回 redirect 或对应首页。
 */
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../stores/auth'
import { DEMO_ACCOUNTS, ROLES, roleName } from '../config/roles'
import { homeFor } from '../router'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const form = reactive({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/** 触发本次登录的角色（来自路由守卫的 query.need） */
const needRole = computed(() => {
  const need = route.query.need
  return typeof need === 'string' && ROLES[need] ? need : null
})

const needText = computed(() => (needRole.value ? roleName(needRole.value) : ''))

/** 演示账号，被拦截的角色排在最前 */
const accounts = computed(() => {
  const list = [...DEMO_ACCOUNTS]
  if (!needRole.value) return list
  return list.sort((a, b) =>
    a.role === needRole.value ? -1 : b.role === needRole.value ? 1 : 0,
  )
})

// 被拦截时自动填入该角色演示账号，减少答辩演示操作
watch(
  needRole,
  (role) => {
    if (!role) return
    const account = DEMO_ACCOUNTS.find((item) => item.role === role)
    if (account) {
      form.username = account.username
      form.password = account.password
    }
  },
  { immediate: true },
)

function useAccount(account) {
  form.username = account.username
  form.password = account.password
  ElMessage.info(`已填入${roleName(account.role)}演示账号，点击登录继续`)
}

/** 各角色路由前缀，用于判断 redirect 是否属于当前登录角色 */
function routeFor(role) {
  if (role === 'admin') return '/admin'
  if (role === 'coach') return '/coach'
  return '/progress'
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const user = await auth.login({ username: form.username, password: form.password })
    ElMessage.success(`欢迎回来，${user.name}`)

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    // 仅当 redirect 属于当前登录角色时才回跳，避免再次被守卫弹回
    if (redirect && redirect.startsWith(routeFor(user.role))) {
      router.replace(redirect)
    } else {
      router.replace(homeFor(user.role))
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  }
}

function onForgot() {
  ElMessage.info('演示环境：三个账号密码统一为 123456')
}
</script>

<template>
  <AuthShell
    title="登录"
    :subtitle="
      needRole
        ? `该功能属于${needText}端，请使用${needText}账号登录`
        : '欢迎回来，请登录后继续'
    "
  >
    <el-alert
      v-if="needRole"
      class="login__notice"
      type="warning"
      :closable="false"
      show-icon
      :title="`「${route.query.redirect}」需要${needText}权限`"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="onSubmit"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          :prefix-icon="Lock"
          show-password
          @keyup.enter="onSubmit"
        />
      </el-form-item>

      <div class="login__row">
        <el-checkbox :model-value="true">记住我</el-checkbox>
        <el-link type="primary" underline="never" @click="onForgot">忘记密码？</el-link>
      </div>

      <el-button
        type="primary"
        class="login__submit"
        size="large"
        :loading="auth.loading"
        native-type="submit"
      >
        登录
      </el-button>

      <div class="login__footer">
        还没有账号？
        <RouterLink class="login__link" :to="{ name: 'register' }">注册学员账号</RouterLink>
      </div>
    </el-form>

    <!-- 三个角色演示账号 -->
    <div class="login__accounts">
      <div class="login__accounts-head">
        <span>演示账号 · 一键填入</span>
        <span class="u-dim">密码均为 123456</span>
      </div>

      <button
        v-for="account in accounts"
        :key="account.username"
        type="button"
        class="account"
        :class="{ 'account--need': account.role === needRole }"
        @click="useAccount(account)"
      >
        <span class="account__badge">{{ ROLES[account.role].short }}</span>
        <span class="account__body">
          <span class="account__name">
            {{ account.name }}
            <em>{{ roleName(account.role) }}</em>
          </span>
          <span class="account__cred">{{ account.username }} / {{ account.password }}</span>
        </span>
        <span class="account__go">填入 →</span>
      </button>
    </div>
  </AuthShell>
</template>

<style scoped>
.login__notice {
  margin-bottom: 18px;
  border-radius: var(--radius-md);
}

.login__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.login__submit {
  width: 100%;
}

.login__footer {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.login__link {
  color: var(--text-primary);
  font-weight: 600;
  text-decoration: none;
  border-bottom: 1px solid var(--border-strong);
}

.login__accounts {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login__accounts-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--text-secondary);
}

.account {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 11px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-base);
  background: var(--bg-sunken);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: border-color 0.2s var(--ease-out), background-color 0.2s var(--ease-out),
    transform 0.2s var(--ease-out);
}

.account:hover {
  border-color: var(--border-strong);
  background: var(--bg-raised);
  transform: translateX(2px);
}

.account--need {
  border-color: var(--text-primary);
  background: var(--bg-raised);
}

.account__badge {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-size: 12px;
  font-weight: 700;
}

.account__body {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
  flex: 1;
}

.account__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.account__name em {
  font-style: normal;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-base);
  color: var(--text-tertiary);
}

.account__cred {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-tertiary);
}

.account__go {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.account:hover .account__go {
  color: var(--text-primary);
}
</style>
