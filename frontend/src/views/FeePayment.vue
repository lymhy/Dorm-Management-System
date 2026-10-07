<template>
  <div class="page-container">
    <ListPageHeader
      title="缴费流水" subtitle="每一笔水电费缴纳记录，支持查看/打印收据与导出"
      :stats="[
        {label:'缴费笔数',value:stats.count},
        {label:'缴费总额',value:'¥'+(stats.amount??0)}
      ]"
    >
      <template #actions>
        <el-button @click="exportExcel"><el-icon><Download /></el-icon> 导出 Excel</el-button>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 补录缴费</el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="房间号" style="width:150px" clearable @clear="doSearch" @keyup.enter="doSearch" />
        <el-select v-model="filterRoom" placeholder="房间" clearable filterable style="width:170px" @change="doSearch">
          <el-option v-for="r in rooms" :key="r.id" :label="roomLabel(r)" :value="r.id" />
        </el-select>
        <el-date-picker v-model="filterMonth" type="month" value-format="YYYY-MM" placeholder="月份" style="width:140px" @change="onMonthChange" />
        <el-select v-model="filterMethod" placeholder="缴费方式" clearable style="width:140px" @change="doSearch">
          <el-option v-for="m in methods" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button type="primary" @click="doSearch"><el-icon><Search /></el-icon> 搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="收据编号" width="120">
          <template #default="s">RCP-{{ String(s.row.id).padStart(6,'0') }}</template>
        </el-table-column>
        <el-table-column label="房间" min-width="130">
          <template #default="s">{{ roomText(s.row) }}</template>
        </el-table-column>
        <el-table-column prop="month" label="月份" width="90" />
        <el-table-column label="缴费金额" width="110" align="right">
          <template #default="s"><strong style="color:#2563EB">¥{{ money(s.row.amount) }}</strong></template>
        </el-table-column>
        <el-table-column prop="payMethod" label="缴费方式" width="110" align="center" />
        <el-table-column label="缴费时间" width="170"><template #default="s">{{ fmt(s.row.payTime) }}</template></el-table-column>
        <el-table-column prop="employeeName" label="缴费人" width="90" />
        <el-table-column prop="operator" label="经办人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="showReceipt(s.row)">收据</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState title="暂无缴费记录" description="在「水电费管理」中标记已缴后会自动生成流水" /></template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条</span>
        <el-pagination background layout="prev,pager,next" :total="total" :current-page="current" :page-size="size" @current-change="load" />
      </div>
    </div>

    <FeeReceipt v-model="receiptVisible" :pay="receiptPay" />

    <el-dialog v-model="dialog" title="补录缴费" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="房间" required>
          <el-select v-model="form.roomId" placeholder="选择房间" filterable style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="roomLabel(r)" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份"><el-date-picker v-model="form.month" type="month" value-format="YYYY-MM" style="width:100%" /></el-form-item>
        <el-form-item label="金额(元)" required><el-input-number v-model="form.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="缴费方式">
          <el-select v-model="form.payMethod" style="width:100%">
            <el-option v-for="m in methods" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费时间">
          <el-date-picker v-model="form.payTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="经办人"><el-input v-model="form.operator" placeholder="可选" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" placeholder="可选" maxlength="100" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import FeeReceipt from '../components/FeeReceipt.vue'
import api from '../api/feePayment'
import roomApi from '../api/room'
import buildingApi from '../api/building'

const methods = ['线下缴纳', '现金', '微信', '支付宝', '银行转账']
const list = ref([]), total = ref(0), current = ref(1), size = ref(10), loading = ref(false)
const keyword = ref(''), filterRoom = ref(null), filterMonth = ref(''), filterMethod = ref('')
const stats = ref({ count: 0, amount: 0 })
const rooms = ref([]), buildings = ref([])
const dialog = ref(false)
const receiptVisible = ref(false), receiptPay = ref({})
const empty = { roomId: null, month: '', amount: 0, payMethod: '线下缴纳', payTime: '', operator: '', remark: '' }
const form = ref({ ...empty })

const buildingMap = computed(() => { const m = {}; buildings.value.forEach(b => { m[b.id] = b.name }); return m })
function roomLabel(r) { return `${buildingMap.value[r.buildingId] || r.buildingId} - ${r.roomNo}` }
function roomText(row) {
  const b = row.buildingName || (row.buildingId ? row.buildingId + '号楼' : '')
  return b ? `${b} - ${row.roomNo || row.roomId}` : String(row.roomNo || row.roomId || '-')
}
function money(v) { return Number(v || 0).toFixed(2) }
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

function params() {
  return { keyword: keyword.value || undefined, roomId: filterRoom.value ?? undefined, month: filterMonth.value || undefined, payMethod: filterMethod.value || undefined }
}
function load(p) {
  if (p) current.value = p
  loading.value = true
  api.page(current.value, size.value, params())
    .then(r => { list.value = r.data.records || []; total.value = r.data.total || 0 })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => { loading.value = false })
}
function loadStats() { api.summary(filterMonth.value || undefined).then(r => { stats.value = r.data || stats.value }).catch(() => {}) }
function doSearch() { current.value = 1; load() }
function onMonthChange() { doSearch(); loadStats() }
function resetFilter() { keyword.value = ''; filterRoom.value = null; filterMonth.value = ''; filterMethod.value = ''; doSearch(); loadStats() }

function openAdd() { form.value = { ...empty, payTime: new Date().toISOString().slice(0, 19).replace('T', ' ') }; dialog.value = true }
function submit() {
  if (!form.value.roomId || !form.value.amount) { ElMessage.warning('请填写房间与金额'); return }
  api.add(form.value).then(() => { ElMessage.success('已保存'); dialog.value = false; load(); loadStats() }).catch(() => {})
}
function remove(r) {
  ElMessageBox.confirm('确定删除该缴费流水吗？', '删除确认', { type: 'warning' })
    .then(() => api.remove(r.id).then(() => { ElMessage.success('已删除'); load(); loadStats() }))
    .catch(() => {})
}
function showReceipt(row) { receiptPay.value = row; receiptVisible.value = true }

function exportExcel() {
  loading.value = true
  api.page(1, 9999, params()).then(r => {
    const rows = r.data.records || []
    if (!rows.length) { ElMessage.warning('没有可导出的数据'); return }
    const aoa = [['收据编号', '房间', '月份', '缴费金额', '缴费方式', '缴费时间', '缴费人', '经办人', '备注']]
    let sum = 0
    rows.forEach(x => {
      sum += Number(x.amount) || 0
      aoa.push(['RCP-' + String(x.id).padStart(6, '0'), roomText(x), x.month, money(x.amount), x.payMethod, fmt(x.payTime), x.employeeName || '', x.operator || '', x.remark || ''])
    })
    aoa.push([])
    aoa.push(['合计', '', '', sum.toFixed(2), '', '', '', '', ''])
    const ws = XLSX.utils.aoa_to_sheet(aoa)
    ws['!cols'] = [{ wch: 14 }, { wch: 16 }, { wch: 10 }, { wch: 12 }, { wch: 12 }, { wch: 20 }, { wch: 10 }, { wch: 10 }, { wch: 16 }]
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '缴费流水')
    XLSX.writeFile(wb, `缴费流水_${filterMonth.value || new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success(`已导出 ${rows.length} 条`)
  }).catch(() => ElMessage.error('导出失败')).finally(() => { loading.value = false })
}

onMounted(() => {
  roomApi.page(1, 500).then(r => { rooms.value = r.data.records || [] }).catch(() => {})
  buildingApi.page(1, 200).then(r => { buildings.value = r.data.records || [] }).catch(() => {})
  load(); loadStats()
})
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.filter-bar { display:flex; align-items:center; gap:10px; flex-wrap:wrap; padding:12px 16px; border-bottom:1px solid #f1f5f9; }
.pagination-row { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-top:1px solid #f1f5f9; }
.total-hint { font-size:13px; color:#64748b; }
</style>
