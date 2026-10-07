<template>
  <div class="page-container">
    <ListPageHeader
      title="费用统计报表" subtitle="按楼栋 / 楼层汇总，含月度趋势图"
      :stats="[
        {label:'账单数',value:sum.count},
        {label:'总金额',value:'¥'+money(sum.totalAmount)},
        {label:'已缴',value:'¥'+money(sum.paidAmount)},
        {label:'未缴',value:'¥'+money(sum.unpaidAmount)}
      ]"
    >
      <template #actions>
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="全部月份" clearable style="width:150px" @change="load" />
        <el-button type="primary" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </template>
    </ListPageHeader>

    <div class="chart-grid">
      <div class="card chart-card">
        <div class="card-title">月度费用趋势</div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="card chart-card">
        <div class="card-title">各楼栋费用（已缴 / 未缴）</div>
        <div ref="buildingRef" class="chart"></div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">按楼栋汇总</div>
      <el-table :data="byBuilding" border stripe v-loading="loading" show-summary :summary-method="buildingSummary">
        <el-table-column prop="buildingName" label="楼栋" min-width="130" />
        <el-table-column prop="count" label="账单数" width="100" align="center" />
        <el-table-column label="总金额" width="130" align="right"><template #default="s">¥{{ money(s.row.totalAmount) }}</template></el-table-column>
        <el-table-column label="已缴金额" width="130" align="right"><template #default="s"><span style="color:#16a34a">¥{{ money(s.row.paidAmount) }}</span></template></el-table-column>
        <el-table-column label="未缴金额" width="130" align="right"><template #default="s"><span style="color:#dc2626">¥{{ money(s.row.unpaidAmount) }}</span></template></el-table-column>
        <el-table-column prop="unpaidCount" label="未缴笔数" width="100" align="center" />
      </el-table>
    </div>

    <div class="card" style="margin-top:16px">
      <div class="card-title">按楼层汇总</div>
      <el-table :data="byFloor" border stripe height="360">
        <el-table-column prop="buildingName" label="楼栋" min-width="130" />
        <el-table-column label="楼层" width="90" align="center"><template #default="s">{{ s.row.floor ?? '未知' }}</template></el-table-column>
        <el-table-column prop="count" label="账单数" width="100" align="center" />
        <el-table-column label="总金额" width="130" align="right"><template #default="s">¥{{ money(s.row.totalAmount) }}</template></el-table-column>
        <el-table-column label="已缴金额" width="130" align="right"><template #default="s"><span style="color:#16a34a">¥{{ money(s.row.paidAmount) }}</span></template></el-table-column>
        <el-table-column label="未缴金额" width="130" align="right"><template #default="s"><span style="color:#dc2626">¥{{ money(s.row.unpaidAmount) }}</span></template></el-table-column>
        <el-table-column prop="unpaidCount" label="未缴笔数" width="100" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ListPageHeader from '../components/ListPageHeader.vue'
import api from '../api/fee'

const month = ref('')
const loading = ref(false)
const byBuilding = ref([])
const byFloor = ref([])
const trend = ref([])
const sum = ref({ count: 0, totalAmount: 0, paidAmount: 0, unpaidAmount: 0 })

const trendRef = ref(null), buildingRef = ref(null)
let trendChart = null, buildingChart = null

function money(v) { return Number(v || 0).toFixed(2) }

function buildingSummary({ columns, data }) {
  const out = []
  data.forEach((row, i) => {
    if (i === 0) {
      out[i] = '合计'
    } else if (['count', 'unpaidCount'].includes(columns[i].property)) {
      out[i] = data.reduce((a, b) => a + Number(b[columns[i].property] || 0), 0)
    } else if (['totalAmount', 'paidAmount', 'unpaidAmount'].includes(columns[i].property)) {
      out[i] = '¥' + data.reduce((a, b) => a + Number(b[columns[i].property] || 0), 0).toFixed(2)
    } else {
      out[i] = ''
    }
  })
  return out
}

function renderCharts() {
  // 趋势折线
  if (trendRef.value) {
    if (!trendChart) trendChart = echarts.init(trendRef.value)
    const months = trend.value.map(t => t.month)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['总金额', '已缴', '未缴'], top: 0 },
      grid: { left: 46, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: months, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [
        { name: '总金额', type: 'line', smooth: true, data: trend.value.map(t => Number(t.totalAmount || 0)), itemStyle: { color: '#2563EB' }, areaStyle: { opacity: 0.12 } },
        { name: '已缴', type: 'line', smooth: true, data: trend.value.map(t => Number(t.paidAmount || 0)), itemStyle: { color: '#16a34a' } },
        { name: '未缴', type: 'line', smooth: true, data: trend.value.map(t => Number(t.unpaidAmount || 0)), itemStyle: { color: '#dc2626' } }
      ]
    })
  }
  // 楼栋柱状
  if (buildingRef.value) {
    if (!buildingChart) buildingChart = echarts.init(buildingRef.value)
    buildingChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['已缴', '未缴'], top: 0 },
      grid: { left: 46, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: byBuilding.value.map(b => b.buildingName) },
      yAxis: { type: 'value' },
      series: [
        { name: '已缴', type: 'bar', stack: 'amt', data: byBuilding.value.map(b => Number(b.paidAmount || 0)), itemStyle: { color: '#16a34a' }, barWidth: '46%' },
        { name: '未缴', type: 'bar', stack: 'amt', data: byBuilding.value.map(b => Number(b.unpaidAmount || 0)), itemStyle: { color: '#dc2626' } }
      ]
    })
  }
}

function resize() { trendChart && trendChart.resize(); buildingChart && buildingChart.resize() }

function load() {
  loading.value = true
  api.report(month.value || undefined).then(r => {
    const d = r.data || {}
    byBuilding.value = d.byBuilding || []
    byFloor.value = d.byFloor || []
    trend.value = d.trend || []
    sum.value = byBuilding.value.reduce((a, b) => ({
      count: a.count + Number(b.count || 0),
      totalAmount: a.totalAmount + Number(b.totalAmount || 0),
      paidAmount: a.paidAmount + Number(b.paidAmount || 0),
      unpaidAmount: a.unpaidAmount + Number(b.unpaidAmount || 0)
    }), { count: 0, totalAmount: 0, paidAmount: 0, unpaidAmount: 0 })
    nextTick(renderCharts)
  }).catch(() => ElMessage.error('加载失败')).finally(() => { loading.value = false })
}

onMounted(() => { load(); window.addEventListener('resize', resize) })
onUnmounted(() => {
  window.removeEventListener('resize', resize)
  trendChart && trendChart.dispose()
  buildingChart && buildingChart.dispose()
})
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.card-title { font-size: 14px; font-weight: 600; color: #1e293b; padding: 12px 16px; border-bottom: 1px solid #f1f5f9; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart { height: 300px; padding: 8px; }
@media (max-width: 1100px) { .chart-grid { grid-template-columns: 1fr; } }
</style>
