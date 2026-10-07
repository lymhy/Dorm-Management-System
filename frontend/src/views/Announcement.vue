<template>
  <div class="page-container">
    <ListPageHeader title="公告管理" subtitle="发布与维护宿舍公告通知">
      <template #actions>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增公告</el-button>
      </template>
    </ListPageHeader>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="输入标题关键字搜索"
        clearable
        style="width: 240px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="onSearch">搜索</el-button>
    </div>

    <div class="card">
      <el-table :data="displayList" border stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="优先级" width="100" align="center">
          <template #default="s">
            <el-tag :type="typeMap[s.row.priority] || 'info'" size="small">
              {{ priorityTagMap[s.row.priority] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisher" label="发布人" width="120" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无公告" description="点击上方「新增公告」发布第一条通知" />
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ displayTotal }}</strong> 条</span>
        <el-pagination
          background
          layout="prev,pager,next"
          :total="displayTotal"
          :current-page="current"
          :page-size="size"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialog" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option :value="1" label="普通" />
            <el-option :value="2" label="重要" />
            <el-option :value="3" label="紧急" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="发布人">
          <el-input :model-value="form.publisher" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api/announcement'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const priorityTagMap = { 1: '普通', 2: '重要', 3: '紧急' }
const typeMap = { 1: 'info', 2: 'warning', 3: 'danger' }

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const keyword = ref('')

const displayList = computed(() =>
  keyword.value ? list.value.filter(r => (r.title || '').includes(keyword.value)) : list.value
)
const displayTotal = computed(() => (keyword.value ? displayList.value.length : total.value))

const dialog = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const emptyForm = () => ({
  id: null,
  title: '',
  priority: 1,
  content: '',
  publisher: store.username || store.realName || '管理员'
})
const form = ref(emptyForm())
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

function load(p) {
  if (p) current.value = p
  loading.value = true
  api.page(current.value, size.value)
    .then(r => {
      list.value = (r.data && r.data.records) || []
      total.value = (r.data && r.data.total) || 0
    })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => (loading.value = false))
}

function onSearch() {
  current.value = 1
  load()
}

function openAdd() {
  form.value = emptyForm()
  dialogTitle.value = '新增公告'
  dialog.value = true
}

function openEdit(row) {
  form.value = { ...row, publisher: row.publisher || form.value.publisher }
  dialogTitle.value = '编辑公告'
  dialog.value = true
}

function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    const fn = form.value.id ? api.update(form.value) : api.add(form.value)
    fn
      .then(() => {
        ElMessage.success('保存成功')
        dialog.value = false
        load()
      })
      .catch(() => ElMessage.error('保存失败'))
  })
}

function remove(row) {
  ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
    .then(() => api.remove(row.id).then(() => { ElMessage.success('已删除'); load() }))
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.pagination-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.total-hint { font-size: 13px; color: #64748b; }
</style>
