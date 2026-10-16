<script setup>
/**
 * 登录 / 注册页外壳：玻璃卡片 + 品牌区
 */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div class="auth">
    <div class="auth__card glass">
      <div class="auth__brand">
        <span class="auth__logo">驾</span>
        <span class="auth__app">
          <strong>驾校学员培训与考试管理系统</strong>
          <small>Driving School Management</small>
        </span>
      </div>

      <h1 class="auth__title">{{ title }}</h1>
      <p v-if="subtitle" class="auth__subtitle">{{ subtitle }}</p>

      <slot />
    </div>

    <RouterLink class="auth__home" to="/">← 返回首页（学员端）</RouterLink>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 18px;
  padding: 32px 20px;
}

.auth__card {
  width: 100%;
  max-width: 420px;
  padding: 30px 30px 26px;
}

.auth__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.auth__logo {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--accent);
  color: var(--text-inverse);
  font-size: 16px;
  font-weight: 700;
}

.auth__app {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.auth__app strong {
  font-size: 12.5px;
  font-weight: 650;
  color: var(--text-primary);
}

.auth__app small {
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.auth__title {
  margin: 0 0 4px;
  font-size: 23px;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.auth__subtitle {
  margin: 0 0 22px;
  font-size: 13px;
  color: var(--text-secondary);
}

.auth__home {
  font-size: 12px;
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color 0.2s var(--ease-out);
}

.auth__home:hover {
  color: var(--text-primary);
}
</style>
