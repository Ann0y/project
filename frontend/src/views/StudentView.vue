<script setup>
const features = [
  '学员报名与信息录入',
  '学员档案维护与查看',
  '缴费记录管理',
  '培训进度跟踪',
  '按姓名、证件号、报名状态等条件组合查询',
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">学员管理</h1>
      <p class="page__summary">
        实现学员报名、档案维护、缴费记录与培训进度跟踪，支持按姓名、证件号、报名状态等条件进行多条件组合查询。
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
