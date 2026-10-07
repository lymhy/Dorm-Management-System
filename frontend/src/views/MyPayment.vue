<template>
  <div class="page-container">
    <ListPageHeader
      title="在线缴费" subtitle="我的水电费账单在线支付与缴费订单记录"
      :stats="[
        {label:'未缴账单',value:unpaidCount},
        {label:'未缴金额',value:'￥'+unpaidAmount},
        {label:'支付订单',value:orders.length},
        {label:'支付成功',value:paidOrders}
      ]"
    />

    <div class="card">
      <div class="section-title">我的账单</div>
      <el-table :data="fees" border stripe v-loading="loading">
        <el-table-column prop="month" label="月份" width="90" />
        <el-table-column label="水费" width="90" align="right"><template #default="s">￥{{ s.row.waterFee ?? 0 }}</template></el-table-column>
        <el-table-column label="电费" width="90" align="right"><template #default="s">￥{{ s.row.elecFee ?? 0 }}</template></el-table-column>
        <el-table-column label="合计" width="100" align="right">
          <template #default="s"><strong style="color:#2563EB">￥{{ s.row.total ?? 0 }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="s">
            <el-tag :type="s.row.paid===1?'success':'danger'" size="small">{{ s.row.paid===1?'已缴':'未缴' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="应缴日期" width="120" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="s">
            <el-button v-if="s.row.paid!==1" type="primary" size="small" @click="startPay(s.row)">
              在线支付
            </el-button>
            <el-tag v-else type="success" size="small" effect="plain">已缴清</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无账单" description="入住后管理员生成的水电费账单会显示在这里" />
        </template>
      </el-table>

      <div class="section-title" style="margin-top:18px">我的支付订单</div>
      <el-table :data="orders" border stripe v-loading="orderLoading">
        <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="month" label="月份" width="90" />
        <el-table-column label="金额" width="100" align="right">
          <template #default="s">￥{{ s.row.amount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="110" align="center">
          <template #default="s">{{ channelLabel(s.row.channel) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="s">
            <el-tag :type="s.row.status==='SUCCESS'?'success':(s.row.status==='PENDING'?'warning':'info')" size="small">
              {{ statusLabel(s.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paidTime" label="支付时间" width="170" />
        <el-table-column label="操作" width="130" align="center">
          <template #default="s">
            <el-button v-if="s.row.status==='PENDING'" size="small" plain type="primary" @click="continuePay(s.row)">继续支付</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无支付订单" description="在上方账单中点击「在线支付」后，订单会显示在这里" />
        </template>
      </el-table>
    </div>

    <PayCashier ref="cashier" @paid="onPaid" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import PayCashier from '../components/PayCashier.vue'
import myApi from '../api/my'
import payApi from '../api/pay'

const fees = ref([]), loading = ref(false)
const orders = ref([]), orderLoading = ref(false)
const cashier = ref(null)

const unpaid = computed(() => fees.value.filter(f => f.paid !== 1))
const unpaidCount = computed(() => unpaid.value.length)
const unpaidAmount = computed(() => unpaid.value.reduce((s, f) => s + (Number(f.total) || 0), 0).toFixed(2))
const paidOrders = computed(() => orders.value.filter(o => o.status === 'SUCCESS').length)

function channelLabel(c) { return c === 'mock' ? '模拟通道' : (c === 'wechat' ? '微信支付' : (c === 'alipay' ? '支付宝' : (c || '-'))) }
function statusLabel(s) { return s === 'SUCCESS' ? '支付成功' : (s === 'PENDING' ? '待支付' : '已关闭') }

function loadFees() {
  loading.value = true
  myApi.feePage(1, 50).then(r => { fees.value = r.data.records || [] })
    .catch(() => ElMessage.error('账单加载失败'))
    .finally(() => { loading.value = false })
}

function loadOrders() {
  orderLoading.value = true
  payApi.my().then(r => { orders.value = r.data || [] })
    .catch(() => {})
    .finally(() => { orderLoading.value = false })
}

function startPay(row) {
  payApi.createOrder(row.id, 'mock').then(r => {
    cashier.value.open({
      orderNo: r.data.orderNo, amount: r.data.amount,
      month: r.data.month, subject: '宿舍水电费 ' + (r.data.month || ''), status: r.data.status
    })
  }).catch(() => {})
}

function continuePay(row) {
  payApi.orderDetail(row.orderNo).then(r => {
    if (r.data.status === 'PENDING' && r.data.expireTime && new Date(r.data.expireTime.replace(' ', 'T')) < new Date()) {
      ElMessage.warning('订单已超时，请重新发起支付'); loadOrders(); return
    }
    cashier.value.open(r.data)
  }).catch(() => {})
}

function onPaid() { loadFees(); loadOrders() }

onMounted(() => { loadFees(); loadOrders() })
</script>

<style scoped>
.section-title { font-size: 14px; font-weight: 700; color: #1e293b; margin-bottom: 10px; }
</style>