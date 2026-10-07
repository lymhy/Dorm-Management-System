import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// Lazy-loaded route components break the request->router->views->api->request
// circular dependency and enable code splitting.
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘' } },
      { path: 'building', name: 'Building', component: () => import('../views/Building.vue'), meta: { title: '宿舍楼管理', roles: ['admin'] } },
      { path: 'room', name: 'Room', component: () => import('../views/Room.vue'), meta: { title: '房间管理', roles: ['admin'] } },
      { path: 'employee', name: 'Employee', component: () => import('../views/Employee.vue'), meta: { title: '员工管理', roles: ['admin'] } },
      { path: 'allocation', name: 'Allocation', component: () => import('../views/Allocation.vue'), meta: { title: '入住管理', roles: ['admin'] } },
      { path: 'fee', name: 'Fee', component: () => import('../views/Fee.vue'), meta: { title: '水电费管理' } },
      { path: 'fee-payment', name: 'FeePayment', component: () => import('../views/FeePayment.vue'), meta: { title: '缴费流水', roles: ['admin'] } },
      { path: 'fee-report', name: 'FeeReport', component: () => import('../views/FeeReport.vue'), meta: { title: '费用报表', roles: ['admin'] } },
      { path: 'repair', name: 'Repair', component: () => import('../views/Repair.vue'), meta: { title: '报修管理' } },
      { path: 'visitor', name: 'Visitor', component: () => import('../views/Visitor.vue'), meta: { title: '访客管理' } },
      { path: 'notification', name: 'Notification', component: () => import('../views/Notification.vue'), meta: { title: '消息通知', roles: ['employee'] } },

      // 管理员路由（roles: ['admin']）
      { path: 'announcement', name: 'Announcement', component: () => import('../views/Announcement.vue'), meta: { title: '公告管理', roles: ['admin'] } },
      { path: 'system-config', name: 'SystemConfig', component: () => import('../views/SystemConfig.vue'), meta: { title: '系统设置', roles: ['admin'] } },
      { path: 'user', name: 'User', component: () => import('../views/User.vue'), meta: { title: '用户管理', roles: ['admin'] } },
      { path: 'change-room-manage', name: 'ChangeRoomManage', component: () => import('../views/ChangeRoomManage.vue'), meta: { title: '调宿管理', roles: ['admin'] } },
      { path: 'move-out-manage', name: 'MoveOutManage', component: () => import('../views/MoveOutManage.vue'), meta: { title: '退宿管理', roles: ['admin'] } },

      // 员工路由（无 roles 限制，employee 可访问）
      { path: 'my-info', name: 'MyInfo', component: () => import('../views/MyInfo.vue'), meta: { title: '个人信息', roles: ['employee'] } },
      { path: 'my-allocation', name: 'MyAllocation', component: () => import('../views/MyAllocation.vue'), meta: { title: '入住信息', roles: ['employee'] } },
      { path: 'my-payment', name: 'MyPayment', component: () => import('../views/MyPayment.vue'), meta: { title: '在线缴费', roles: ['employee'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) {
    return '/login'
  }
  // Role guard: block employees from admin-only routes.
  if (to.meta.roles && to.meta.roles.length) {
    const role = useUserStore().role
    if (role && !to.meta.roles.includes(role)) return '/dashboard'
  }
})

export default router
