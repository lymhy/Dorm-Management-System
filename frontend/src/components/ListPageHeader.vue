<template>
  <div class="page-header">
    <!-- 标题行 -->
    <div class="ph-row">
      <div class="ph-left">
        <h1 class="ph-title">{{ title }}</h1>
        <span v-if="subtitle" class="ph-sub">{{ subtitle }}</span>
      </div>
      <div class="ph-right" v-if="$slots.actions">
        <slot name="actions" />
      </div>
    </div>
    <!-- 统计行 -->
    <div v-if="stats && stats.length" class="ph-stats">
      <div v-for="s in stats" :key="s.label" class="stat-chip">
        <span class="stat-num">{{ s.value }}</span>
        <span class="stat-lbl">{{ s.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  stats: { type: Array, default: () => [] },
})
</script>

<style scoped>
.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px 12px;
  margin-bottom: 14px;
  box-shadow: 0 1px 2px rgba(0,0,0,.05);
  border: 1px solid #f1f5f9;
}
.ph-row {
  display: flex; align-items: center; justify-content: space-between;
}
.ph-left { display: flex; align-items: baseline; gap: 10px; }
.ph-title {
  margin: 0; font-size: 17px; font-weight: 700; color: #1e293b;
  line-height: 1.2;
}
.ph-sub {
  font-size: 13px; color: #94a3b8;
}
.ph-right { display: flex; gap: 8px; }
.ph-stats {
  display: flex; gap: 8px; flex-wrap: wrap;
  margin-top: 10px; padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}
.stat-chip {
  display: flex; align-items: center; gap: 5px;
  background: #f8fafc; border: 1px solid #e2e8f0;
  border-radius: 6px; padding: 4px 10px;
}
.stat-num { font-size: 15px; font-weight: 700; color: #2563EB; }
.stat-lbl { font-size: 12px; color: #64748b; }
</style>
