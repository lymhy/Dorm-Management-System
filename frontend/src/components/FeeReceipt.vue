<template>
  <el-dialog
    :model-value="modelValue"
    title="缴费收据"
    width="480px"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div ref="receiptRef" class="receipt">
      <div class="r-head">
        <div class="r-title">员工宿舍管理系统</div>
        <div class="r-sub">水电费缴费收据</div>
      </div>
      <div class="r-meta">
        <span>收据编号：{{ receiptNo }}</span>
        <span>缴费时间：{{ fmtTime(pay.payTime) }}</span>
      </div>
      <table class="r-table">
        <tr><td class="k">房间</td><td class="v">{{ roomText }}</td></tr>
        <tr><td class="k">账单月份</td><td class="v">{{ pay.month || '-' }}</td></tr>
        <tr><td class="k">缴费方式</td><td class="v">{{ pay.payMethod || '线下缴纳' }}</td></tr>
        <tr><td class="k">缴费金额</td><td class="v amount">¥ {{ money(pay.amount) }}</td></tr>
        <tr><td class="k">经办人</td><td class="v">{{ pay.operator || '-' }}</td></tr>
        <tr><td class="k">备注</td><td class="v">{{ pay.remark || '-' }}</td></tr>
      </table>
      <div class="r-foot">
        <span>（本收据为系统生成，盖章有效）</span>
        <span>收费方：___________</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" @click="print"><el-icon><Printer /></el-icon> 打印收据</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { Printer } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  pay: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const receiptNo = computed(() => 'RCP-' + String(props.pay.id ?? 0).padStart(6, '0'))
const roomText = computed(() => {
  const p = props.pay
  if (!p) return '-'
  const b = p.buildingName || (p.buildingId ? p.buildingId + '号楼' : '')
  const r = p.roomNo || p.roomId || ''
  return b ? `${b} - ${r}` : String(r)
})

function money(v) { return Number(v || 0).toFixed(2) }
function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

function print() {
  const p = props.pay || {}
  const html = `
  <html><head><meta charset="utf-8"><title>缴费收据 ${receiptNo.value}</title>
  <style>
    body{font-family:"Microsoft YaHei",Arial,sans-serif;padding:28px;color:#1f2937}
    .head{text-align:center;margin-bottom:14px}
    .title{font-size:19px;font-weight:700}
    .sub{font-size:15px;letter-spacing:3px;margin-top:4px;color:#374151}
    .meta{display:flex;justify-content:space-between;font-size:12px;color:#6b7280;border-bottom:1px dashed #cbd5e1;padding-bottom:8px;margin-bottom:10px}
    table{width:100%;border-collapse:collapse;font-size:14px}
    td{border:1px solid #d1d5db;padding:9px 12px}
    td.k{width:110px;background:#f8fafc;color:#475569}
    .amount{font-size:18px;font-weight:700;color:#2563eb}
    .foot{display:flex;justify-content:space-between;font-size:12px;color:#6b7280;margin-top:26px}
  </style></head><body>
    <div class="head"><div class="title">员工宿舍管理系统</div><div class="sub">水电费缴费收据</div></div>
    <div class="meta"><span>收据编号：${receiptNo.value}</span><span>缴费时间：${fmtTime(p.payTime)}</span></div>
    <table>
      <tr><td class="k">房间</td><td>${roomText.value}</td></tr>
      <tr><td class="k">账单月份</td><td>${p.month || '-'}</td></tr>
      <tr><td class="k">缴费方式</td><td>${p.payMethod || '线下缴纳'}</td></tr>
      <tr><td class="k">缴费金额</td><td class="amount">¥ ${money(p.amount)}</td></tr>
      <tr><td class="k">经办人</td><td>${p.operator || '-'}</td></tr>
      <tr><td class="k">备注</td><td>${p.remark || '-'}</td></tr>
    </table>
    <div class="foot"><span>（本收据为系统生成，盖章有效）</span><span>收费方：___________</span></div>
  </body></html>`
  const w = window.open('', '_blank', 'width=760,height=680')
  if (!w) { alert('请允许弹出窗口以打印收据'); return }
  w.document.write(html)
  w.document.close()
  w.focus()
  setTimeout(() => w.print(), 300)
}
</script>

<style scoped>
.receipt { border: 1px solid #e2e8f0; border-radius: 10px; padding: 18px 20px; background: #fff; }
.r-head { text-align: center; margin-bottom: 12px; }
.r-title { font-size: 17px; font-weight: 700; color: #1e293b; }
.r-sub { font-size: 14px; letter-spacing: 3px; color: #475569; margin-top: 4px; }
.r-meta { display:flex; justify-content:space-between; font-size:12px; color:#64748b; border-bottom:1px dashed #cbd5e1; padding-bottom:8px; margin-bottom:10px; }
.r-table { width:100%; border-collapse: collapse; font-size:13px; }
.r-table td { border:1px solid #e2e8f0; padding:8px 10px; }
.r-table td.k { width:96px; background:#f8fafc; color:#475569; }
.amount { font-size:16px; font-weight:700; color:#2563eb; }
.r-foot { display:flex; justify-content:space-between; font-size:12px; color:#94a3b8; margin-top:18px; }
</style>
