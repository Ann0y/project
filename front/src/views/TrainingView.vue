<script setup>
const features = [
  '培训课程与学时管理',
  '学时记录与审核',
  '学员在线预约培训时段',
  '时段冲突检测与状态校验',
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">培训与约车</h1>
      <p class="page__summary">
        提供培训课程与学时管理、学时记录与审核功能，支持学员在线预约培训时段，通过时段冲突检测与状态校验避免重复、冲突预约。
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
