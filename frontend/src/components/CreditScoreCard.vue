<template>
  <div class="credit-score-card">
    <div class="credit-header">
      <div class="user-info">
        <img :src="creditScore.avatarUrl" alt="avatar" class="avatar" />
        <div>
          <h3>{{ creditScore.displayName }}</h3>
          <p class="username">@{{ creditScore.username }}</p>
        </div>
      </div>
      <div class="credit-badge" :class="'level-' + creditScore.level">
        <div class="level-icon">{{ getLevelIcon(creditScore.level) }}</div>
        <div class="level-name">{{ creditScore.levelName }}</div>
      </div>
    </div>

    <div class="credit-score">
      <div class="score-circle">
        <div class="score-value">{{ creditScore.totalScore }}</div>
        <div class="score-label">信用分</div>
      </div>
    </div>

    <div class="credit-stats">
      <div class="stat-item">
        <div class="stat-value">{{ creditScore.totalSales }}</div>
        <div class="stat-label">已售商品</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ creditScore.totalPurchases }}</div>
        <div class="stat-label">已购商品</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ creditScore.positiveRate ? creditScore.positiveRate.toFixed(1) + '%' : '0%' }}</div>
        <div class="stat-label">好评率</div>
      </div>
    </div>

    <div class="review-summary" v-if="creditScore.totalReviews > 0">
      <h4>评价详情</h4>
      <div class="review-bars">
        <div class="review-bar">
          <span class="bar-label">好评</span>
          <div class="bar-container">
            <div class="bar positive" :style="{ width: getPercentage(creditScore.positiveReviews, creditScore.totalReviews) + '%' }"></div>
          </div>
          <span class="bar-count">{{ creditScore.positiveReviews }}</span>
        </div>
        <div class="review-bar">
          <span class="bar-label">中评</span>
          <div class="bar-container">
            <div class="bar neutral" :style="{ width: getPercentage(creditScore.neutralReviews, creditScore.totalReviews) + '%' }"></div>
          </div>
          <span class="bar-count">{{ creditScore.neutralReviews }}</span>
        </div>
        <div class="review-bar">
          <span class="bar-label">差评</span>
          <div class="bar-container">
            <div class="bar negative" :style="{ width: getPercentage(creditScore.negativeReviews, creditScore.totalReviews) + '%' }"></div>
          </div>
          <span class="bar-count">{{ creditScore.negativeReviews }}</span>
        </div>
      </div>
    </div>

    <div class="rating-info" v-if="creditScore.averageSellerRating > 0">
      <span>平均卖家评分：</span>
      <div class="stars">
        <span v-for="star in 5" :key="star" class="star" :class="{ active: star <= Math.round(creditScore.averageSellerRating) }">★</span>
      </div>
      <span class="rating-value">{{ creditScore.averageSellerRating.toFixed(1) }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CreditScoreCard',
  props: {
    creditScore: {
      type: Object,
      required: true,
    },
  },
  methods: {
    getLevelIcon(level) {
      const icons = {
        1: '🌱',
        2: '🥉',
        3: '🥈',
        4: '🥇',
        5: '💎',
      };
      return icons[level] || '🌱';
    },
    getPercentage(count, total) {
      return total > 0 ? (count / total) * 100 : 0;
    },
  },
};
</script>

<style scoped>
.credit-score-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 15px;
  padding: 25px;
  color: white;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.credit-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 3px solid white;
  object-fit: cover;
}

.user-info h3 {
  margin: 0;
  font-size: 20px;
}

.username {
  margin: 5px 0 0 0;
  opacity: 0.9;
  font-size: 14px;
}

.credit-badge {
  text-align: center;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
}

.level-icon {
  font-size: 32px;
  margin-bottom: 5px;
}

.level-name {
  font-size: 14px;
  font-weight: 500;
}

.credit-score {
  display: flex;
  justify-content: center;
  margin: 30px 0;
}

.score-circle {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 4px solid white;
}

.score-value {
  font-size: 48px;
  font-weight: bold;
}

.score-label {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 5px;
}

.credit-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin: 20px 0;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
}

.review-summary {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 15px;
  margin-top: 20px;
}

.review-summary h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
}

.review-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-label {
  width: 40px;
  font-size: 14px;
}

.bar-container {
  flex: 1;
  height: 20px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  overflow: hidden;
}

.bar {
  height: 100%;
  transition: width 0.3s;
}

.bar.positive {
  background: #4caf50;
}

.bar.neutral {
  background: #ff9800;
}

.bar.negative {
  background: #f44336;
}

.bar-count {
  width: 30px;
  text-align: right;
  font-size: 14px;
}

.rating-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
}

.stars {
  font-size: 20px;
}

.star {
  color: rgba(255, 255, 255, 0.3);
}

.star.active {
  color: #ffd700;
}

.rating-value {
  font-size: 18px;
  font-weight: bold;
}
</style>

