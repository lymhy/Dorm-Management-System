<template>
  <div class="page-container">
    <ListPageHeader title="消息通知" subtitle="报修接单、维修完成、水电超限、换寝审批等消息提醒">
      <template #actions>
        <el-button :disabled="unread <= 0" @click="doReadAll">
          <el-icon><Select /></el-icon> 全部已读
        </el-button>
      </template>
    </ListPageHeader>

    <!-- 筛选栏 -->
    <div class="toolbar">
      <el-radio-group v-model="filter" @change="onFilter">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="0">未读<span v-if="unread" class="tab-badge">{{ unread }}</span></el-radio-button>
        <el-radio-button label="1">已读</el-radio-button>
      </el-radio-group>
      <span class="hint">共 {{ total }} 条消息</span>
    </div>

    <div class="card" v-loading="loading">
      <div v-if="list.length === 0" class="empty-wrap">
        <EmptyState title="暂无消息" description="有报修、水电、换寝等动态时会在这里通知你" />
      </div>

      <ul v-else class="notif-list">
        <li
          v-for="n in list"
          :key="n.id"
          class="notif-item"
          :class="{ unread: !n.isRead }"
          @click="openItem(n)"
        >
          <div class="n-icon" :class="meta(n.type).cls">
            <el-icon><component :is="meta(n.type).icon" /></el-icon>
          </div>
          <div class="n-body">
            <div class="n-top">
              <span class="n-title">{{ n.title }}</span>
              <el-tag size="small" :type="meta(n.type).tag" effect="plain">{{ meta(n.type).label }}</el-tag>
              <span v-if="!n.isRead" class="dot" />
            </div>
            <div class="n-content">{{ n.content }}</div>
            <div class="n-time">{{ n.createTime }}</div>
          </div>
          <div class="n-actions" @click.stop>
            <el-button
              v-if="!n.isRead"
              size="small"
              text
              type="primary"
              @click="toggleRead(n, 1)"
            >标记已读</el-button>
            <el-button v-else size="small" text type="info" @click="toggleRead(n, 0)">标记未读</el-button>
            <el-button size="small" text type="danger" @click="remove(n)">删除</el-button>
          </div>
        </li>
      </ul>

      <div class="pagination-row" v-if="total > size">
        <span class="total-hint">第 {{ current }} 页</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import {
  Select, Tools, CircleCheck, Warning, Switch, BellFilled, Money
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api/notification'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const list = ref([])
const total = ref(0)
const unread = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const filter = ref('')

const typeMeta = {
  repair_accept: { label: '报修接单', tag: 'primary', cls: 'c-blue', icon: Tools },
  repair_finish: { label: '维修完成', tag: 'success', cls: 'c-green', icon: CircleCheck },
  fee_warning: { label: '水电超限', tag: 'warning', cls: 'c-amber', icon: Warning },
  fee_urge: { label: '缴费催缴', tag: 'danger', cls: 'c-amber', icon: Money },
  fee_paid: { label: '缴费成功', tag: 'success', cls: 'c-green', icon: CircleCheck },
  change_room_result: { label: '换寝审批', tag: 'info', cls: 'c-purple', icon: Switch },
  default: { label: '通知', tag: 'info', cls: 'c-gray', icon: BellFilled }
}
function meta(type) {
  return typeMeta[type] || typeMeta.default
}

function load(p) {
  if (p) current.value = p
  loading.value = true
  const isRead = filter.value === '' ? undefined : filter.value
  api
    .page(current.value, size.value, isRead)
    .then(r => {
      list.value = (r.data && r.data.records) || []
      total.value = (r.data && r.data.total) || 0
    })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => (loading.value = false))
}

function loadUnread() {
  api.unreadCount().then(r => (unread.value = r.data || 0)).catch(() => {})
}

function onFilter() {
  current.value = 1
  load()
}

function openItem(n) {
  // 点击未读项自动标记已读（可选）
  if (!n.isRead) toggleRead(n, 1)
}

function toggleRead(n, val) {
  const fn = val === 1 ? api.markRead(n.id) : api.markUnread(n.id)
  fn.then(() => {
    n.isRead = val
    loadUnread()
  }).catch(() => ElMessage.error('操作失败'))
}

function doReadAll() {
  api.readAll().then(() => {
    ElMessage.success('全部标记为已读')
    load()
    loadUnread()
  }).catch(() => ElMessage.error('操作失败'))
}

function remove(n) {
  ElMessageBox.confirm(`确定删除消息「${n.title}」吗？`, '删除确认', { type: 'warning' })
    .then(() => api.remove(n.id).then(() => {
      ElMessage.success('已删除')
      load()
      loadUnread()
    }))
    .catch(() => {})
}

onMounted(() => {
  load()
  loadUnread()
})
onActivated(() => {
  loadUnread()
})
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.hint { font-size: 13px; color: #64748b; }
.tab-badge {
  display: inline-block; margin-left: 4px; padding: 0 6px;
  background: #ef4444; color: #fff; border-radius: 10px;
  font-size: 11px; line-height: 16px;
}
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.empty-wrap { padding: 40px 0; }
.notif-list { list-style: none; margin: 0; padding: 0; }
.notif-item {
  display: flex; gap: 14px; padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9; cursor: pointer;
  transition: background .2s ease;
}
.notif-item:last-child { border-bottom: none; }
.notif-item:hover { background: #f8fafc; }
.notif-item.unread { background: #f5f9ff; }
.notif-item.unread:hover { background: #eef5ff; }
.n-icon {
  width: 40px; height: 40px; border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 19px;
}
.c-blue { background: #dbeafe; color: #2563eb; }
.c-green { background: #dcfce7; color: #16a34a; }
.c-amber { background: #fef3c7; color: #d97706; }
.c-purple { background: #ede9fe; color: #7c3aed; }
.c-gray { background: #f1f5f9; color: #64748b; }
.n-body { flex: 1; min-width: 0; }
.n-top { display: flex; align-items: center; gap: 8px; }
.n-title { font-size: 14px; font-weight: 600; color: #1e293b; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #ef4444; flex-shrink: 0; }
.n-content { margin-top: 6px; font-size: 13px; color: #475569; line-height: 1.6; }
.n-time { margin-top: 8px; font-size: 12px; color: #94a3b8; }
.n-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.pagination-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.total-hint { font-size: 13px; color: #64748b; }
</style>
