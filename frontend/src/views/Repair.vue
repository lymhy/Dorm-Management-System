<template>
  <div class="page-container">
    <ListPageHeader
      title="报修管理" subtitle="记录宿舍报修工单，跟踪处理进度"
      icon="Tools"
      :stats="[{label:'工单总数',value:total},{label:'待处理',value:pendingCount},{label:'处理中',value:processingCount}]"
    >
      <template #actions>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增报修</el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="房间" min-width="120">
          <template #default="s">{{ roomMap[s.row.roomId] || s.row.roomId }}</template>
        </el-table-column>
        <el-table-column prop="title" label="报修标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="description" label="问题描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="s"><el-tag :type="statusType[s.row.status]" size="small">{{ statusMap[s.row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="reportTime" label="报修时间" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无报修记录" description="点击上方「新增报修」添加第一条记录">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增报修</el-button>
          </EmptyState>
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条（待处理 {{ pendingCount }}，处理中 {{ processingCount }}）</span>
        <el-pagination background layout="prev,pager,next" :total="total" :current-page="current" :page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="90px" class="form-grid">
        <el-form-item label="报修房间" required>
          <el-select v-model="form.roomId" placeholder="选择房间" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.buildingId}栋-${r.roomNo}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修标题" required><el-input v-model="form.title" placeholder="简要描述问题" maxlength="50" /></el-form-item>
        <el-form-item label="问题描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="详细描述报修内容" maxlength="500" /></el-form-item>
        <el-form-item v-if="!isEmployee" label="处理状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option :value="0" label="待处理" />
            <el-option :value="1" label="处理中" />
            <el-option :value="2" label="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEmployee" label="处理人"><el-input v-model="form.handler" placeholder="维修人员姓名" maxlength="20" /></el-form-item>
        <el-form-item v-if="!isEmployee" label="处理备注"><el-input v-model="form.handleRemark" type="textarea" :rows="2" placeholder="处理说明" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api/repair'
import roomApi from '../api/room'
import myApi from '../api/my'
import { useUserStore } from '../stores/user'

const list = ref([]), total = ref(0), current = ref(1), size = ref(10), loading = ref(false)
const dialog = ref(false), dialogTitle = ref('')
const rooms = ref([])
const statusMap = { 0:'待处理', 1:'处理中', 2:'已完成' }
const statusType = { 0:'danger', 1:'warning', 2:'success' }
const empty = { roomId:null, employeeId:null, title:'', description:'', status:0, handler:'', handleRemark:'' }
const form = ref({ ...empty })
const store = useUserStore()
const isEmployee = computed(() => store.role === 'employee')
const roomMap = computed(() => { const m={}; rooms.value.forEach(r=>{m[r.id]=`${r.buildingId}栋-${r.roomNo}`}); return m })
const pendingCount = computed(() => list.value.filter(l=>l.status===0).length)
const processingCount = computed(() => list.value.filter(l=>l.status===1).length)

function load(p) { if(p)current.value=p; loading.value=true; const call=isEmployee.value?myApi.repairPage(current.value,size.value):api.page(current.value,size.value); call.then(r=>{list.value=r.data.records;total.value=r.data.total}).catch(()=>ElMessage.error('加载失败')).finally(()=>loading.value=false) }
function openAdd() { form.value={...empty}; dialogTitle.value='新增报修'; dialog.value=true }
function openEdit(r) { form.value={...r}; dialogTitle.value='编辑报修'; dialog.value=true }
function submit() { if(!form.value.roomId||!form.value.title){ElMessage.warning('请填写必填项');return}; const fn=form.value.id?api.update(form.value):api.add(form.value); fn.then(()=>{ElMessage.success('保存成功');dialog.value=false;load()}).catch(()=>ElMessage.error('保存失败')) }
function remove(r) { ElMessageBox.confirm(`确定删除该报修记录吗？`,'删除确认',{type:'warning'}).then(()=>api.remove(r.id).then(()=>{ElMessage.success('已删除');load()})).catch(()=>{}) }
onMounted(() => { roomApi.page(1,200).then(r=>{rooms.value=r.data.records}); load() })
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.pagination-row { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-top:1px solid #f1f5f9; }
.total-hint { font-size:13px; color:#64748b; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:0 8px; }
.form-grid :deep(.el-form-item:last-child) { grid-column:1/-1; }
</style>
