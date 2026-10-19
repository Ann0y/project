<script setup>
/**
 * 页头：标题 + 描述 + 右侧操作区
 */
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  desc: { type: String, default: '' },
})
</script>

<template>
  <header class="page-head">
    <div>
      <span v-if="eyebrow" class="page-head__eyebrow">{{ eyebrow }}</span>
      <h1 class="page-head__title">{{ title }}</h1>
      <p v-if="desc" class="page-head__desc">{{ desc }}</p>
    </div>
    <div v-if="$slots.actions" class="page-head__actions">
      <slot name="actions" />
    </div>
  </header>
</template>
