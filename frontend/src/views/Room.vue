<template>
  <div class="page-container">
    <ListPageHeader
      title="房间管理"
      subtitle="查看和管理所有宿舍房间"
      :stats="[{label:'房间总数',value:total},{label:'空置',value:emptyCount},{label:'已满',value:fullCount}]"
    >
      <template #actions>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增房间
        </el-button>
      </template>
    </ListPageHeader>

    <div class="card">
      <!-- 筛选栏：楼栋 → 楼层 → 状态 -->
      <div class="filter-bar">
        <span class="filter-label">筛选：</span>

        <!-- 楼栋选择 -->
        <el-select
          v-model="filterBuilding"
          placeholder="全部楼栋"
          clearable
          style="width:140px"
          @change="onBuildingChange"
        >
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>

        <!-- 楼层（根据楼栋动态生成） -->
        <el-select
          v-model="filterFloor"
          placeholder="全部楼层"
          clearable
          style="width:120px"
          :disabled="!filterBuilding"
          @change="load(1)"
        >
          <el-option v-for="f in floorOptions" :key="f" :label="f + '层'" :value="f" />
        </el-select>

        <!-- 状态筛选 -->
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:120px" @change="load(1)">
          <el-option v-for="(v,k) in statusMap" :key="k" :label="v" :value="Number(k)" />
        </el-select>

        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="楼栋" min-width="110">
          <template #default="s">{{ buildingMap[s.row.buildingId] || `楼栋${s.row.buildingId}` }}</template>
        </el-table-column>
        <el-table-column prop="roomNo" label="房号" width="90" />
        <el-table-column prop="floor" label="楼层" width="70" align="center" />
        <el-table-column label="房型" width="100" align="center">
          <template #default="s">{{ typeMap[s.row.type] || s.row.type }}</template>
        </el-table-column>
        <el-table-column label="容量" width="100" align="center">
          <template #default="s">
            <el-progress
              :percentage="s.row.capacity > 0 ? Math.round(s.row.occupied / s.row.capacity * 100) : 0"
              :color="s.row.status === 2 ? '#10b981' : s.row.status === 0 ? '#94a3b8' : '#f59e0b'"
              :stroke-width="6"
            >
              <template #default>{{ s.row.occupied }}/{{ s.row.capacity }}</template>
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="s">
            <el-tag :type="statusType[s.row.status]" size="small">{{ statusMap[s.row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="月租" width="100">
          <template #default="s">￥{{ s.row.price }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="s">
            <el-button size="small" type="primary" plain @click="openEdit(s.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(s.row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无房间数据" description="点击上方「新增房间」添加第一条记录">
            <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增房间</el-button>
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

    <el-dialog v-model="dialog" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px" class="form-grid">
        <el-form-item label="所属楼栋" required>
          <el-select v-model="form.buildingId" placeholder="选择楼栋" style="width:100%">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房号" required>
          <el-input v-model="form.roomNo" placeholder="如：101" maxlength="20" />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="form.floor" :min="1" :max="99" style="width:100%" />
        </el-form-item>
        <el-form-item label="房型">
          <el-select v-model="form.type" placeholder="选择房型" style="width:100%">
            <el-option v-for="(v,k) in typeMap" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="床位数量">
          <el-input-number v-model="form.capacity" :min="1" :max="20" style="width:100%" />
        </el-form-item>
        <el-form-item label="月租(元)">
          <el-input-number v-model="form.price" :min="0" :precision="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import roomApi from '../api/room'
import buildingApi from '../api/building'

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const dialog = ref(false)
const dialogTitle = ref('')
const buildings = ref([])

const typeMap = { 1:'单人房', 2:'双人房', 4:'四人间', 6:'六人间', 8:'八人间' }
const statusMap = { 0:'空置', 1:'部分入住', 2:'已满', 3:'维修中' }
const statusType = { 0:'info', 1:'warning', 2:'success', 3:'danger' }

const filterBuilding = ref(null)
const filterFloor = ref(null)
const filterStatus = ref(null)
const empty = { buildingId:1, roomNo:'', floor:1, type:4, capacity:4, occupied:0, status:0, price:800, remark:'' }
const form = ref({ ...empty })

const buildingMap = computed(() => {
  const m = {}
  buildings.value.forEach(b => { m[b.id] = b.name })
  return m
})

// 根据所选楼栋动态生成楼层选项
const floorOptions = computed(() => {
  if (!filterBuilding.value) return []
  const b = buildings.value.find(b => b.id === filterBuilding.value)
  if (!b) return []
  return Array.from({ length: b.floors }, (_, i) => i + 1)
})

const emptyCount = computed(() => list.value.filter(r => r.status === 0).length)
const fullCount = computed(() => list.value.filter(r => r.status === 2).length)

function load(p) {
  if (p) current.value = p
  loading.value = true
  roomApi.page(current.value, size.value, filterBuilding.value || undefined, filterFloor.value || undefined, filterStatus.value || undefined)
    .then(r => { list.value = r.data.records; total.value = r.data.total })
    .catch(() => ElMessage.error('加载失败'))
    .finally(() => { loading.value = false })
}

function onBuildingChange() {
  filterFloor.value = null
  load(1)
}

function resetFilter() {
  filterBuilding.value = null
  filterFloor.value = null
  filterStatus.value = null
  load(1)
}

function loadBuildings() {
  buildingApi.page(1, 100).then(r => { buildings.value = r.data.records })
}

function openAdd() { form.value = { ...empty }; dialogTitle.value = '新增房间'; dialog.value = true }
function openEdit(row) { form.value = { ...row }; dialogTitle.value = '编辑房间'; dialog.value = true }

function submit() {
  if (!form.value.buildingId || !form.value.roomNo) { ElMessage.warning('请填写必填项'); return }
  const fn = form.value.id ? roomApi.update(form.value) : roomApi.add(form.value)
  fn.then(() => { ElMessage.success('保存成功'); dialog.value = false; load() }).catch(() => ElMessage.error('保存失败'))
}

function remove(row) {
  ElMessageBox.confirm(`确定删除「${buildingMap.value[row.buildingId] || row.buildingId}栋 - ${row.roomNo}」吗？`, '删除确认', { type:'warning' })
    .then(() => roomApi.remove(row.id).then(() => { ElMessage.success('已删除'); load() })).catch(() => {})
}

onMounted(() => { loadBuildings(); load() })
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.06); overflow: hidden; }
.filter-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid #f1f5f9;
  flex-wrap: wrap;
}
.filter-label { font-size: 13px; color: #64748b; white-space: nowrap; }
.pagination-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid #f1f5f9;
}
.total-hint { font-size: 13px; color: #64748b; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 8px; }
.form-grid :deep(.el-form-item:last-child) { grid-column: 1 / -1; }
</style>
