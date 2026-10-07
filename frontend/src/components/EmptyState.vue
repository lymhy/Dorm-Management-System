<template>
  <div class="empty-state">
    <div class="empty-icon">
      <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="8" y="16" width="48" height="36" rx="4" fill="#EFF6FF" stroke="#BFDBFE" stroke-width="2"/>
        <path d="M8 24h48" stroke="#BFDBFE" stroke-width="2"/>
        <rect x="14" y="30" width="20" height="3" rx="1.5" fill="#BFDBFE"/>
        <rect x="14" y="37" width="14" height="3" rx="1.5" fill="#BFDBFE"/>
        <rect x="14" y="44" width="17" height="3" rx="1.5" fill="#BFDBFE"/>
        <circle cx="48" cy="48" r="10" fill="#2563EB" opacity="0.1"/>
        <path d="M44 48h8M48 44v8" stroke="#2563EB" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p class="empty-desc">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '暂无相关记录，请先添加数据' },
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.empty-icon { margin-bottom: 16px; }
.empty-icon svg { width: 80px; height: 80px; }
.empty-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}
.empty-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: #94a3b8;
}
</style>
