<template>
  <div class="page-container">
    <ListPageHeader
      title="访客管理" subtitle="登记访客信息，记录进出时间"
      icon="UserFilled"
      :stats="[{label:'访客总数',value:total},{label:'今日来访',value:todayCount}]"
    >
      <template #actions>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 访客登记</el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="name" label="访客姓名" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="受访员工" min-width="110">
          <template #default="s">{{ empMap[s.row.visitorEmpId] || s.row.visitorEmpId }}</template>
        </el-table-column>
        <el-table-column label="访问房间" min-width="120">
          <template #default="s">{{ roomMap[s.row.roomId] || s.row.roomId }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="来访事由" min-width="150" show-overflow-tooltip />
        <el-table-column prop="timeIn" label="进入时间" width="160" />
        <el-table-column prop="timeOut" label="离开时间" width="160">
          <template #default="s">{{ s.row.timeOut || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无访客记录" description="点击上方「访客登记」添加第一条记录">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 访客登记</el-button>
          </EmptyState>
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条（今日来访 {{ todayCount }} 人次）</span>
        <el-pagination background layout="prev,pager,next" :total="total" :current-page="current" :page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="dialogTitle" width="540px" destroy-on-close>
      <el-form :model="form" label-width="90px" class="form-grid">
        <el-form-item label="访客姓名" required><el-input v-model="form.name" placeholder="访客姓名" maxlength="20" /></el-form-item>
        <el-form-item label="联系电话" required><el-input v-model="form.phone" placeholder="手机号码" maxlength="20" /></el-form-item>
        <el-form-item label="受访员工">
          <el-select v-model="form.visitorEmpId" placeholder="选择员工" style="width:100%">
            <el-option v-for="e in emps" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问房间">
          <el-select v-model="form.roomId" placeholder="选择房间" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.buildingId}栋-${r.roomNo}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来访事由"><el-input v-model="form.reason" placeholder="探亲/送物/公务等" maxlength="100" /></el-form-item>
        <el-form-item label="进入时间"><el-date-picker v-model="form.timeIn" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="离开时间"><el-date-picker v-model="form.timeOut" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
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
import api from '../api/visitor'
import empApi from '../api/employee'
import roomApi from '../api/room'
import myApi from '../api/my'
import { useUserStore } from '../stores/user'

const list = ref([]), total = ref(0), current = ref(1), size = ref(10), loading = ref(false)
const dialog = ref(false), dialogTitle = ref('')
const emps = ref([]), rooms = ref([])
const empty = { name:'', phone:'', visitorEmpId:null, roomId:null, reason:'', timeIn:'', timeOut:'' }
const form = ref({ ...empty })
const empMap = computed(() => { const m={}; emps.value.forEach(e=>{m[e.id]=e.name}); return m })
const roomMap = computed(() => { const m={}; rooms.value.forEach(r=>{m[r.id]=`${r.buildingId}栋-${r.roomNo}`}); return m })
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0,10)
  return list.value.filter(l => l.timeIn && l.timeIn.startsWith(today)).length
})

function load(p) { if(p)current.value=p; loading.value=true; const store=useUserStore(); const call=store.role==='employee'?myApi.visitorPage(current.value,size.value):api.page(current.value,size.value); call.then(r=>{list.value=r.data.records;total.value=r.data.total}).catch(()=>ElMessage.error('加载失败')).finally(()=>loading.value=false) }
function openAdd() { form.value={...empty, timeIn: new Date().toLocaleString('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit'}).replace(/\//g,'-')}; dialogTitle.value='访客登记'; dialog.value=true }
function openEdit(r) { form.value={...r}; dialogTitle.value='编辑访客'; dialog.value=true }
function submit() { if(!form.value.name||!form.value.phone){ElMessage.warning('请填写必填项');return}; const fn=form.value.id?api.update(form.value):api.add(form.value); fn.then(()=>{ElMessage.success('保存成功');dialog.value=false;load()}).catch(()=>ElMessage.error('保存失败')) }
function remove(r) { ElMessageBox.confirm(`确定删除「${r.name}」的访客记录吗？`,'删除确认',{type:'warning'}).then(()=>api.remove(r.id).then(()=>{ElMessage.success('已删除');load()})).catch(()=>{}) }
onMounted(() => { empApi.page(1,200).then(r=>{emps.value=r.data.records}); roomApi.page(1,200).then(r=>{rooms.value=r.data.records}); load() })
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.pagination-row { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-top:1px solid #f1f5f9; }
.total-hint { font-size:13px; color:#64748b; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:0 8px; }
.form-grid :deep(.el-form-item:last-child) { grid-column:1/-1; }
</style>
