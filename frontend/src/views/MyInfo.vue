<template>
  <div class="page-container">
    <ListPageHeader title="个人信息" subtitle="查看我的基本资料">
      <template #actions>
        <el-button type="primary" @click="pwdDialog = true"><el-icon><Key /></el-icon> 修改密码</el-button>
      </template>
    </ListPageHeader>

    <div class="card" v-loading="loading">
      <el-descriptions :column="2" border v-if="info">
        <el-descriptions-item label="姓名">{{ info.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ info.empNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ info.dept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ info.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ info.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">{{ info.entryDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="info.status === 1 ? 'success' : 'danger'" size="small">
            {{ info.status === 1 ? '在职' : '离职' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <EmptyState v-else title="暂未获取到个人信息" description="请稍后重试或联系管理员" />
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialog" title="修改密码" width="420px" destroy-on-close>
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="90px">
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input v-model="pwdForm.oldPwd" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="pwdForm.newPwd" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="changePwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import my from '../api/my'

const info = ref(null)
const loading = ref(false)

const pwdDialog = ref(false)
const pwdLoading = ref(false)
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

function load() {
  loading.value = true
  my.me()
    .then(r => { info.value = r.data || null })
    .catch(() => ElMessage.error('加载个人信息失败'))
    .finally(() => (loading.value = false))
}

function changePwd() {
  pwdRef.value.validate(valid => {
    if (!valid) return
    pwdLoading.value = true
    my.changePassword(pwdForm.oldPwd, pwdForm.newPwd)
      .then(() => {
        ElMessage.success('密码修改成功')
        pwdDialog.value = false
        pwdForm.oldPwd = ''
        pwdForm.newPwd = ''
        pwdForm.confirmPwd = ''
      })
      .catch(() => ElMessage.error('密码修改失败'))
      .finally(() => (pwdLoading.value = false))
  })
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); padding: 16px; }
</style>
