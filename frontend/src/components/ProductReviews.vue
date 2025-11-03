<template>
  <div class="product-reviews">
    <h3>商品评价 ({{ totalReviews }})</h3>

    <div class="average-rating" v-if="averageRating > 0">
      <span class="rating-value">{{ averageRating.toFixed(1) }}</span>
      <div class="stars">
        <span v-for="star in 5" :key="star" class="star" :class="{ active: star <= Math.round(averageRating) }">★</span>
      </div>
    </div>

    <div class="reviews-list" v-if="reviews.length > 0">
      <div v-for="review in reviews" :key="review.id" class="review-item">
        <div class="review-header">
          <div class="buyer-info">
            <img :src="review.buyerAvatar" alt="avatar" class="avatar" />
            <span class="buyer-name">{{ review.buyerName }}</span>
          </div>
          <div class="review-date">{{ formatDate(review.createdAt) }}</div>
        </div>

        <div class="ratings">
          <div class="rating-row">
            <span class="label">商品:</span>
            <div class="stars">
              <span v-for="star in 5" :key="'p-' + star" class="star small" :class="{ active: star <= review.productRating }">★</span>
            </div>
          </div>
          <div class="rating-row">
            <span class="label">卖家:</span>
            <div class="stars">
              <span v-for="star in 5" :key="'s-' + star" class="star small" :class="{ active: star <= review.sellerRating }">★</span>
            </div>
          </div>
        </div>

        <div class="review-comment" v-if="review.comment">
          {{ review.comment }}
        </div>

        <div class="review-images" v-if="review.reviewImages && review.reviewImages.length > 0">
          <img v-for="(img, index) in review.reviewImages" :key="index" :src="img" alt="review image" />
        </div>
      </div>
    </div>

    <div v-else class="no-reviews">
      暂无评价
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProductReviews',
  props: {
    reviews: {
      type: Array,
      default: () => [],
    },
    averageRating: {
      type: Number,
      default: 0,
    },
    totalReviews: {
      type: Number,
      default: 0,
    },
  },
  methods: {
    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN');
    },
  },
};
</script>

<style scoped>
.product-reviews {
  margin-top: 30px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

h3 {
  margin-bottom: 20px;
  color: #333;
}

.average-rating {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
}

.rating-value {
  font-size: 36px;
  font-weight: bold;
  color: #ffd700;
}

.stars {
  font-size: 24px;
}

.star {
  color: #ddd;
}

.star.active {
  color: #ffd700;
}

.star.small {
  font-size: 16px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.review-item {
  background: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.buyer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.buyer-name {
  font-weight: 500;
  color: #333;
}

.review-date {
  color: #999;
  font-size: 14px;
}

.ratings {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.label {
  color: #666;
  font-size: 14px;
}

.review-comment {
  color: #333;
  line-height: 1.6;
  margin-top: 10px;
}

.review-images {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.review-images img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 5px;
  cursor: pointer;
}

.no-reviews {
  text-align: center;
  color: #999;
  padding: 40px;
  background: white;
  border-radius: 8px;
}
</style>

