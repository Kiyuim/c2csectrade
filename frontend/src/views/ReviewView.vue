<template>
  <div class="review-container">
    <div class="review-card">
      <h2>评价订单</h2>

      <div class="product-info" v-if="orderInfo">
        <h3>订单信息</h3>
        <p>订单号: {{ orderId }}</p>
      </div>

      <div class="rating-section">
        <h3>商品评分</h3>
        <div class="star-rating">
          <span
            v-for="star in 5"
            :key="'product-' + star"
            @click="productRating = star"
            class="star"
            :class="{ active: star <= productRating }"
          >
            ★
          </span>
        </div>
      </div>

      <div class="rating-section">
        <h3>卖家评分</h3>
        <div class="star-rating">
          <span
            v-for="star in 5"
            :key="'seller-' + star"
            @click="sellerRating = star"
            class="star"
            :class="{ active: star <= sellerRating }"
          >
            ★
          </span>
        </div>
      </div>

      <div class="comment-section">
        <h3>评价内容</h3>
        <textarea
          v-model="comment"
          placeholder="请输入您的评价..."
          rows="5"
        ></textarea>
      </div>

      <div class="anonymous-section">
        <label>
          <input type="checkbox" v-model="isAnonymous" />
          匿名评价
        </label>
      </div>

      <div class="button-group">
        <button @click="submitReview" class="submit-btn" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交评价' }}
        </button>
        <button @click="goBack" class="cancel-btn">取消</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ReviewView',
  data() {
    return {
      orderId: null,
      orderInfo: null,
      productRating: 5,
      sellerRating: 5,
      comment: '',
      isAnonymous: false,
      submitting: false,
    };
  },
  mounted() {
    this.orderId = this.$route.params.orderId;
    if (!this.orderId) {
      this.$router.push('/orders');
    }
  },
  methods: {
    async submitReview() {
      if (this.productRating === 0 || this.sellerRating === 0) {
        alert('请为商品和卖家评分');
        return;
      }

      this.submitting = true;
      try {
        const token = localStorage.getItem('token');
        const response = await axios.post(
          '/api/reviews',
          {
            orderId: parseInt(this.orderId),
            productRating: this.productRating,
            sellerRating: this.sellerRating,
            comment: this.comment,
            isAnonymous: this.isAnonymous,
          },
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        if (response.data.success) {
          alert('评价成功！');
          this.$router.push('/orders');
        } else {
          alert(response.data.message || '评价失败');
        }
      } catch (error) {
        console.error('提交评价失败:', error);
        alert(error.response?.data?.message || '评价失败，请重试');
      } finally {
        this.submitting = false;
      }
    },
    goBack() {
      this.$router.back();
    },
  },
};
</script>

<style scoped>
.review-container {
  max-width: 600px;
  margin: 20px auto;
  padding: 20px;
}

.review-card {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

h3 {
  margin-top: 20px;
  margin-bottom: 10px;
  color: #666;
  font-size: 16px;
}

.product-info {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 5px;
  margin-bottom: 20px;
}

.rating-section {
  margin-bottom: 20px;
}

.star-rating {
  font-size: 32px;
  cursor: pointer;
}

.star {
  color: #ddd;
  transition: color 0.2s;
}

.star.active {
  color: #ffd700;
}

.star:hover {
  color: #ffed4e;
}

.comment-section textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  resize: vertical;
}

.anonymous-section {
  margin: 20px 0;
}

.anonymous-section label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.anonymous-section input[type='checkbox'] {
  margin-right: 8px;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.button-group {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.submit-btn,
.cancel-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn {
  background: #007bff;
  color: white;
}

.submit-btn:hover:not(:disabled) {
  background: #0056b3;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e0e0e0;
}
</style>

