<template>
  <div class="page-container">
    <ListPageHeader
      title="员工管理"
      subtitle="管理员工基本信息"
      :stats="[{label:'员工总数',value:total}]"
    >
      <template #actions>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增员工
        </el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <!-- 多级筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名、工号、部门、手机号"
          style="width:240px"
          clearable
          @clear="doSearch"
          @keyup.enter="doSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterDept" placeholder="部门" clearable filterable style="width:150px" @change="doSearch">
          <el-option v-for="d in deptOptions" :key="d" :value="d" :label="d" />
        </el-select>
        <el-select v-model="filterGender" placeholder="性别" clearable style="width:110px" @change="doSearch">
          <el-option :value="1" label="男" />
          <el-option :value="2" label="女" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="在职状态" clearable style="width:130px" @change="doSearch">
          <el-option :value="1" label="在职" />
          <el-option :value="0" label="离职" />
        </el-select>
        <el-button type="primary" @click="doSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="empNo" label="工号" width="100" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="s">{{ s.row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="dept" label="部门" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="idCard" label="身份证号" min-width="170" />
        <el-table-column prop="entryDate" label="入职日期" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="s">
            <el-tag :type="s.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ s.row.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无员工数据" description="点击上方「新增员工」添加第一条记录">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增员工</el-button>
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

    <el-dialog v-model="dialog" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="90px" class="form-grid">
        <el-form-item label="工号" required>
          <el-input v-model="form.empNo" placeholder="如：E001" maxlength="20" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" placeholder="员工姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" style="width:100%">
            <el-option :value="1" label="男" />
            <el-option :value="2" label="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.dept" placeholder="所属部门" maxlength="30" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号码" maxlength="20" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="form.idCard" placeholder="18位身份证号" maxlength="20" />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="form.entryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="在职状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="在职" inactive-text="离职" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api/employee'

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const dialog = ref(false)
const dialogTitle = ref('')
const keyword = ref('')
const filterStatus = ref(null)
const filterDept = ref('')
const filterGender = ref(null)
const deptOptions = ref([])
const empty = { empNo:'', name:'', gender:1, dept:'', phone:'', idCard:'', entryDate:'', status:1 }
const form = ref({ ...empty })

function load(p) {
  if (p) current.value = p
  loading.value = true
  api.page(current.value, size.value, keyword.value || undefined,
           filterStatus.value ?? undefined, filterDept.value || undefined, filterGender.value ?? undefined)
    .then(r => { list.value = r.data.records; total.value = r.data.total })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => { loading.value = false })
}

function loadDepts() {
  api.depts().then(r => { deptOptions.value = r.data || [] }).catch(() => {})
}

function doSearch() { current.value = 1; load() }

function resetFilter() {
  keyword.value = ''
  filterStatus.value = null
  filterDept.value = ''
  filterGender.value = null
  doSearch()
}

function openAdd() { form.value = { ...empty }; dialogTitle.value = '新增员工'; dialog.value = true }
function openEdit(row) { form.value = { ...row }; dialogTitle.value = '编辑员工'; dialog.value = true }

function submit() {
  if (!form.value.empNo || !form.value.name) { ElMessage.warning('请填写必填项'); return }
  const fn = form.value.id ? api.update(form.value) : api.add(form.value)
  fn.then(() => { ElMessage.success('保存成功'); dialog.value = false; load() }).catch(() => ElMessage.error('保存失败'))
}

function remove(row) {
  ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type:'warning' })
    .then(() => api.remove(row.id).then(() => { ElMessage.success('已删除'); load() }))
    .catch(() => {})
}

onMounted(() => { load(); loadDepts() })
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.06); overflow: hidden; }
.filter-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}
.pagination-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #f1f5f9;
}
.total-hint { font-size: 13px; color: #64748b; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 8px; }
.form-grid :deep(.el-form-item:last-child) { grid-column: 1 / -1; }
</style>
