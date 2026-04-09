<template>
  <div class="score-list">
    <div class="page-card">
      <div class="page-header">
        <h2>我的成绩</h2>
      </div>

      <el-table :data="scoreList" v-loading="loading" stripe>
        <el-table-column prop="paperTitle" label="试卷名称" min-width="200" />
        <el-table-column prop="submitTime" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="得分" width="100" align="center">
          <template #default="{ row }">
            <span class="score" :class="getScoreClass(row.totalScore)">
              {{ row.totalScore ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="gradingStatus" label="批改状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.gradingStatus)">
              {{ getStatusText(row.gradingStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              @click="router.push(`/score/detail/${row.id}`)"
              :disabled="row.gradingStatus !== 'COMPLETED'"
            >
              查看详情
            </el-button>
            <el-button size="small" @click="router.push(`/ranking/${row.paperId}`)">
              排行榜
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && scoreList.length === 0" class="empty-tip">
        <el-empty description="暂无考试记录" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyScores } from '@/api'

const router = useRouter()
const loading = ref(false)
const scoreList = ref([])

const getStatusType = (status) => {
  const map = { PENDING: 'warning', GRADING: 'info', COMPLETED: 'success' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { PENDING: '待批改', GRADING: '批改中', COMPLETED: '已完成' }
  return map[status] || status
}

const getScoreClass = (score) => {
  if (score === null || score === undefined) return ''
  if (score >= 90) return 'excellent'
  if (score >= 60) return 'pass'
  return 'fail'
}

const formatTime = (time) => {
  if (!time) return '-'
  // 处理 ISO 格式: 2026-02-03T18:31:40.322952718
  if (typeof time === 'string' && time.includes('T')) {
    const [datePart, timePart] = time.split('T')
    const timeOnly = timePart.split('.')[0] // 去掉纳秒部分
    return `${datePart} ${timeOnly}`
  }
  return time
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyScores()
    scoreList.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.score-list {
  .score {
    font-size: 18px;
    font-weight: 600;

    &.excellent { color: #67C23A; }
    &.pass { color: #409EFF; }
    &.fail { color: #F56C6C; }
  }

  .empty-tip {
    padding: 40px;
  }
}
</style>
