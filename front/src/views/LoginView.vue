<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  // 从注册页跳转过来时带回用户名，省去重复输入
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
  remember: true,
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="登录" subtitle="欢迎回来，请登录后继续">
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
        />
      </el-form-item>

      <div class="auth-form__row">
        <el-checkbox v-model="form.remember">记住我</el-checkbox>
        <el-link type="primary" :underline="false">忘记密码？</el-link>
      </div>

      <el-button
        type="primary"
        class="auth-form__submit"
        :loading="loading"
        native-type="submit"
      >
        登录
      </el-button>

      <div class="auth-form__footer">
        还没有账号？
        <RouterLink class="auth-form__link" :to="{ name: 'register' }">创建账户</RouterLink>
      </div>
    </el-form>

    <el-alert
      class="auth-form__tip"
      type="info"
      :closable="false"
      show-icon
      title="演示账号：管理员 admin / 123456，学员 student / 123456"
    />
  </AuthShell>
</template>

<style scoped>
.auth-form__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.auth-form__submit {
  width: 100%;
}

.auth-form__footer {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

.auth-form__link {
  color: var(--el-color-primary);
  text-decoration: none;
}

.auth-form__link:hover {
  text-decoration: underline;
}

.auth-form__tip {
  margin-top: 20px;
}
</style>
