<template>
  <div class="login">
    <div class="login-card">
      <div class="login-head">
        <div class="logo">宿</div>
        <h1>员工宿舍管理系统</h1>
        <p class="sub">Dormitory Management Platform</p>
      </div>
      <el-form :model="form" size="large">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" @keyup.enter="login" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock"
                    show-password @keyup.enter="login" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="login">登 录</el-button>
      </el-form>
      <div class="login-foot">
        <span>管理员 / 员工统一登录</span>
        <span class="demo">演示账号 admin / admin123</span>
      </div>
    </div>
    <div class="login-aside">
      <div class="aside-inner">
        <h2>智慧宿舍 · 一体化管理</h2>
        <p>楼栋 · 房间 · 入住 · 水电费 · 报修 · 访客，全流程在线化。</p>
        <ul>
          <li>实时仪表盘，掌握整体入住与待办</li>
          <li>员工自助报修、访客登记</li>
          <li>管理员高效排房与计费</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import userApi from '../api/user'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useUserStore()
const form = ref({ username: '', password: '' })
const loading = ref(false)

function login() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  userApi.login(form.value).then(res => {
    store.setToken(res.data.token)
    store.setInfo(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  }).catch(() => {}).finally(() => { loading.value = false })
}
</script>

<style scoped>
.login { height: 100vh; display: flex; }
.login-aside {
  flex: 1;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563EB 55%, #0ea5e9 100%);
  color: #fff; display: flex; align-items: center; justify-content: center;
}
.aside-inner { max-width: 440px; padding: 40px; }
.aside-inner h2 { font-size: 30px; margin: 0 0 16px; }
.aside-inner p { opacity: .9; line-height: 1.7; }
.aside-inner ul { margin-top: 24px; padding-left: 18px; line-height: 2; opacity: .92; }
.login-card {
  width: 440px; background: #fff; display: flex; flex-direction: column;
  justify-content: center; padding: 0 56px; box-shadow: var(--shadow-md);
}
.login-head { text-align: center; margin-bottom: 28px; }
.logo {
  width: 56px; height: 56px; border-radius: 14px; margin: 0 auto 14px;
  background: linear-gradient(135deg, #2563EB, #0ea5e9); color: #fff;
  font-size: 26px; font-weight: 700; display: flex; align-items: center; justify-content: center;
}
.login-head h1 { font-size: 22px; margin: 0; }
.sub { color: var(--c-muted); font-size: 13px; letter-spacing: 1px; margin: 6px 0 0; }
.login-btn { width: 100%; }
.login-foot {
  margin-top: 18px; display: flex; justify-content: space-between;
  color: var(--c-muted); font-size: 12px;
}
.demo { color: #2563EB; }
@media (max-width: 768px) {
  .login-aside { display: none; }
  .login-card { width: 100%; padding: 0 24px; }
}
</style>
