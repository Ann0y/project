<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div class="auth">
    <div class="auth__card">
      <div class="auth__brand">
        <span class="auth__logo">驾</span>
        <span class="auth__app">驾校学员培训与考试管理系统</span>
      </div>

      <h1 class="auth__title">{{ title }}</h1>
      <p v-if="subtitle" class="auth__subtitle">{{ subtitle }}</p>

      <slot />
    </div>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: radial-gradient(1200px 600px at 50% -10%, #e8f1ff 0%, var(--color-bg) 60%);
}

.auth__card {
  width: 100%;
  max-width: 400px;
  padding: 32px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
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
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.auth__app {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.auth__title {
  margin: 0 0 4px;
  font-size: 22px;
  color: var(--color-text);
}

.auth__subtitle {
  margin: 0 0 24px;
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>
