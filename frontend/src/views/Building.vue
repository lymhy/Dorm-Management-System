<template>
  <div class="page-container">
    <ListPageHeader
      title="宿舍楼管理"
      subtitle="管理宿舍楼栋信息"
      :stats="[{label:'楼栋总数',value:total}]"
    >
      <template #actions>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增楼栋
        </el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <!-- 搜索栏 -->
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索楼栋名称、编号、地址、负责人"
          style="width:280px"
          clearable
          @clear="doSearch"
          @keyup.enter="doSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="doSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="name" label="楼栋名称" min-width="110" />
        <el-table-column prop="code" label="楼栋编号" width="110" />
        <el-table-column prop="address" label="地址" min-width="150" />
        <el-table-column prop="floors" label="楼层数" width="80" align="center" />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无楼栋数据" description="点击上方「新增楼栋」添加第一条记录">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增楼栋</el-button>
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

    <el-dialog v-model="dialog" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="form" label-width="90px" class="form-grid">
        <el-form-item label="楼栋名称" required>
          <el-input v-model="form.name" placeholder="如：1号楼" maxlength="20" />
        </el-form-item>
        <el-form-item label="楼栋编号" required>
          <el-input v-model="form.code" placeholder="如：B001" maxlength="20" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="宿舍楼所在地址" maxlength="100" />
        </el-form-item>
        <el-form-item label="楼层数">
          <el-input-number v-model="form.floors" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" placeholder="管理员姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="手机或座机" maxlength="20" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="补充信息" maxlength="200" />
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
import api from '../api/building'

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const dialog = ref(false)
const dialogTitle = ref('')
const keyword = ref('')
const empty = { name:'', code:'', address:'', floors:6, manager:'', phone:'', remark:'' }
const form = ref({ ...empty })

function load(p) {
  if (p) current.value = p
  loading.value = true
  api.page(current.value, size.value, keyword.value || undefined)
    .then(r => { list.value = r.data.records; total.value = r.data.total })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => { loading.value = false })
}

function doSearch() { current.value = 1; load() }

function openAdd() { form.value = { ...empty }; dialogTitle.value = '新增楼栋'; dialog.value = true }
function openEdit(row) { form.value = { ...row }; dialogTitle.value = '编辑楼栋'; dialog.value = true }

function submit() {
  if (!form.value.name || !form.value.code) { ElMessage.warning('请填写必填项'); return }
  const fn = form.value.id ? api.update(form.value) : api.add(form.value)
  fn.then(() => {
    ElMessage.success(form.value.id ? '修改成功' : '添加成功')
    dialog.value = false
    load()
  }).catch(() => ElMessage.error('保存失败'))
}

function remove(row) {
  ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type:'warning' })
    .then(() => api.remove(row.id).then(() => { ElMessage.success('已删除'); load() }))
    .catch(() => {})
}

onMounted(load)
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
