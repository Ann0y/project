<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TopBar from './components/TopBar.vue'

const route = useRoute()
const showTopBar = computed(() => !route.meta.blank)
</script>

<template>
  <TopBar v-if="showTopBar" />
  <main :class="{ container: showTopBar }">
    <RouterView />
  </main>
</template>
