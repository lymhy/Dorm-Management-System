<template>
  <div class="page-container">
    <ListPageHeader title="系统参数配置" subtitle="维护水价、电价、默认押金等基础参数" />

    <div class="card" v-loading="loading">
      <el-descriptions :column="1" border v-if="configs.length">
        <el-descriptions-item v-for="c in configs" :key="c.key" :label="c.name || c.label || c.key">
          <div class="cfg-row">
            <template v-if="editing[c.key]">
              <el-input-number
                v-model="drafts[c.key]"
                :min="0"
                :precision="2"
                :step="0.5"
                style="width: 220px"
              />
              <span class="cfg-unit" v-if="c.unit">{{ c.unit }}</span>
              <el-button type="success" size="small" @click="save(c)"><el-icon><Check /></el-icon> 保存</el-button>
              <el-button size="small" @click="cancel(c)">取消</el-button>
            </template>
            <template v-else>
              <span class="cfg-value">
                {{ formatValue(c) }}
                <span class="cfg-unit" v-if="c.unit">{{ c.unit }}</span>
              </span>
              <el-button size="small" type="primary" link @click="startEdit(c)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
            </template>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <EmptyState v-else title="暂无系统参数" description="未获取到任何配置项" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Check, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ListPageHeader from '../components/ListPageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import systemConfig from '../api/systemConfig'

const configs = ref([])
const loading = ref(false)
const editing = reactive({})
const drafts = reactive({})

function formatValue(c) {
  const v = c.value
  if (v == null || v === '') return '-'
  return Number(v).toFixed(2).replace(/\.?0+$/, '') || v
}

function load() {
  loading.value = true
  systemConfig.list()
    .then(r => { configs.value = Array.isArray(r.data) ? r.data : (r.data && r.data.records) || [] })
    .catch(() => ElMessage.error('加载系统参数失败'))
    .finally(() => (loading.value = false))
}

function startEdit(c) {
  editing[c.key] = true
  drafts[c.key] = Number(c.value)
}

function cancel(c) {
  editing[c.key] = false
  drafts[c.key] = Number(c.value)
}

function save(c) {
  systemConfig.update({ key: c.key, value: drafts[c.key] })
    .then(() => {
      c.value = drafts[c.key]
      editing[c.key] = false
      ElMessage.success('已保存')
    })
    .catch(() => ElMessage.error('保存失败'))
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 16px 20px; }
.card { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); padding: 16px; }
.cfg-row { display: flex; align-items: center; gap: 10px; }
.cfg-value { font-size: 14px; font-weight: 600; color: #1e293b; }
.cfg-unit { font-size: 12px; color: #94a3b8; margin-left: 2px; }
</style>
