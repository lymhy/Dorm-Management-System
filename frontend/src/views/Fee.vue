<template>
  <div class="page-container">
    <ListPageHeader
      title="水电费管理" subtitle="水电费账单：筛选、批量操作、自动算费、导出 Excel"
      :stats="[
        {label:'账单总数',value:stats.count},
        {label:'已缴',value:stats.paidCount},
        {label:'未缴',value:stats.unpaidCount},
        {label:'未缴金额',value:'¥'+(stats.unpaidAmount??0)}
      ]"
    >
      <template #actions>
        <el-button @click="exportExcel"><el-icon><Download /></el-icon> 导出 Excel</el-button>
        <el-button v-if="isAdmin" type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增账单</el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <!-- 多条件筛选（管理员） -->
      <div v-if="isAdmin" class="filter-bar">
        <el-input v-model="keyword" placeholder="房间号" style="width:150px" clearable @clear="doSearch" @keyup.enter="doSearch" />
        <el-select v-model="filterRoom" placeholder="房间" clearable filterable style="width:170px" @change="doSearch">
          <el-option v-for="r in rooms" :key="r.id" :label="roomLabel(r)" :value="r.id" />
        </el-select>
        <el-date-picker v-model="filterMonth" type="month" value-format="YYYY-MM" placeholder="月份" style="width:140px" @change="onMonthChange" />
        <el-select v-model="filterPaid" placeholder="缴纳状态" clearable style="width:130px" @change="doSearch">
          <el-option :value="1" label="已缴" />
          <el-option :value="0" label="未缴" />
        </el-select>
        <el-button type="primary" @click="doSearch"><el-icon><Search /></el-icon> 搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <template v-if="isAdmin">
          <el-divider direction="vertical" />
          <el-button :disabled="!selection.length" @click="batchPaid(1)">批量标记已缴</el-button>
          <el-button :disabled="!selection.length" @click="batchPaid(0)">批量标记未缴</el-button>
          <el-button type="danger" plain :disabled="!selection.length" @click="batchRemove">批量删除</el-button>
          <el-button type="warning" plain @click="urge"><el-icon><Bell /></el-icon> 一键催缴</el-button>
        </template>
      </div>

      <el-table :data="list" border stripe v-loading="loading" @selection-change="v=>selection=v">
        <el-table-column v-if="isAdmin" type="selection" width="46" />
        <el-table-column label="房间" min-width="130">
          <template #default="s">{{ roomMap[s.row.roomId] || s.row.roomId }}</template>
        </el-table-column>
        <el-table-column prop="month" label="月份" width="90" />
        <el-table-column label="水用量" width="90" align="right"><template #default="s">{{ s.row.waterUsage ?? '-' }}</template></el-table-column>
        <el-table-column label="电用量" width="90" align="right"><template #default="s">{{ s.row.elecUsage ?? '-' }}</template></el-table-column>
        <el-table-column label="水费" width="90" align="right"><template #default="s">¥{{ s.row.waterFee ?? 0 }}</template></el-table-column>
        <el-table-column label="电费" width="90" align="right"><template #default="s">¥{{ s.row.elecFee ?? 0 }}</template></el-table-column>
        <el-table-column label="合计" width="100" align="right">
          <template #default="s"><strong style="color:#2563EB">¥{{ s.row.total ?? 0 }}</strong></template>
        </el-table-column>
        <el-table-column label="缴纳状态" width="90" align="center">
          <template #default="s">
            <el-tag :type="s.row.paid===1?'success':'danger'" size="small">{{ s.row.paid===1?'已缴':'未缴' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="应缴日期" width="120" />
        <el-table-column label="操作" width="290" fixed="right" align="center">
          <template #default="s">
            <template v-if="isAdmin">
              <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
              <el-button size="small" :type="s.row.paid===1?'warning':'success'" plain @click="togglePaid(s.row)">
                {{ s.row.paid===1 ? '标记未缴' : '标记已缴' }}
              </el-button>
              <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
            </template>
            <el-button v-if="!isAdmin && s.row.paid!==1" size="small" type="primary" @click="startPay(s.row)">在线支付</el-button>
            <el-button v-if="s.row.paid===1" size="small" plain @click="showReceipt(s.row)">收据</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无账单记录" description="点击上方「新增账单」添加第一条记录" />
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条</span>
        <el-pagination background layout="prev,pager,next" :total="total" :current-page="current" :page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="dialogTitle" width="620px" destroy-on-close>
      <el-alert
        v-if="prices.water || prices.elec"
        type="info" :closable="false" show-icon style="margin-bottom:12px"
        :title="`当前单价：水 ¥${prices.water}/m³、电 ¥${prices.elec}/kWh（来自系统参数）`"
      />
      <el-form :model="form" label-width="100px" class="form-grid">
        <el-form-item label="房间" required>
          <el-select v-model="form.roomId" placeholder="选择房间" style="width:100%" filterable>
            <el-option v-for="r in rooms" :key="r.id" :label="roomLabel(r)" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份" required>
          <el-date-picker v-model="form.month" type="month" value-format="YYYY-MM" style="width:100%" />
        </el-form-item>
        <el-form-item label="水用量(m³)"><el-input-number v-model="form.waterUsage" :min="0" :precision="2" style="width:100%" @change="recalc" /></el-form-item>
        <el-form-item label="电用量(kWh)"><el-input-number v-model="form.elecUsage" :min="0" :precision="2" style="width:100%" @change="recalc" /></el-form-item>
        <el-form-item label="水费(元)"><el-input-number v-model="form.waterFee" :min="0" :precision="2" style="width:100%" @change="syncTotal" /></el-form-item>
        <el-form-item label="电费(元)"><el-input-number v-model="form.elecFee" :min="0" :precision="2" style="width:100%" @change="syncTotal" /></el-form-item>
        <el-form-item label="合计(元)">
          <el-input-number v-model="form.total" :min="0" :precision="2" style="width:100%" />
          <el-button link type="primary" style="margin-left:8px" @click="recalc">按用量重算</el-button>
        </el-form-item>
        <el-form-item label="应缴日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="是否已缴">
          <el-switch v-model="form.paid" :active-value="1" :inactive-value="0" active-text="已缴" inactive-text="未缴" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>

    <FeeReceipt v-model="receiptVisible" :pay="receiptPay" />
    <PayCashier ref="cashier" @paid="load" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Download, Bell } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import FeeReceipt from '../components/FeeReceipt.vue'
import api from '../api/fee'
import feePaymentApi from '../api/feePayment'
import payApi from '../api/pay'
import PayCashier from '../components/PayCashier.vue'
import roomApi from '../api/room'
import buildingApi from '../api/building'
import myApi from '../api/my'
import cfgApi from '../api/systemConfig'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const isAdmin = computed(() => store.role === 'admin')

const list = ref([]), total = ref(0), current = ref(1), size = ref(10), loading = ref(false)
const keyword = ref(''), filterRoom = ref(null), filterMonth = ref(''), filterPaid = ref(null)
const selection = ref([])
const dialog = ref(false), dialogTitle = ref('')
const rooms = ref([]), buildings = ref([])
const stats = ref({ count: 0, paidCount: 0, unpaidCount: 0, unpaidAmount: 0 })
const prices = ref({ water: null, elec: null })
const receiptVisible = ref(false), receiptPay = ref({})
const cashier = ref(null)

function startPay(row) {
  payApi.createOrder(row.id, 'mock').then(r => {
    cashier.value.open({
      orderNo: r.data.orderNo, amount: r.data.amount,
      month: r.data.month, subject: '宿舍水电费 ' + (r.data.month || ''), status: r.data.status
    })
  }).catch(() => {})
}

const empty = { roomId: null, month: '', waterUsage: 0, elecUsage: 0, waterFee: 0, elecFee: 0, total: 0, paid: 0, dueDate: '', remark: '' }
const form = ref({ ...empty })

const buildingMap = computed(() => { const m = {}; buildings.value.forEach(b => { m[b.id] = b.name }); return m })
function roomLabel(r) { return `${buildingMap.value[r.buildingId] || r.buildingId} - ${r.roomNo}` }
const roomMap = computed(() => { const m = {}; rooms.value.forEach(r => { m[r.id] = roomLabel(r) }); return m })

function currentParams() {
  return {
    keyword: keyword.value || undefined,
    roomId: filterRoom.value ?? undefined,
    month: filterMonth.value || undefined,
    paid: filterPaid.value ?? undefined
  }
}

function load(p) {
  if (p) current.value = p
  loading.value = true
  const call = isAdmin.value
    ? api.page(current.value, size.value, currentParams())
    : myApi.feePage(current.value, size.value)
  call.then(r => { list.value = r.data.records || []; total.value = r.data.total || 0 })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => { loading.value = false })
}

function loadStats() {
  if (!isAdmin.value) return
  api.summary(filterMonth.value || undefined)
    .then(r => { stats.value = r.data || stats.value })
    .catch(() => {})
}

function doSearch() { current.value = 1; load() }
function onMonthChange() { doSearch(); loadStats() }
function resetFilter() { keyword.value = ''; filterRoom.value = null; filterMonth.value = ''; filterPaid.value = null; doSearch(); loadStats() }

function openAdd() {
  form.value = { ...empty, month: filterMonth.value || new Date().toISOString().slice(0, 7) }
  dialogTitle.value = '新增账单'; dialog.value = true
}
function openEdit(r) { form.value = { ...empty, ...r }; dialogTitle.value = '编辑账单'; dialog.value = true }

function recalc() {
  const w = Number(form.value.waterUsage) || 0
  const e = Number(form.value.elecUsage) || 0
  if (prices.value.water != null) form.value.waterFee = +(w * prices.value.water).toFixed(2)
  if (prices.value.elec != null) form.value.elecFee = +(e * prices.value.elec).toFixed(2)
  syncTotal()
}
function syncTotal() {
  const w = Number(form.value.waterFee) || 0
  const e = Number(form.value.elecFee) || 0
  form.value.total = +(w + e).toFixed(2)
}

function submit() {
  if (!form.value.roomId || !form.value.month) { ElMessage.warning('请填写必填项'); return }
  syncTotal()
  const fn = form.value.id ? api.update(form.value) : api.add(form.value)
  fn.then(() => { ElMessage.success('保存成功'); dialog.value = false; load(); loadStats() }).catch(() => {})
}

function togglePaid(row) {
  const paid = row.paid === 1 ? 0 : 1
  api.markPaid(row.id, paid).then(() => { ElMessage.success('已更新'); load(); loadStats() }).catch(() => {})
}

function batchPaid(paid) {
  const ids = selection.value.map(r => r.id)
  if (!ids.length) return
  ElMessageBox.confirm(`确定将选中的 ${ids.length} 条账单标记为「${paid === 1 ? '已缴' : '未缴'}」吗？`, '批量操作', { type: 'warning' })
    .then(() => api.batchPaid(ids, paid).then(() => { ElMessage.success('操作成功'); load(); loadStats() }))
    .catch(() => {})
}

function batchRemove() {
  const ids = selection.value.map(r => r.id)
  if (!ids.length) return
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条账单吗？`, '批量删除', { type: 'warning' })
    .then(() => api.batchRemove(ids).then(() => { ElMessage.success('已删除'); load(); loadStats() }))
    .catch(() => {})
}

function remove(r) {
  ElMessageBox.confirm('确定删除该账单吗？', '删除确认', { type: 'warning' })
    .then(() => api.remove(r.id).then(() => { ElMessage.success('已删除'); load(); loadStats() }))
    .catch(() => {})
}

/* ---------- 一键催缴：给未缴账单所在房间的在住员工发通知 ---------- */
function urge() {
  const unpaidSel = selection.value.filter(r => r.paid !== 1).map(r => r.id)
  const scope = unpaidSel.length
    ? `选中的 ${unpaidSel.length} 条未缴账单`
    : (filterMonth.value ? `${filterMonth.value} 月全部未缴账单` : '全部未缴账单')
  ElMessageBox.confirm(`确定对「${scope}」所在房间的在住员工发送催缴通知吗？`, '一键催缴', { type: 'warning' })
    .then(() => api.urge(unpaidSel, filterMonth.value || undefined).then(r => {
      ElMessage.success(`已发送 ${r.data.notified} 条催缴通知（涉及 ${r.data.bills} 条未缴账单）`)
    }))
    .catch(() => {})
}

/* ---------- 收据（读取该账单的缴费流水；无流水则按账单信息生成） ---------- */
function showReceipt(row) {
  const rm = rooms.value.find(x => x.id === row.roomId) || {}
  const fallback = {
    id: row.id, roomId: row.roomId, roomNo: rm.roomNo, buildingName: buildingMap.value[rm.buildingId],
    month: row.month, amount: row.total, payMethod: '未登记', payTime: row.dueDate, operator: '-', remark: '按账单信息生成'
  }
  feePaymentApi.byFee(row.id).then(r => {
    receiptPay.value = r.data || fallback
    receiptVisible.value = true
  }).catch(() => { receiptPay.value = fallback; receiptVisible.value = true })
}

/* ---------- 导出 Excel（按当前筛选条件导出全部） ---------- */
function exportExcel() {
  loading.value = true
  const p = isAdmin.value ? api.page(1, 9999, currentParams()) : myApi.feePage(1, 9999)
  p.then(r => {
    const rows = r.data.records || []
    if (!rows.length) { ElMessage.warning('没有可导出的数据'); return }
    const aoa = [['房间', '月份', '水用量(m³)', '电用量(kWh)', '水费(元)', '电费(元)', '合计(元)', '缴纳状态', '应缴日期', '备注']]
    let sumTotal = 0, sumUnpaid = 0
    rows.forEach(x => {
      const t = Number(x.total) || 0
      sumTotal += t
      if (x.paid !== 1) sumUnpaid += t
      aoa.push([
        roomMap.value[x.roomId] || x.roomId, x.month,
        x.waterUsage ?? '', x.elecUsage ?? '',
        x.waterFee ?? 0, x.elecFee ?? 0, t,
        x.paid === 1 ? '已缴' : '未缴', x.dueDate ?? '', x.remark ?? ''
      ])
    })
    aoa.push([])
    aoa.push(['合计', '', '', '', '', '', sumTotal.toFixed(2), `未缴合计 ${sumUnpaid.toFixed(2)}`, '', ''])
    const ws = XLSX.utils.aoa_to_sheet(aoa)
    ws['!cols'] = [{ wch: 16 }, { wch: 10 }, { wch: 12 }, { wch: 12 }, { wch: 10 }, { wch: 10 }, { wch: 10 }, { wch: 10 }, { wch: 12 }, { wch: 16 }]
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '水电费账单')
    const tag = filterMonth.value ? filterMonth.value : new Date().toISOString().slice(0, 10)
    XLSX.writeFile(wb, `水电费账单_${tag}.xlsx`)
    ElMessage.success(`已导出 ${rows.length} 条`)
  }).catch(() => ElMessage.error('导出失败')).finally(() => { loading.value = false })
}

onMounted(() => {
  roomApi.page(1, 500).then(r => { rooms.value = r.data.records || [] }).catch(() => {})
  buildingApi.page(1, 200).then(r => { buildings.value = r.data.records || [] }).catch(() => {})
  cfgApi.list().then(r => {
    const m = {}
    ;(r.data || []).forEach(c => { m[c.cfgKey || c.key] = Number(c.cfgValue ?? c.value) })
    prices.value = { water: m.water_price ?? null, elec: m.elec_price ?? null }
  }).catch(() => {})
  load(); loadStats()
})
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.filter-bar { display:flex; align-items:center; gap:10px; flex-wrap:wrap; padding:12px 16px; border-bottom:1px solid #f1f5f9; }
.pagination-row { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-top:1px solid #f1f5f9; }
.total-hint { font-size:13px; color:#64748b; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:0 8px; }
.form-grid :deep(.el-form-item:last-child) { grid-column:1/-1; }
</style>
