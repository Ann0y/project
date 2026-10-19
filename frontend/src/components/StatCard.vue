<script setup>
/** 指标卡：关键数字展示 */
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' },
  hint: { type: String, default: '' },
})
</script>

<template>
  <div class="glass stat-card">
    <span class="stat-card__label">{{ label }}</span>
    <div class="stat-card__value">
      {{ value }}<small v-if="unit">{{ unit }}</small>
    </div>
    <span v-if="hint" class="stat-card__foot">{{ hint }}</span>
  </div>
</template>
