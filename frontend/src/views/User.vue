<template>
  <div class="page-container">
    <ListPageHeader
      title="用户管理"
      subtitle="管理系统登录账号：增删改查、批量导入、重置密码、启用/禁用"
      :stats="[{ label: '账号总数', value: total }, { label: '已禁用', value: disabledCount }]"
    >
      <template #actions>
        <el-button @click="downloadTemplate">
          <el-icon><Download /></el-icon> 下载模板
        </el-button>
        <el-button @click="openImport">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增账号
        </el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名、姓名、手机号"
          style="width:260px"
          clearable
          @clear="doSearch"
          @keyup.enter="doSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterRole" placeholder="角色" clearable style="width:130px" @change="doSearch">
          <el-option value="admin" label="管理员" />
          <el-option value="employee" label="员工" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:130px" @change="doSearch">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="禁用" />
        </el-select>
        <el-button type="primary" @click="doSearch"><el-icon><Search /></el-icon> 搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="s">
            <el-tag :type="s.row.role === 'admin' ? 'danger' : 'success'" size="small" effect="plain">
              {{ s.row.role === 'admin' ? '管理员' : '员工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="150" align="center">
          <template #default="s">
            <el-switch
              :model-value="s.row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="isBuiltinAdmin(s.row)"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="(v) => toggleStatus(s.row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="warning" plain @click="resetPwd(s.row)">重置密码</el-button>
            <el-button
              size="small" type="danger" plain
              :disabled="isBuiltinAdmin(s.row)"
              @click="remove(s.row)"
            >删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无账号" description="点击上方「新增账号」或「批量导入」添加">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增账号</el-button>
          </EmptyState>
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条</span>
        <el-pagination
          background layout="prev,pager,next"
          :total="total" :current-page="current" :page-size="size"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialog" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录账号，如 zhangsan" maxlength="30" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名（需与员工姓名一致）" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option value="admin" label="管理员" />
            <el-option value="employee" label="员工" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号码" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.id ? '' : 'password'">
          <el-input
            v-model="form.password" type="password" show-password
            :placeholder="form.id ? '留空表示不修改' : '默认 123456'"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importDialog" title="批量导入账号（Excel）" width="720px" destroy-on-close>
      <el-alert
        type="info" :closable="false" show-icon
        title="支持 .xlsx / .xls；列：用户名(必填)、姓名、角色(管理员/员工，默认员工)、手机号、密码(默认123456)、状态(启用/禁用，默认启用)"
        style="margin-bottom:12px"
      />
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-change="handleFile"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 Excel 到此处，或 <em>点击选择文件</em></div>
      </el-upload>

      <div v-if="importRows.length" style="margin-top:12px">
        <div class="import-tip">已解析 <strong>{{ importRows.length }}</strong> 条数据，预览：</div>
        <el-table :data="importRows.slice(0, 8)" border size="small" max-height="260">
          <el-table-column prop="username" label="用户名" min-width="110" />
          <el-table-column prop="realName" label="姓名" min-width="90" />
          <el-table-column prop="role" label="角色" width="90" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="status" label="状态" width="70" />
        </el-table>
        <div v-if="importRows.length > 8" class="import-tip">… 其余 {{ importRows.length - 8 }} 条略</div>
      </div>

      <template #footer>
        <el-button @click="importDialog=false">取消</el-button>
        <el-button type="primary" :disabled="!importRows.length" :loading="importing" @click="confirmImport">
          确认导入（{{ importRows.length }} 条）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api/user'

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const keyword = ref('')
const filterRole = ref('')
const filterStatus = ref(null)

const disabledCount = computed(() => list.value.filter(u => u.status === 0).length)

const dialog = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const emptyForm = () => ({ id: null, username: '', realName: '', role: 'employee', phone: '', password: '', status: 1 })
const form = ref(emptyForm())
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

const importDialog = ref(false)
const importRows = ref([])
const importing = ref(false)

function isBuiltinAdmin(row) { return row.username === 'admin' }

function load(p) {
  if (p) current.value = p
  loading.value = true
  api.page(current.value, size.value, keyword.value || undefined,
           filterRole.value || undefined, filterStatus.value ?? undefined)
    .then(r => { list.value = r.data.records || []; total.value = r.data.total || 0 })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => { loading.value = false })
}

function doSearch() { current.value = 1; load() }
function resetFilter() { keyword.value = ''; filterRole.value = ''; filterStatus.value = null; doSearch() }

function openAdd() { form.value = emptyForm(); dialogTitle.value = '新增账号'; dialog.value = true }
function openEdit(row) {
  form.value = { id: row.id, username: row.username, realName: row.realName, role: row.role, phone: row.phone, password: '', status: row.status }
  dialogTitle.value = '编辑账号'
  dialog.value = true
}

function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    const fn = form.value.id ? api.update(form.value) : api.add(form.value)
    fn.then(() => { ElMessage.success('保存成功'); dialog.value = false; load() })
      .catch(() => {})
  })
}

function resetPwd(row) {
  ElMessageBox.prompt(`为「${row.username}」设置新密码：`, '重置密码', {
    inputValue: '123456',
    inputPlaceholder: '请输入新密码',
    inputValidator: v => (v && v.length >= 6) || '密码至少 6 位',
    type: 'warning'
  }).then(({ value }) => {
    api.resetPassword(row.id, value).then(() => ElMessage.success('密码已重置为：' + value)).catch(() => {})
  }).catch(() => {})
}

function toggleStatus(row, val) {
  const action = val === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确定${action}账号「${row.username}」吗？`, `${action}确认`, { type: 'warning' })
    .then(() => {
      api.setStatus(row.id, val).then(() => {
        ElMessage.success(`已${action}`)
        load()
      }).catch(() => {})
    })
    .catch(() => {})
}

function remove(row) {
  ElMessageBox.confirm(`确定删除账号「${row.username}」吗？`, '删除确认', { type: 'warning' })
    .then(() => api.remove(row.id).then(() => { ElMessage.success('已删除'); load() }))
    .catch(() => {})
}

/* ---------- Excel 导入 ---------- */
function openImport() {
  importRows.value = []
  importDialog.value = true
}

function pick(obj, keys) {
  for (const k of keys) {
    if (obj[k] !== undefined && obj[k] !== null && String(obj[k]).trim() !== '') return String(obj[k]).trim()
  }
  return ''
}

function handleFile(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const wb = XLSX.read(new Uint8Array(e.target.result), { type: 'array' })
      const ws = wb.Sheets[wb.SheetNames[0]]
      const raw = XLSX.utils.sheet_to_json(ws, { defval: '' })
      const rows = raw.map(r => {
        const roleRaw = pick(r, ['角色', 'role', 'Role'])
        const role = /admin|管理/i.test(roleRaw) ? 'admin' : 'employee'
        const statusRaw = pick(r, ['状态', 'status', 'Status'])
        const status = /禁用|停用|0|false/i.test(String(statusRaw)) ? 0 : 1
        return {
          username: pick(r, ['用户名', '账号', 'username', 'Username']),
          realName: pick(r, ['姓名', '真实姓名', 'realName', 'RealName']),
          role,
          phone: pick(r, ['手机号', '电话', 'phone', 'Phone']),
          password: pick(r, ['密码', 'password', 'Password']),
          status
        }
      }).filter(r => r.username)
      if (!rows.length) {
        ElMessage.warning('未解析到有效数据（请确认表头含「用户名」列）')
        return
      }
      importRows.value = rows
    } catch (err) {
      ElMessage.error('解析失败：' + err.message)
    }
  }
  reader.readAsArrayBuffer(file)
}

function confirmImport() {
  importing.value = true
  api.importUsers(importRows.value)
    .then(r => {
      const d = r.data || {}
      if (d.fail && d.fail > 0) {
        ElMessageBox.alert(
          `成功 ${d.success} 条，失败 ${d.fail} 条。<br/><br/>` +
          (d.errors || []).map(x => '• ' + x).join('<br/>'),
          '导入结果', { dangerouslyUseHTMLString: true }
        )
      } else {
        ElMessage.success(`导入成功 ${d.success} 条`)
      }
      importDialog.value = false
      load()
    })
    .catch(() => {})
    .finally(() => { importing.value = false })
}

function downloadTemplate() {
  const data = [
    ['用户名', '姓名', '角色', '手机号', '密码', '状态'],
    ['zhangsan', '张三', '员工', '13800000000', '123456', '启用'],
    ['lisi', '李四', '管理员', '13900000000', '', '启用']
  ]
  const ws = XLSX.utils.aoa_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '账号模板')
  XLSX.writeFile(wb, '用户账号导入模板.xlsx')
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.06); overflow: hidden; }
.filter-bar {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 12px 16px; border-bottom: 1px solid #f1f5f9;
}
.pagination-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #f1f5f9;
}
.total-hint { font-size: 13px; color: #64748b; }
.import-tip { margin: 8px 0; font-size: 13px; color: #475569; }
</style>
