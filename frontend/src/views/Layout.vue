<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '210px'" class="aside">
      <!-- Logo -->
      <div class="brand">
        <div class="brand-logo">宿</div>
        <transition name="fade-text">
          <span v-if="!collapsed" class="brand-name">宿舍管理</span>
        </transition>
      </div>

      <!-- 菜单 -->
      <el-menu
        :default-active="active"
        router
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#fff"
        class="side-menu"
      >
        <template v-for="group in menuGroups" :key="group.label">
          <div v-if="!collapsed" class="menu-group-label">{{ group.label }}</div>
          <el-menu-item
            v-for="m in group.items"
            :key="m.path"
            :index="m.path"
          >
            <el-icon><component :is="m.icon" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="h-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <component :is="collapsed ? Expand : Fold" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="h-right">
          <el-badge v-if="store.role === 'employee'" :value="unread" :hidden="unread === 0" :max="99" class="bell-badge">
            <el-icon class="bell-btn" title="消息通知" @click="goNotification"><Bell /></el-icon>
          </el-badge>
          <span class="role-badge" :class="store.role">
            {{ store.role === 'admin' ? '管理员' : '员工' }}
          </span>
          <el-dropdown @command="onCommand" trigger="click">
            <span class="user-chip">
              <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
              <span class="uname">{{ store.realName || store.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="pwdDialog" title="修改密码" width="400px" destroy-on-close>
    <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="90px">
      <el-form-item label="旧密码" prop="oldPwd">
        <el-input v-model="pwdForm.oldPwd" type="password" show-password placeholder="请输入旧密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPwd">
        <el-input v-model="pwdForm.newPwd" type="password" show-password placeholder="请输入新密码" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPwd">
        <el-input v-model="pwdForm.confirmPwd" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialog=false">取消</el-button>
      <el-button type="primary" @click="changePwd">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api/notification'
import {
  Fold, Expand, ArrowDown, SwitchButton,
  DataLine, OfficeBuilding, HomeFilled, User, UserFilled,
  Connection, Money, Tools, Promotion, House, Setting,
  Switch, Remove, Bell, Key, Tickets, TrendCharts, CreditCard
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const collapsed = ref(false)
const pwdDialog = ref(false)
const pwdRef = ref(null)
const pwdForm = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' })
const pwdRules = {
  oldPwd: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, val, cb) => {
        if (val !== pwdForm.newPwd) cb(new Error('两次密码不一致'))
        else cb()
      }, trigger: 'blur'
    }
  ]
}

// 菜单分组
const adminGroups = [
  { label: '数据管理', items: [
    { path: '/dashboard', title: '仪表盘', icon: DataLine },
    { path: '/building', title: '宿舍楼', icon: OfficeBuilding },
    { path: '/room', title: '房间', icon: HomeFilled },
    { path: '/employee', title: '员工', icon: User },
    { path: '/allocation', title: '入住', icon: Connection },
  ]},
  { label: '业务', items: [
    { path: '/fee', title: '水电费', icon: Money },
    { path: '/fee-payment', title: '缴费流水', icon: Tickets },
    { path: '/fee-report', title: '费用报表', icon: TrendCharts },
    { path: '/repair', title: '报修', icon: Tools },
    { path: '/visitor', title: '访客', icon: Promotion },
    { path: '/change-room-manage', title: '调宿管理', icon: Switch },
    { path: '/move-out-manage', title: '退宿管理', icon: Remove },
    { path: '/announcement', title: '公告管理', icon: Bell },
    { path: '/system-config', title: '系统设置', icon: Setting },
    { path: '/user', title: '用户管理', icon: Key },
  ]},
]

const employeeGroups = [
  { label: '个人', items: [
    { path: '/dashboard', title: '我的概览', icon: House },
    { path: '/my-info', title: '我的信息', icon: UserFilled },
    { path: '/my-allocation', title: '入住信息', icon: Connection },
    { path: '/notification', title: '消息通知', icon: Bell },
  ]},
  { label: '业务', items: [
    { path: '/repair', title: '报修', icon: Tools },
    { path: '/fee', title: '水电费', icon: Money },
    { path: '/my-payment', title: '在线缴费', icon: CreditCard },
    { path: '/visitor', title: '访客登记', icon: Promotion },
  ]},
]

const menuGroups = computed(() =>
  store.role === 'admin' ? adminGroups : employeeGroups
)

const active = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '')

const avatarText = computed(() =>
  (store.realName || store.username || '?').slice(0, 1).toUpperCase()
)

function onCommand(c) {
  if (c === 'logout') { store.logout(); router.push('/login') }
  if (c === 'password') { pwdDialog.value = true }
  if (c === 'profile') { router.push('/my-info') }
}

// 消息通知角标
const unread = ref(0)
let timer = null
function loadUnread() {
  if (store.role !== 'employee') { unread.value = 0; return }
  api.unreadCount().then(r => (unread.value = r.data || 0)).catch(() => {})
}
function goNotification() { router.push('/notification') }
watch(() => route.path, () => loadUnread())
onMounted(() => {
  loadUnread()
  timer = setInterval(loadUnread, 30000)
})
onUnmounted(() => clearInterval(timer))

function changePwd() {
  pwdRef.value.validate(valid => {
    if (!valid) return
    // TODO: call API
    pwdDialog.value = false
  })
}
</script>

<style scoped>
.app-layout { height: 100vh; }
.aside {
  background: #0f172a;
  transition: width .25s ease;
  display: flex; flex-direction: column; overflow: hidden;
}
.brand {
  height: 58px; display: flex; align-items: center; gap: 10px;
  padding: 0 14px; color: #fff;
  background: linear-gradient(135deg, #1e3a8a, #2563EB);
  flex-shrink: 0;
}
.brand-logo {
  width: 32px; height: 32px; border-radius: 8px; flex-shrink: 0;
  background: rgba(255,255,255,.18);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 16px;
}
.brand-name { font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; }

.menu-group-label {
  padding: 12px 16px 4px;
  font-size: 10px; font-weight: 600; letter-spacing: 1px;
  text-transform: uppercase; color: #475569;
  white-space: nowrap;
}

.side-menu { border-right: none; flex: 1; }
.side-menu:not(.el-menu--collapse) { width: 210px; }
.side-menu :deep(.el-menu-item) {
  height: 42px; line-height: 42px;
  margin: 2px 8px; border-radius: 8px;
  padding-left: 12px !important;
  font-size: 13.5px;
}
.side-menu :deep(.el-menu-item.is-active) { background: rgba(37,99,235,.8) !important; }
.side-menu :deep(.el-menu-item:hover) { background: rgba(255,255,255,.05) !important; }
.side-menu :deep(.el-icon) { font-size: 16px; }

.header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; height: 56px; padding: 0 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,.06); position: relative; z-index: 5;
}
.h-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { font-size: 18px; cursor: pointer; color: #64748b; }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 8px; border-radius: 8px; }
.user-chip:hover { background: #f1f5f9; }
.avatar { background: #2563EB; color: #fff; font-size: 12px; font-weight: 600; }
.uname { font-size: 13px; font-weight: 500; color: #374151; }
.h-right { display: flex; align-items: center; gap: 10px; }
.role-badge {
  font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 20px;
}
.role-badge.admin { background: #fee2e2; color: #dc2626; }
.role-badge.employee { background: #dcfce7; color: #16a34a; }
.bell-badge { display: flex; align-items: center; margin-right: 6px; }
.bell-btn { font-size: 20px; color: #64748b; cursor: pointer; transition: color .2s; }
.bell-btn:hover { color: #2563EB; }

.main { background: #f4f6fb; padding: 16px; overflow-y: auto; }
</style>
