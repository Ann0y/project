<script setup>
const features = [
  '各科目考试安排',
  '考试报名',
  '成绩录入与合格状态管理',
  '补考记录维护',
  '考试数据分析',
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">考试管理</h1>
      <p class="page__summary">
        实现各科目考试安排、考试报名、成绩录入与合格状态管理，支持补考记录维护与考试数据分析。
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
