<script setup>
const features = [
  '管理员、教练、学员等多角色账号管理',
  '基于 RBAC 的功能权限控制',
  '数据权限控制',
  'JWT 无状态登录认证',
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">系统管理</h1>
      <p class="page__summary">
        实现管理员、教练、学员等多角色账号管理，基于 RBAC 模型进行功能权限与数据权限控制，保障系统安全。
      </p>
    </header>

    <el-card shadow="never" class="page__card">
      <template #header>
        <div class="page__card-header">
          <span>功能清单</span>
          <el-tag size="small">开发中</el-tag>
        </div>
      </template>
      <ul class="page__features">
        <li v-for="feature in features" :key="feature">{{ feature }}</li>
      </ul>
    </el-card>
  </section>
</template>
