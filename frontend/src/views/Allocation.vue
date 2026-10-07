<template>
  <div class="page-container">
    <ListPageHeader title="入住管理" subtitle="记录员工入住宿舍情况，支持新增、编辑、删除入住记录" icon="Key">
      <template #actions>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增入住</el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="员工" min-width="110">
          <template #default="s">{{ empMap[s.row.employeeId] || s.row.employeeId }}</template>
        </el-table-column>
        <el-table-column label="房间" min-width="120">
          <template #default="s">{{ roomMap[s.row.roomId] || s.row.roomId }}</template>
        </el-table-column>
        <el-table-column prop="bedNo" label="床位" width="80" align="center" />
        <el-table-column prop="deposit" label="押金(元)" width="100">
          <template #default="s">¥{{ s.row.deposit }}</template>
        </el-table-column>
        <el-table-column prop="checkInDate" label="入住日期" width="120" />
        <el-table-column prop="checkOutDate" label="退宿日期" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="s"><el-tag :type="s.row.status===1?'success':'info'" size="small">{{ s.row.status===1?'在住':'已退' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无入住记录" description="点击上方「新增入住」添加第一条记录">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增入住</el-button>
          </EmptyState>
        </template>
      </el-table>
      <div class="pagination-row">
        <span class="total-hint">共 <strong>{{ total }}</strong> 条</span>
        <el-pagination background layout="prev,pager,next" :total="total" :current-page="current" :page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px" class="form-grid">
        <el-form-item label="员工" required>
          <el-select v-model="form.employeeId" placeholder="选择员工" style="width:100%">
            <el-option v-for="e in emps" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间" required>
          <el-select v-model="form.roomId" placeholder="选择房间" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.buildingId}栋-${r.roomNo}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="床位"><el-input v-model="form.bedNo" placeholder="如：A1" maxlength="10" /></el-form-item>
        <el-form-item label="押金"><el-input-number v-model="form.deposit" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="入住日期"><el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="退宿日期"><el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="在住" inactive-text="已退" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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
import api from '../api/allocation'
import empApi from '../api/employee'
import roomApi from '../api/room'

const list = ref([]), total = ref(0), current = ref(1), size = ref(10), loading = ref(false)
const dialog = ref(false), dialogTitle = ref('')
const emps = ref([]), rooms = ref([])
const empty = { employeeId:null, roomId:null, bedNo:'A1', deposit:500, checkInDate:'', checkOutDate:null, status:1, remark:'' }
const form = ref({ ...empty })
const empMap = computed(() => { const m={}; emps.value.forEach(e=>{m[e.id]=e.name}); return m })
const roomMap = computed(() => { const m={}; rooms.value.forEach(r=>{m[r.id]=`${r.buildingId}栋-${r.roomNo}`}); return m })

function load(p) { if(p)current.value=p; loading.value=true; api.page(current.value,size.value).then(r=>{list.value=r.data.records;total.value=r.data.total}).catch(()=>ElMessage.error('加载失败')).finally(()=>loading.value=false) }
function openAdd() { form.value={...empty}; dialogTitle.value='新增入住'; dialog.value=true }
function openEdit(r) { form.value={...r}; dialogTitle.value='编辑入住'; dialog.value=true }
function submit() { if(!form.value.employeeId||!form.value.roomId){ElMessage.warning('请选择员工和房间');return}; const fn=form.value.id?api.update(form.value):api.add(form.value); fn.then(()=>{ElMessage.success('保存成功');dialog.value=false;load()}).catch(()=>ElMessage.error('保存失败')) }
function remove(r) { ElMessageBox.confirm(`确定删除该入住记录吗？`,'删除确认',{type:'warning'}).then(()=>api.remove(r.id).then(()=>{ElMessage.success('已删除');load()})).catch(()=>{}) }
onMounted(() => {
  empApi.page(1,200).then(r=>{emps.value=r.data.records})
  roomApi.page(1,200).then(r=>{rooms.value=r.data.records})
  load()
})
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); overflow: hidden; }
.pagination-row { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-top:1px solid #f1f5f9; }
.total-hint { font-size:13px; color:#64748b; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:0 8px; }
.form-grid :deep(.el-form-item:last-child) { grid-column:1/-1; }
</style>
