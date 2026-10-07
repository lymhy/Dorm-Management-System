<template>
  <el-dialog v-model="visible" title="在线缴费收银台" width="440px" :close-on-click-modal="false" @closed="onClosed">
    <div v-loading="loading" class="cashier">
      <template v-if="order">
        <!-- 待支付 -->
        <template v-if="order.status === 'PENDING'">
          <div class="amount-box">
            <div class="amount-label">支付金额</div>
            <div class="amount-num">￥{{ order.amount }}</div>
            <div class="amount-sub">{{ order.subject }}</div>
          </div>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:14px"
            :title="`账单月份：${order.month || '-'}`"
            :description="`订单号 ${order.orderNo}，30 分钟内未支付将自动关闭。`" />
          <div class="channel-row">
            <div class="channel selected">
              <span class="ch-dot"></span>
              <div>
                <div class="ch-name">模拟支付通道</div>
                <div class="ch-desc">演示环境 · 确认后立即到账（真实环境可接入微信/支付宝）</div>
              </div>
            </div>
          </div>
        </template>

        <!-- 成功 -->
        <template v-else-if="order.status === 'SUCCESS'">
          <div class="pay-ok">
            <div class="ok-icon">
              <svg viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="22" fill="#E8F7EE" stroke="#22A45D" stroke-width="2"/><path d="M15 24.5l6 6 12-13" stroke="#22A45D" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </div>
            <div class="ok-title">支付成功</div>
            <div class="ok-amount">￥{{ order.amount }}</div>
            <div class="ok-sub">{{ order.month || '' }} 水电费已缴纳 · 订单号 {{ order.orderNo }}</div>
          </div>
        </template>

        <!-- 已关闭 -->
        <template v-else>
          <el-result icon="warning" title="订单已关闭" sub-title="订单超时或已取消，可返回重新发起支付" />
        </template>
      </template>
    </div>
    <template #footer>
      <template v-if="order && order.status === 'PENDING'">
        <el-button @click="cancel">取消支付</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">确认支付 ￥{{ order.amount }}</el-button>
      </template>
      <el-button v-else type="primary" @click="visible = false">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import payApi from '../api/pay'

const visible = ref(false)
const loading = ref(false)
const paying = ref(false)
const order = ref(null)
let timer = null

const emit = defineEmits(['paid'])

function open(payload) {
  // payload: { orderNo, amount, month, subject, status }
  order.value = { ...payload }
  visible.value = true
  if (payload.orderNo) refresh()
}
defineExpose({ open })

async function refresh() {
  try {
    const r = await payApi.orderDetail(order.value.orderNo)
    if (r.data) order.value = { ...order.value, ...r.data }
  } catch (e) { /* 保留本地信息 */ }
}

async function confirmPay() {
  paying.value = true
  try {
    const r = await payApi.confirm(order.value.orderNo)
    if (r.code === 200) {
      ElMessage.success('支付成功，账单已核销')
      await refresh()
      emit('paid', order.value)
    }
  } catch (e) { /* request.js 已提示 */ }
  finally { paying.value = false }
}

async function cancel() {
  try { await payApi.close(order.value.orderNo, '用户取消支付'); ElMessage.info('已取消支付') }
  catch (e) { /* ignore */ }
  visible.value = false
}

function onClosed() { order.value = null }
onBeforeUnmount(() => { if (timer) clearTimeout(timer) })
</script>

<style scoped>
.cashier { min-height: 220px; }
.amount-box { text-align: center; padding: 6px 0 14px; }
.amount-label { font-size: 12px; color: #94a3b8; }
.amount-num { font-size: 34px; font-weight: 700; color: #1e293b; line-height: 1.3; }
.amount-sub { font-size: 13px; color: #64748b; }
.channel-row { display: flex; flex-direction: column; gap: 8px; }
.channel { display: flex; align-items: center; gap: 10px; border: 1.5px solid #2563EB; background: #EFF6FF; border-radius: 8px; padding: 10px 12px; }
.ch-dot { width: 10px; height: 10px; border-radius: 50%; background: #2563EB; flex: none; }
.ch-name { font-size: 14px; font-weight: 600; color: #1e293b; }
.ch-desc { font-size: 12px; color: #94a3b8; }
.pay-ok { text-align: center; padding: 12px 0 6px; }
.ok-icon svg { width: 56px; height: 56px; }
.ok-title { font-size: 17px; font-weight: 700; color: #1e293b; margin-top: 8px; }
.ok-amount { font-size: 26px; font-weight: 700; color: #22A45D; margin: 4px 0; }
.ok-sub { font-size: 12px; color: #94a3b8; word-break: break-all; }
</style>