<template>
  <div class="score-detail" v-loading="loading">
    <template v-if="record">
      <!-- Summary Card -->
      <div class="summary-card">
        <div class="summary-left">
          <h2>{{ record.paperTitle }}</h2>
          <div class="summary-info">
            <span>提交时间：{{ formatTime(record.submitTime) }}</span>
          </div>
        </div>
        <div class="summary-right">
          <div class="score-circle" :class="getScoreClass(record.totalScore)">
            <span class="score-value">{{ record.totalScore }}</span>
            <span class="score-label">总分</span>
          </div>
        </div>
      </div>

      <!-- Questions -->
      <div class="questions-section">
        <div v-for="(answer, index) in record.answers" :key="answer.id" class="question-card">
          <div class="question-header">
            <span class="q-num">第 {{ index + 1 }} 题</span>
            <el-tag :class="getTypeClass(answer.question?.type)" size="small">
              {{ getTypeName(answer.question?.type) }}
            </el-tag>
            <span class="q-max-score">满分 {{ answer.question?.score }} 分</span>
            <span class="q-score" :class="getAnswerScoreClass(answer)">
              得分：{{ answer.score ?? '-' }}
            </span>
          </div>

          <div class="q-content">{{ answer.question?.content }}</div>

          <div v-if="answer.question?.options" class="q-options">
            <div class="label">选项：</div>
            <div class="options-list">
              <span 
                v-for="opt in parseOptions(answer.question.options)" 
                :key="opt"
                class="option-item"
                :class="{ 
                  correct: isCorrectOption(opt, answer.question.answer),
                  selected: isSelectedOption(opt, answer.userAnswer),
                  wrong: isSelectedOption(opt, answer.userAnswer) && !isCorrectOption(opt, answer.question.answer)
                }"
              >
                {{ opt }}
              </span>
            </div>
          </div>

          <div class="answer-compare">
            <div class="answer-item correct-answer">
              <div class="label">
                <el-icon color="#67C23A"><CircleCheck /></el-icon>
                正确答案
              </div>
              <div class="value">{{ answer.question?.answer }}</div>
            </div>
            <div class="answer-item user-answer" :class="{ wrong: answer.score < answer.question?.score }">
              <div class="label">
                <el-icon :color="answer.score >= answer.question?.score ? '#67C23A' : '#F56C6C'">
                  <component :is="answer.score >= answer.question?.score ? 'CircleCheck' : 'CircleClose'" />
                </el-icon>
                你的答案
              </div>
              <div class="value">{{ answer.userAnswer || '(未作答)' }}</div>
            </div>
          </div>

          <div v-if="answer.aiComment" class="ai-comment">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ answer.aiComment }}</span>
          </div>
        </div>
      </div>

      <!-- Back Button -->
      <div class="action-bar">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" @click="router.push(`/ranking/${record.paperId}`)">
          查看排行榜
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScoreDetail } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const record = ref(null)

const typeMap = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  ESSAY: '论述题'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
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

const getScoreClass = (score) => {
  if (score >= 90) return 'excellent'
  if (score >= 60) return 'pass'
  return 'fail'
}

const getAnswerScoreClass = (answer) => {
  if (answer.score === null || answer.score === undefined) return ''
  if (answer.score >= answer.question?.score) return 'full'
  if (answer.score > 0) return 'partial'
  return 'zero'
}

const parseOptions = (options) => {
  try {
    return JSON.parse(options)
  } catch {
    return []
  }
}

const isCorrectOption = (opt, answer) => {
  const letter = opt.charAt(0)
  return answer?.includes(letter)
}

const isSelectedOption = (opt, userAnswer) => {
  const letter = opt.charAt(0)
  return userAnswer?.includes(letter)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getScoreDetail(route.params.recordId)
    record.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.score-detail {
  .summary-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 32px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    color: #fff;

    h2 {
      font-size: 24px;
      margin-bottom: 12px;
    }

    .summary-info {
      opacity: 0.9;
    }

    .score-circle {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.2);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .score-value {
        font-size: 32px;
        font-weight: 700;
      }

      .score-label {
        font-size: 12px;
        opacity: 0.8;
      }

      &.excellent { background: rgba(103, 194, 58, 0.3); }
      &.fail { background: rgba(245, 108, 108, 0.3); }
    }
  }

  .question-card {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .question-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #EBEEF5;

      .q-num {
        font-weight: 600;
      }

      .q-max-score {
        color: #909399;
        font-size: 12px;
      }

      .q-score {
        margin-left: auto;
        font-weight: 600;

        &.full { color: #67C23A; }
        &.partial { color: #E6A23C; }
        &.zero { color: #F56C6C; }
      }
    }

    .q-content {
      font-size: 15px;
      line-height: 1.8;
      margin-bottom: 16px;
    }

    .q-options {
      margin-bottom: 16px;

      .label {
        color: #909399;
        font-size: 12px;
        margin-bottom: 8px;
      }

      .options-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .option-item {
        padding: 8px 12px;
        background: #F5F7FA;
        border-radius: 4px;
        font-size: 14px;

        &.correct {
          background: #E1F3D8;
          color: #67C23A;
        }

        &.selected {
          border: 2px solid #409EFF;
        }

        &.wrong {
          background: #FEF0F0;
          color: #F56C6C;
          border-color: #F56C6C;
        }
      }
    }

    .answer-compare {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 16px;

      .answer-item {
        padding: 12px;
        border-radius: 8px;
        background: #F5F7FA;

        .label {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: #909399;
          margin-bottom: 8px;
        }

        .value {
          font-size: 14px;
          line-height: 1.6;
        }

        &.correct-answer {
          background: #F0F9EB;
        }

        &.wrong {
          background: #FEF0F0;
        }
      }
    }

    .ai-comment {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      padding: 12px;
      background: #FDF6EC;
      border-radius: 8px;
      color: #E6A23C;
      font-size: 13px;
      line-height: 1.6;
    }
  }

  .action-bar {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 24px;
  }
}
</style>
