<template>
  <div class="ranking">
    <div class="page-card">
      <div class="page-header">
        <h2>
          <el-icon><Trophy /></el-icon>
          {{ paperTitle || '排行榜' }}
        </h2>
        <el-button @click="router.back()">返回</el-button>
      </div>

      <div v-if="loading" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        加载中...
      </div>

      <div v-else-if="rankingList.length === 0" class="empty-tip">
        <el-empty description="暂无排名数据" />
      </div>

      <div v-else class="ranking-list">
        <!-- Top 3 Podium -->
        <div class="podium">
          <!-- 第二名 -->
          <div v-if="topThree[1]" class="podium-item second">
            <div class="medal">🥈</div>
            <div class="avatar silver">{{ topThree[1].nickname?.charAt(0) || 'U' }}</div>
            <div class="info">
              <div class="nickname">{{ topThree[1].nickname }}</div>
              <div class="score">{{ topThree[1].score }} 分</div>
            </div>
            <div class="pedestal"></div>
          </div>
          
          <!-- 第一名 -->
          <div v-if="topThree[0]" class="podium-item first">
            <div class="crown">👑</div>
            <div class="medal">🥇</div>
            <div class="avatar gold">{{ topThree[0].nickname?.charAt(0) || 'U' }}</div>
            <div class="info">
              <div class="nickname">{{ topThree[0].nickname }}</div>
              <div class="score">{{ topThree[0].score }} 分</div>
            </div>
            <div class="pedestal"></div>
          </div>
          
          <!-- 第三名 -->
          <div v-if="topThree[2]" class="podium-item third">
            <div class="medal">🥉</div>
            <div class="avatar bronze">{{ topThree[2].nickname?.charAt(0) || 'U' }}</div>
            <div class="info">
              <div class="nickname">{{ topThree[2].nickname }}</div>
              <div class="score">{{ topThree[2].score }} 分</div>
            </div>
            <div class="pedestal"></div>
          </div>
        </div>

        <!-- Rest of the list -->
        <div v-if="restList.length > 0" class="rest-ranking">
          <div class="section-title">其他排名</div>
          <div class="rank-item" v-for="item in restList" :key="item.rank">
            <div class="rank-num">{{ item.rank }}</div>
            <div class="user-info">
              <div class="mini-avatar">{{ item.nickname?.charAt(0) || 'U' }}</div>
              <span class="name">{{ item.nickname }}</span>
            </div>
            <div class="score-info">
              <span class="score">{{ item.score }}</span>
              <span class="unit">分</span>
            </div>
            <div class="time">{{ formatTime(item.submitTime) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRanking } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const rankingList = ref([])
const paperTitle = ref('')

const topThree = computed(() => rankingList.value.slice(0, 3))
const restList = computed(() => rankingList.value.slice(3))

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRanking(route.params.paperId)
    rankingList.value = res.data
    if (res.data.length > 0 && res.data[0].paperTitle) {
      paperTitle.value = res.data[0].paperTitle + ' - 排行榜'
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.ranking {
  .loading-tip {
    text-align: center;
    padding: 40px;
    color: #909399;
  }

  .empty-tip {
    padding: 40px;
  }

  .podium {
    display: flex;
    justify-content: center;
    align-items: flex-end;
    gap: 16px;
    padding: 48px 24px 0;
    margin-bottom: 32px;
    background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    min-height: 320px;

    .podium-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;

      .crown {
        font-size: 32px;
        margin-bottom: -8px;
        animation: bounce 2s infinite;
      }

      .medal {
        font-size: 36px;
        margin-bottom: 8px;
        filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
      }

      .avatar {
        width: 72px;
        height: 72px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        font-weight: 700;
        color: #fff;
        border: 4px solid #fff;
        box-shadow: 0 4px 16px rgba(0,0,0,0.2);
        margin-bottom: 12px;

        &.gold {
          background: linear-gradient(135deg, #f5af19, #f12711);
        }
        &.silver {
          background: linear-gradient(135deg, #bdc3c7, #2c3e50);
        }
        &.bronze {
          background: linear-gradient(135deg, #c97b4b, #8b4513);
        }
      }

      .info {
        text-align: center;
        margin-bottom: 16px;

        .nickname {
          font-size: 16px;
          font-weight: 600;
          color: #fff;
          margin-bottom: 4px;
          text-shadow: 0 1px 2px rgba(0,0,0,0.3);
        }

        .score {
          font-size: 22px;
          font-weight: 700;
          color: #FFD700;
          text-shadow: 0 2px 4px rgba(0,0,0,0.3);
        }
      }

      .pedestal {
        width: 100px;
        background: linear-gradient(180deg, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0.1) 100%);
        border-radius: 8px 8px 0 0;
        border: 2px solid rgba(255,255,255,0.3);
        border-bottom: none;
      }

      &.first {
        .avatar {
          width: 88px;
          height: 88px;
          font-size: 32px;
        }
        .medal {
          font-size: 44px;
        }
        .pedestal {
          height: 100px;
          width: 120px;
        }
        .info .score {
          font-size: 26px;
        }
      }

      &.second {
        .pedestal {
          height: 70px;
        }
      }

      &.third {
        .pedestal {
          height: 50px;
        }
      }
    }
  }

  .rest-ranking {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.08);

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 2px solid #f0f0f0;
    }

    .rank-item {
      display: flex;
      align-items: center;
      padding: 14px 16px;
      border-radius: 8px;
      margin-bottom: 8px;
      transition: all 0.2s;

      &:nth-child(odd) {
        background: #fafafa;
      }

      &:hover {
        background: #f0f7ff;
        transform: translateX(4px);
      }

      .rank-num {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: linear-gradient(135deg, #e0e0e0, #c0c0c0);
        color: #666;
        font-size: 14px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
      }

      .user-info {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 12px;

        .mini-avatar {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          background: linear-gradient(135deg, #667eea, #764ba2);
          color: #fff;
          font-size: 14px;
          font-weight: 600;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .name {
          font-size: 15px;
          font-weight: 500;
          color: #303133;
        }
      }

      .score-info {
        margin-right: 24px;

        .score {
          font-size: 20px;
          font-weight: 700;
          color: #409EFF;
        }

        .unit {
          font-size: 12px;
          color: #909399;
          margin-left: 2px;
        }
      }

      .time {
        font-size: 13px;
        color: #909399;
        min-width: 150px;
        text-align: right;
      }
    }
  }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
</style>
