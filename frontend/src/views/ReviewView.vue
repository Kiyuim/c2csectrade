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

      <div class="image-upload-section">
        <h3>上传图片（最多5张）</h3>
        <div class="upload-area">
          <div class="image-preview-list">
            <div v-for="(image, index) in reviewImages" :key="index" class="image-preview-item">
              <img :src="image" alt="评价图片" />
              <button @click="removeImage(index)" class="remove-image-btn">×</button>
            </div>
            <label v-if="reviewImages.length < 5" class="upload-btn">
              <input
                type="file"
                accept="image/*"
                multiple
                @change="handleImageUpload"
                style="display: none"
              />
              <span class="upload-icon">📷</span>
              <span>添加图片</span>
            </label>
          </div>
        </div>
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
      reviewImages: [], // 评价图片URL数组
      isAnonymous: false,
      submitting: false,
      uploading: false,
    };
  },
  mounted() {
    this.orderId = this.$route.params.orderId;
    if (!this.orderId) {
      this.$router.push('/orders');
    }
  },
  methods: {
    async handleImageUpload(event) {
      const files = Array.from(event.target.files);
      const remainingSlots = 5 - this.reviewImages.length;

      if (files.length > remainingSlots) {
        alert(`最多只能上传${remainingSlots}张图片`);
        return;
      }

      this.uploading = true;
      try {
        const token = localStorage.getItem('token');

        for (const file of files) {
          // 验证文件大小（限制5MB）
          if (file.size > 5 * 1024 * 1024) {
            alert(`图片 ${file.name} 超过5MB，请选择更小的图片`);
            continue;
          }

          // 上传图片到服务器
          const formData = new FormData();
          formData.append('file', file);

          const response = await axios.post('/api/upload', formData, {
            headers: {
              Authorization: `Bearer ${token}`,
              'Content-Type': 'multipart/form-data',
            },
          });

          if (response.data.url) {
            this.reviewImages.push(response.data.url);
          }
        }
      } catch (error) {
        console.error('上传图片失败:', error);
        alert('上传图片失败，请重试');
      } finally {
        this.uploading = false;
        // 清空input，允许重复选择相同文件
        event.target.value = '';
      }
    },

    removeImage(index) {
      this.reviewImages.splice(index, 1);
    },

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
            reviewImages: this.reviewImages, // 添加图片数组
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

.image-upload-section {
  margin: 20px 0;
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e0e0e0;
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  font-size: 18px;
  line-height: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.remove-image-btn:hover {
  background: rgba(255, 0, 0, 0.8);
}

.upload-btn {
  width: 100px;
  height: 100px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.upload-btn:hover {
  border-color: #007bff;
  background: #f0f8ff;
}

.upload-icon {
  font-size: 32px;
  margin-bottom: 5px;
}

.upload-btn span:last-child {
  font-size: 12px;
  color: #666;
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

