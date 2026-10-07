<template>
  <div class="dash">
    <!-- 问候区 -->
    <div class="greeting">
      <div>
        <span class="greet-text">{{ greeting }}</span>
        <span class="greet-name">{{ store.realName || store.username }}</span>
      </div>
      <span class="greet-date">{{ today }}</span>
    </div>

    <!-- 统计卡片（管理员8个，员工4个） -->
    <div class="stat-grid" :class="store.role">
      <div v-for="c in displayCards" :key="c.label" class="stat-card">
        <div class="sc-body">
          <span class="sc-num" :style="{ color: c.color }">{{ c.value }}</span>
          <span class="sc-lbl">{{ c.label }}</span>
        </div>
        <div class="sc-icon" :style="{ background: c.bg }">
          <el-icon :style="{ color: c.color }"><component :is="c.icon" /></el-icon>
        </div>
      </div>
    </div>

    <!-- 下方：待办 + 住宿概览（管理员）/ 我的入住（员工） -->
    <div class="bottom-grid">
      <!-- 待办事项 -->
      <el-card class="panel" shadow="never">
        <template #header><span class="panel-hd">待办事项</span></template>
        <div v-if="todoList.length" class="todo-list">
          <div v-for="t in todoList" :key="t.label" class="todo-item">
            <el-icon :color="t.color"><component :is="t.icon" /></el-icon>
            <span class="todo-text">{{ t.label }}</span>
            <el-tag size="small" :type="t.tagType">{{ t.value }} {{ t.unit }}</el-tag>
          </div>
        </div>
        <div v-else class="empty-todo">
          <el-icon color="#10b981"><CircleCheck /></el-icon>
          <span>暂无待办事项</span>
        </div>
      </el-card>

      <!-- 住宿概览 / 我的入住信息 -->
      <el-card class="panel" shadow="never">
        <template #header>
          <span class="panel-hd">{{ store.role === 'admin' ? '住宿概览' : '我的入住信息' }}</span>
        </template>
        <el-descriptions :column="2" border size="small" v-if="store.role === 'admin'">
          <el-descriptions-item label="宿舍楼">{{ d.buildingCount }} 栋</el-descriptions-item>
          <el-descriptions-item label="房间总数">{{ d.roomCount }} 间</el-descriptions-item>
          <el-descriptions-item label="空房间">{{ d.roomEmptyCount }} 间</el-descriptions-item>
          <el-descriptions-item label="在住人数">{{ d.residentCount }} 人</el-descriptions-item>
        </el-descriptions>
        <div v-else class="my-room">
          <p class="my-room-num">
            <el-icon><HomeFilled /></el-icon>
            {{ myRoom.building }} {{ myRoom.room }}
          </p>
          <p class="my-room-info">
            {{ myRoom.type }} · 床位 {{ myRoom.bed }} · 入住于 {{ myRoom.date }}
          </p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import {
  OfficeBuilding, HomeFilled, House, User, UserFilled,
  Tools, Money, Promotion, Warning, CircleCheck
} from '@element-plus/icons-vue'
import dashboardApi from '../api/dashboard'
import myApi from '../api/my'

const store = useUserStore()
const d = ref({
  buildingCount: 0, roomCount: 0, roomEmptyCount: 0, employeeCount: 0,
  residentCount: 0, repairPendingCount: 0, feeUnpaidCount: 0, visitorTodayCount: 0,
  // 员工专属字段
  myRoom: '', myRoomCount: 0, feeTotal: 0, repairTotal: 0, visitorTotal: 0
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const today = new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'long' })

const fieldMap = {
  '宿舍楼': 'buildingCount',
  '房间': 'roomCount',
  '空房间': 'roomEmptyCount',
  '在职员工': 'employeeCount',
  '在住人数': 'residentCount',
  '待处理报修': 'repairPendingCount',
  '未缴费用': 'feeUnpaidCount',
  '今日访客': 'visitorTodayCount',
  '我的房间': 'myRoomCount',
  '待缴费用': 'feeTotal',
  '我的报修': 'repairTotal',
  '本月访客': 'visitorTotal',
}

const adminCards = [
  { label:'宿舍楼', icon:OfficeBuilding, color:'#2563EB', bg:'#eef3fe' },
  { label:'房间', icon:HomeFilled, color:'#0ea5e9', bg:'#e0f2fe' },
  { label:'空房间', icon:House, color:'#10b981', bg:'#dcfce7' },
  { label:'在职员工', icon:User, color:'#8b5cf6', bg:'#f3e8ff' },
  { label:'在住人数', icon:UserFilled, color:'#f59e0b', bg:'#fef3c7' },
  { label:'待处理报修', icon:Tools, color:'#ef4444', bg:'#fee2e2' },
  { label:'未缴费用', icon:Money, color:'#ec4899', bg:'#fce7f3' },
  { label:'今日访客', icon:Promotion, color:'#14b8a6', bg:'#ccfbf1' },
]
const empCards = [
  { label:'我的房间', icon:HomeFilled, color:'#2563EB', bg:'#eef3fe' },
  { label:'待缴费用', icon:Money, color:'#ec4899', bg:'#fce7f3' },
  { label:'我的报修', icon:Tools, color:'#f59e0b', bg:'#fef3c7' },
  { label:'本月访客', icon:Promotion, color:'#14b8a6', bg:'#ccfbf1' },
]
const displayCards = computed(() => {
  const list = store.role === 'admin' ? adminCards : empCards
  return list.map(c => ({ ...c, value: d.value[fieldMap[c.label]] ?? 0 }))
})

const todoList = computed(() => {
  if (store.role !== 'admin') return []
  return [
    { label:'待处理报修', value:d.value.repairPendingCount, unit:'单', color:'#ef4444', tagType:'danger', icon:Tools },
    { label:'未缴水电费', value:d.value.feeUnpaidCount, unit:'笔', color:'#ec4899', tagType:'danger', icon:Money },
    { label:'今日访客', value:d.value.visitorTodayCount, unit:'人', color:'#2563EB', tagType:'', icon:Promotion },
  ].filter(t => t.value > 0)
})

const myRoom = ref({ building: '—', room: '', type: '', bed: '', date: '' })

function load() {
  if (store.role === 'admin') {
    dashboardApi.stats().then(r => { d.value = { ...d.value, ...r.data } })
  } else {
    // 员工：并行加载4个接口
    Promise.all([
      myApi.myAllocation(),
      myApi.feePage(1, 100),
      myApi.repairPage(1, 100),
      myApi.visitorPage(1, 100),
    ]).then(([alloc, fees, repairs, visitors]) => {
      // 入住信息
      if (alloc.data && alloc.data.building) {
        const roomData = alloc.data
        myRoom.value = {
          building: roomData.building ? roomData.building.name : '—',
          room: roomData.room ? roomData.room.roomNo : '—',
          type: roomTypeMap[roomData.room ? roomData.room.type : ''] || '—',
          bed: roomData.allocation ? roomData.allocation.bedNo : '—',
          date: roomData.allocation ? roomData.allocation.checkInDate : '—',
        }
        d.value.myRoom = alloc.data.building.name + (alloc.data.room ? ' ' + alloc.data.room.roomNo : '')
        d.value.myRoomCount = 1
      }
      d.value.feeTotal = fees.data ? fees.data.total : 0
      d.value.repairTotal = repairs.data ? repairs.data.total : 0
      d.value.visitorTotal = visitors.data ? visitors.data.total : 0
    }).catch(() => {})
  }
}

const roomTypeMap = { 1: '单人间', 2: '双人间', 4: '四人间', 6: '六人间' }

onMounted(load)
</script>

<style scoped>
.dash { max-width: 1200px; }
.greeting {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563EB 60%);
  color: #fff; padding: 18px 22px; border-radius: 14px;
  margin-bottom: 16px;
}
.greet-text { font-size: 16px; opacity: .85; margin-right: 6px; }
.greet-name { font-size: 20px; font-weight: 700; }
.greet-date { font-size: 13px; opacity: .75; }

.stat-grid {
  display: grid; gap: 12px;
  grid-template-columns: repeat(4, 1fr);
  margin-bottom: 14px;
}
.stat-grid.admin { grid-template-columns: repeat(4, 1fr); }
.stat-grid.employee { grid-template-columns: repeat(4, 1fr); }
.stat-card {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,.05);
  border: 1px solid #f1f5f9;
  display: flex; align-items: center; justify-content: space-between;
}
.sc-body { display: flex; flex-direction: column; gap: 2px; }
.sc-num { font-size: 24px; font-weight: 800; line-height: 1; }
.sc-lbl { font-size: 12px; color: #64748b; margin-top: 2px; }
.sc-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}

.bottom-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 14px;
}
.panel { border-radius: 12px; border: 1px solid #f1f5f9; }
.panel-hd { font-size: 14px; font-weight: 600; color: #1e293b; }

.todo-list { display: flex; flex-direction: column; gap: 0; }
.todo-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 0; border-bottom: 1px solid #f8fafc;
  font-size: 13px; color: #475569;
}
.todo-item:last-child { border-bottom: none; }
.todo-text { flex: 1; }

.empty-todo {
  display: flex; align-items: center; gap: 8px;
  color: #10b981; font-size: 13px; padding: 8px 0;
}

.my-room { padding: 4px 0; }
.my-room-num { font-size: 18px; font-weight: 600; color: #1e293b; margin: 0 0 6px; display: flex; align-items: center; gap: 6px; }
.my-room-info { font-size: 13px; color: #64748b; margin: 0; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .bottom-grid { grid-template-columns: 1fr; }
}
</style>
