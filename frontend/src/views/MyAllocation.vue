<template>
  <div class="page-container">
    <ListPageHeader title="我的入住信息" subtitle="查看当前入住详情，可申请调宿或退宿">
      <template #actions>
        <el-button type="warning" plain @click="openChange" :disabled="!alloc"><el-icon><Switch /></el-icon> 申请调宿</el-button>
        <el-button type="danger" plain @click="openMoveOut" :disabled="!alloc"><el-icon><Remove /></el-icon> 申请退宿</el-button>
      </template>
    </ListPageHeader>

    <div class="card" v-loading="loading">
      <el-card v-if="alloc && alloc.allocation" shadow="never" class="alloc-card">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="楼栋">{{ alloc.building ? alloc.building.name : '-' }}</el-descriptions-item>
          <el-descriptions-item label="房间号">{{ alloc.room ? alloc.room.roomNo : '-' }}</el-descriptions-item>
          <el-descriptions-item label="房型">
            <el-tag size="small" type="primary">{{ roomTypeMap[alloc.room ? alloc.room.type : ''] || (alloc.room ? alloc.room.type : '-') }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="床位">{{ alloc.allocation.bedNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入住日期">{{ alloc.allocation.checkInDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="押金">
            <span style="color:#2563EB;font-weight:600">¥{{ alloc.allocation.deposit != null ? alloc.allocation.deposit : '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
      <EmptyState v-else title="您当前未入住" description="暂无入住记录，入住后可在此查看详情并申请调宿/退宿" />
    </div>

    <!-- 申请调宿 -->
    <el-dialog v-model="changeDialog" title="申请调宿" width="460px" destroy-on-close>
      <el-form :model="changeForm" :rules="changeRules" ref="changeRef" label-width="90px">
        <el-form-item label="目标房型" prop="targetRoomType">
          <el-select v-model="changeForm.targetRoomType" placeholder="请选择目标房型" style="width:100%">
            <el-option :value="1" label="单人间" />
            <el-option :value="2" label="双人间" />
            <el-option :value="4" label="四人间" />
            <el-option :value="6" label="六人间" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="changeForm.reason" type="textarea" :rows="4" placeholder="请说明调宿原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialog = false">取消</el-button>
        <el-button type="primary" :loading="changeLoading" @click="submitChange">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 申请退宿 -->
    <el-dialog v-model="moveOutDialog" title="申请退宿" width="460px" destroy-on-close>
      <el-form :model="moveOutForm" :rules="moveOutRules" ref="moveOutRef" label-width="90px">
        <el-form-item label="退宿日期" prop="moveOutDate">
          <el-date-picker v-model="moveOutForm.moveOutDate" type="date" value-format="YYYY-MM-DD" placeholder="选择退宿日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="押金退还" prop="depositRefund">
          <el-input-number v-model="moveOutForm.depositRefund" :min="0" :precision="2" :step="50" style="width:100%" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveOutDialog = false">取消</el-button>
        <el-button type="primary" :loading="moveOutLoading" @click="submitMoveOut">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Switch, Remove } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import my from '../api/my'
import changeRoomApi from '../api/changeRoom'
import moveOutApi from '../api/moveOut'

const roomTypeMap = { 1: '单人间', 2: '双人间', 4: '四人间', 6: '六人间' }

const alloc = ref(null)
const loading = ref(false)

const changeDialog = ref(false)
const changeLoading = ref(false)
const changeRef = ref(null)
const changeForm = reactive({ targetRoomType: null, reason: '' })
const changeRules = {
  targetRoomType: [{ required: true, message: '请选择目标房型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写申请原因', trigger: 'blur' }]
}

const moveOutDialog = ref(false)
const moveOutLoading = ref(false)
const moveOutRef = ref(null)
const moveOutForm = reactive({ moveOutDate: '', depositRefund: 0 })
const moveOutRules = {
  moveOutDate: [{ required: true, message: '请选择退宿日期', trigger: 'change' }]
}

function load() {
  loading.value = true
  my.myAllocation()
    .then(r => { alloc.value = r.data || null })
    .catch(() => ElMessage.error('加载入住信息失败'))
    .finally(() => (loading.value = false))
}

function openChange() {
  changeForm.targetRoomType = null
  changeForm.reason = ''
  changeDialog.value = true
}

function submitChange() {
  changeRef.value.validate(valid => {
    if (!valid) return
    changeLoading.value = true
    changeRoomApi.add({ targetRoomType: changeForm.targetRoomType, reason: changeForm.reason })
      .then(() => { ElMessage.success('调宿申请已提交'); changeDialog.value = false; load() })
      .catch(() => ElMessage.error('提交失败'))
      .finally(() => (changeLoading.value = false))
  })
}

function openMoveOut() {
  moveOutForm.moveOutDate = ''
  moveOutForm.depositRefund = alloc.value && alloc.value.allocation && alloc.value.allocation.deposit != null ? Number(alloc.value.allocation.deposit) : 0
  moveOutDialog.value = true
}

function submitMoveOut() {
  moveOutRef.value.validate(valid => {
    if (!valid) return
    moveOutLoading.value = true
    moveOutApi.add({ moveOutDate: moveOutForm.moveOutDate, depositRefund: moveOutForm.depositRefund })
      .then(() => { ElMessage.success('退宿申请已提交'); moveOutDialog.value = false; load() })
      .catch(() => ElMessage.error('提交失败'))
      .finally(() => (moveOutLoading.value = false))
  })
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); padding: 16px; }
.alloc-card { border-radius: 10px; }
</style>
