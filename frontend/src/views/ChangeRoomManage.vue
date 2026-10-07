<template>
  <div class="page-container">
    <ListPageHeader
      title="调宿申请管理"
      subtitle="审批员工调宿申请"
      :stats="[{ label: '待审批', value: pendingCount }]"
    />

    <!-- 筛选栏 -->
    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="onFilter">
        <el-radio-button :value="''">全部</el-radio-button>
        <el-radio-button :value="0">待审批</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <div class="card">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="employeeName" label="员工姓名" width="120" />
        <el-table-column label="当前房间" min-width="130">
          <template #default="s">{{ roomText(s.row.currentRoom) }}</template>
        </el-table-column>
        <el-table-column label="目标房型" width="110" align="center">
          <template #default="s">
            <el-tag size="small">{{ roomTypeMap[s.row.targetRoomType] || s.row.targetRoomType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="s">
            <el-tag :type="typeMap[s.row.status] || 'info'" size="small">
              {{ statusMap[s.row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="s">
            <template v-if="s.row.status === 0">
              <el-button size="small" type="success" plain @click="approve(s.row)">通过</el-button>
              <el-button size="small" type="danger" plain @click="openReject(s.row)">拒绝</el-button>
            </template>
            <span v-else style="color:#94a3b8;font-size:13px">已处理</span>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无调宿申请" description="当前筛选条件下没有记录" />
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条</span>
        <el-pagination
          background
          layout="prev,pager,next"
          :total="total"
          :current-page="current"
          :page-size="size"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectDialog" title="拒绝申请" width="440px" destroy-on-close>
      <el-input v-model="rejectReply" type="textarea" :rows="4" placeholder="请填写拒绝原因（将反馈给申请人）" />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api/changeRoom'

const roomTypeMap = { 1: '单人间', 2: '双人间', 4: '四人间', 6: '六人间' }
const statusMap = { 0: '待审批', 1: '已通过', 2: '已拒绝' }
const typeMap = { 0: 'warning', 1: 'success', 2: 'danger' }

const list = ref([])
const total = ref(0)
const pendingCount = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const statusFilter = ref('')

const rejectDialog = ref(false)
const rejectLoading = ref(false)
const rejectReply = ref('')
const rejectRow = ref(null)

function roomText(room) {
  if (!room) return '-'
  if (typeof room === 'string') return room
  return `${room.buildingName || room.buildingId || ''}栋-${room.roomNo || ''}`
}

function load(p) {
  if (p) current.value = p
  loading.value = true
  const status = statusFilter.value === '' ? undefined : statusFilter.value
  api.page(current.value, size.value, status)
    .then(r => {
      list.value = (r.data && r.data.records) || []
      total.value = (r.data && r.data.total) || 0
    })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => (loading.value = false))
  // 待审批统计
  api.page(1, 1, 0)
    .then(r => { pendingCount.value = (r.data && r.data.total) || 0 })
    .catch(() => {})
}

function onFilter() {
  current.value = 1
  load()
}

function approve(row) {
  api.approve(row.id)
    .then(() => { ElMessage.success('已通过'); load() })
    .catch(() => ElMessage.error('操作失败'))
}

function openReject(row) {
  rejectRow.value = row
  rejectReply.value = ''
  rejectDialog.value = true
}

function submitReject() {
  if (!rejectReply.value.trim()) { ElMessage.warning('请填写拒绝原因'); return }
  rejectLoading.value = true
  api.reject(rejectRow.value.id, rejectReply.value)
    .then(() => { ElMessage.success('已拒绝'); rejectDialog.value = false; load() })
    .catch(() => ElMessage.error('操作失败'))
    .finally(() => (rejectLoading.value = false))
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.toolbar { margin-bottom: 12px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.pagination-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.total-hint { font-size: 13px; color: #64748b; }
</style>
