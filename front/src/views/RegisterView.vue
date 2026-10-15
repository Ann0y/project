<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Phone, User, UserFilled } from '@element-plus/icons-vue'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  name: '',
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

function validateConfirm(rule, value, callback) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.register({
      username: form.username,
      password: form.password,
      name: form.name,
      phone: form.phone,
    })
    ElMessage.success('账户创建成功，请登录')
    router.replace({ name: 'login', query: { username: form.username } })
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="创建账户" subtitle="填写以下信息完成注册">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="onSubmit"
    >
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" placeholder="请输入真实姓名" :prefix-icon="UserFilled" />
      </el-form-item>

      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="3-20 个字符" :prefix-icon="User" clearable />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Phone" clearable />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="至少 6 位"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-button
        type="primary"
        class="auth-form__submit"
        :loading="loading"
        native-type="submit"
      >
        创建账户
      </el-button>

      <div class="auth-form__footer">
        已有账号？
        <RouterLink class="auth-form__link" :to="{ name: 'login' }">去登录</RouterLink>
      </div>
    </el-form>
  </AuthShell>
</template>

<style scoped>
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
</style>
