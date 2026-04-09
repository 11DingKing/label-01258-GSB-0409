<template>
  <div class="exam-list">
    <div class="page-card">
      <div class="page-header">
        <h2>可参加的考试</h2>
      </div>

      <div v-if="loading" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        加载中...
      </div>

      <div v-else-if="examList.length === 0" class="empty-tip">
        <el-empty description="暂无可参加的考试" />
      </div>

      <div v-else class="exam-grid">
        <div v-for="exam in examList" :key="exam.id" class="exam-card">
          <div class="exam-header">
            <el-icon :size="24" color="#409EFF"><Notebook /></el-icon>
            <h3>{{ exam.title }}</h3>
          </div>
          <div class="exam-info">
            <div class="info-item">
              <el-icon><Clock /></el-icon>
              <span>{{ exam.durationMinutes }} 分钟</span>
            </div>
            <div class="info-item">
              <el-icon><Trophy /></el-icon>
              <span>满分 {{ exam.totalScore }} 分</span>
            </div>
          </div>
          <p class="exam-desc">{{ exam.description || '暂无描述' }}</p>
          <div class="exam-actions">
            <el-button type="primary" @click="handleStart(exam)">
              <el-icon><VideoPlay /></el-icon>
              开始考试
            </el-button>
            <el-button @click="router.push(`/ranking/${exam.id}`)">
              <el-icon><Rank /></el-icon>
              排行榜
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAvailableExams, startExam } from '@/api'

const router = useRouter()
const loading = ref(false)
const examList = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAvailableExams()
    examList.value = res.data
  } finally {
    loading.value = false
  }
}

const handleStart = async (exam) => {
  await ElMessageBox.confirm(
    `确定开始考试【${exam.title}】？\n考试时长：${exam.durationMinutes} 分钟`,
    '开始考试',
    { type: 'info', confirmButtonText: '开始', cancelButtonText: '取消' }
  )

  const res = await startExam(exam.id)
  ElMessage.success('考试开始，祝你好运！')
  router.push(`/exam/take/${res.data.id}`)
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.exam-list {
  .loading-tip {
    text-align: center;
    padding: 40px;
    color: #909399;
  }

  .empty-tip {
    padding: 40px;
  }

  .exam-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
  }

  .exam-card {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    .exam-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        margin: 0;
      }
    }

    .exam-info {
      display: flex;
      gap: 24px;
      margin-bottom: 12px;

      .info-item {
        display: flex;
        align-items: center;
        gap: 6px;
        color: #606266;
        font-size: 14px;

        .el-icon {
          color: #909399;
        }
      }
    }

    .exam-desc {
      color: #909399;
      font-size: 14px;
      margin-bottom: 20px;
      line-height: 1.6;
    }

    .exam-actions {
      display: flex;
      gap: 12px;
    }
  }
}
</style>
