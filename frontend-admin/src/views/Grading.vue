<template>
  <div class="grading">
    <div class="page-card">
      <div class="page-header">
        <h2>待批改列表</h2>
      </div>

      <el-table :data="recordList" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无待批改试卷" />
        </template>
        <el-table-column prop="paperTitle" label="试卷名称" min-width="200" />
        <el-table-column prop="username" label="考生" width="120" />
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column prop="totalScore" label="当前得分" width="100" align="center">
          <template #default="{ row }">
            <span class="score">{{ row.totalScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="gradingStatus" label="批改状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.gradingStatus)">
              {{ getStatusText(row.gradingStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="success" 
              @click="handleAiGrade(row)"
              :loading="gradingId === row.id"
              :disabled="row.gradingStatus === 'COMPLETED'"
            >
              <el-icon><MagicStick /></el-icon>
              AI 批卷
            </el-button>
            <el-button size="small" type="primary" @click="openDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="答卷详情" width="800px" top="5vh">
      <div v-if="currentRecord" class="detail-content">
        <div class="detail-header">
          <span>考生：{{ currentRecord.username }}</span>
          <span>试卷：{{ currentRecord.paperTitle }}</span>
          <span>得分：<strong>{{ currentRecord.totalScore ?? '-' }}</strong></span>
        </div>

        <div v-for="(answer, index) in currentRecord.answers" :key="answer.id" class="answer-item">
          <div class="answer-header">
            <span class="q-num">第 {{ index + 1 }} 题</span>
            <el-tag :class="getTypeClass(answer.question?.type)" size="small">
              {{ getTypeName(answer.question?.type) }}
            </el-tag>
            <span class="q-score">满分 {{ answer.question?.score }} 分</span>
            <el-tag v-if="answer.isGraded" type="success" size="small">已批改</el-tag>
            <el-tag v-else type="warning" size="small">待批改</el-tag>
          </div>

          <div class="q-content">
            <div class="label">题目：</div>
            <div>{{ answer.question?.content }}</div>
          </div>

          <div v-if="answer.question?.options" class="q-options">
            <div class="label">选项：</div>
            <div>{{ formatOptions(answer.question.options) }}</div>
          </div>

          <div class="q-answer">
            <div class="label">标准答案：</div>
            <div class="correct">{{ answer.question?.answer }}</div>
          </div>

          <div class="q-user-answer">
            <div class="label">考生答案：</div>
            <div :class="{ wrong: answer.isGraded && answer.score < answer.question?.score }">
              {{ answer.userAnswer || '(未作答)' }}
            </div>
          </div>

          <div class="grading-area">
            <el-form inline>
              <el-form-item label="得分">
                <el-input-number 
                  v-model="answer.editScore" 
                  :min="0" 
                  :max="answer.question?.score" 
                  size="small"
                />
              </el-form-item>
              <el-form-item label="评语">
                <el-input v-model="answer.editComment" size="small" style="width: 300px" />
              </el-form-item>
              <el-form-item>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="handleManualGrade(answer)"
                  :loading="answer.grading"
                >
                  保存
                </el-button>
              </el-form-item>
            </el-form>
            <div v-if="answer.aiComment" class="ai-comment">
              <el-icon><ChatDotRound /></el-icon>
              AI 评语：{{ answer.aiComment }}
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingGrading, aiGrade, manualGrade, getScoreDetail } from '@/api'

const loading = ref(false)
const recordList = ref([])
const gradingId = ref(null)
const detailVisible = ref(false)
const currentRecord = ref(null)

const typeMap = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题'
}

const getTypeName = (type) => typeMap[type] || type
const getTypeClass = (type) => {
  const map = {
    SINGLE_CHOICE: 'question-type single',
    MULTI_CHOICE: 'question-type multi',
    TRUE_FALSE: 'question-type truefalse',
    FILL_BLANK: 'question-type fill',
    SHORT_ANSWER: 'question-type short',
    ESSAY: 'question-type essay'
  }
  return map[type] || ''
}

const getStatusType = (status) => {
  const map = { PENDING: 'warning', GRADING: 'info', COMPLETED: 'success' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { PENDING: '待批改', GRADING: '批改中', COMPLETED: '已完成' }
  return map[status] || status
}

const formatOptions = (options) => {
  try {
    const arr = JSON.parse(options)
    return arr.join(' | ')
  } catch {
    return options
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPendingGrading()
    // 前端防御性过滤：只显示待批改/批改中状态，确保已完成的不出现
    recordList.value = res.data.filter(item => {
      return item.gradingStatus === 'PENDING' || item.gradingStatus === 'GRADING'
    })
  } finally {
    loading.value = false
  }
}

const handleAiGrade = async (row) => {
  gradingId.value = row.id
  try {
    await aiGrade(row.id)
    ElMessage.success('AI 批卷完成')
    loadData()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    gradingId.value = null
  }
}

const openDetail = async (row) => {
  const res = await getScoreDetail(row.id)
  currentRecord.value = res.data
  // Initialize edit fields
  currentRecord.value.answers.forEach(a => {
    a.editScore = a.score ?? 0
    a.editComment = a.aiComment || ''
    a.grading = false
  })
  detailVisible.value = true
}

const handleManualGrade = async (answer) => {
  answer.grading = true
  try {
    await manualGrade(answer.id, answer.editScore, answer.editComment)
    answer.score = answer.editScore
    answer.aiComment = answer.editComment
    answer.isGraded = true
    ElMessage.success('保存成功')
    loadData()
  } finally {
    answer.grading = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.grading {
  .score {
    font-weight: 600;
    color: #409EFF;
  }

  .detail-content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-header {
    display: flex;
    gap: 24px;
    padding: 16px;
    background: #F5F7FA;
    border-radius: 8px;
    margin-bottom: 16px;

    strong {
      color: #409EFF;
      font-size: 18px;
    }
  }

  .answer-item {
    background: #FAFAFA;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 16px;
    border: 1px solid #EBEEF5;

    .answer-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #EBEEF5;

      .q-num {
        font-weight: 600;
      }

      .q-score {
        color: #909399;
        font-size: 12px;
      }
    }

    .label {
      color: #909399;
      font-size: 12px;
      margin-bottom: 4px;
    }

    .q-content, .q-options, .q-answer, .q-user-answer {
      margin-bottom: 12px;
    }

    .correct {
      color: #67C23A;
    }

    .wrong {
      color: #F56C6C;
    }

    .grading-area {
      background: #fff;
      padding: 12px;
      border-radius: 4px;
      margin-top: 12px;

      .ai-comment {
        margin-top: 8px;
        color: #909399;
        font-size: 12px;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}
</style>
