<template>
  <div v-if="product" class="product-detail-container">
    <div class="product-header">
      <div class="media-carousel-container">
        <!-- 主展示区域 -->
        <div class="main-media-display">
          <div v-if="product.media && product.media.length > 0" class="carousel-wrapper">
            <!-- 左右切换按钮 -->
            <button
              v-if="product.media.length > 1"
              @click="previousMedia"
              class="nav-button nav-button-left"
              :disabled="currentMediaIndex === 0">
              &#8249;
            </button>
            <button
              v-if="product.media.length > 1"
              @click="nextMedia"
              class="nav-button nav-button-right"
              :disabled="currentMediaIndex === product.media.length - 1">
              &#8250;
            </button>

            <!-- 当前媒体展示 -->
            <div class="current-media">
              <img
                v-if="currentMedia && currentMedia.mediaType === 1"
                :src="currentMedia.url"
                :alt="product.name"
                class="main-image" />
              <video
                v-if="currentMedia && currentMedia.mediaType === 2"
                :src="currentMedia.url"
                controls
                class="main-video">
                您的浏览器不支持视频播放
              </video>
            </div>

            <!-- 媒体计数器 -->
            <div v-if="product.media.length > 1" class="media-counter">
              {{ currentMediaIndex + 1 }} / {{ product.media.length }}
            </div>
          </div>

          <!-- 无媒体时的占位符 -->
          <div v-else class="no-media-placeholder">
            <div class="placeholder-icon">📷</div>
            <p>暂无图片</p>
          </div>
        </div>

        <!-- 缩略图导航 -->
        <div v-if="product.media && product.media.length > 1" class="thumbnail-nav">
          <div class="thumbnail-list">
            <div
              v-for="(media, index) in product.media"
              :key="media.id"
              @click="selectMedia(index)"
              :class="['thumbnail-item', { active: index === currentMediaIndex }]">
              <img
                v-if="media.mediaType === 1"
                :src="media.url"
                :alt="`缩略图 ${index + 1}`"
                class="thumbnail-image" />
              <div v-if="media.mediaType === 2" class="thumbnail-video">
                <video :src="media.url" class="thumbnail-video-preview"></video>
                <div class="video-play-icon">▶</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="product-info">
        <h1 class="product-title">{{ product.name }}</h1>
        <div class="product-price">¥{{ product.price }}</div>
        <div class="product-meta">
          <div class="meta-item" v-if="product.category">
            <span class="meta-label">分类：</span>
            <span class="meta-value">{{ getCategoryLabel(product.category) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">成色：</span>
            <span class="meta-value">{{ product.conditionLevel }}/10</span>
          </div>
          <div class="meta-item"><span class="meta-label">库存：</span>
            <span class="meta-value" :class="{ 'low-stock': product.stock < 5 }">
              剩余{{ product.stock }}件
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">位置：</span>
            <span class="meta-value">{{ product.location || '未设置' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">状态：</span>
            <span class="meta-value status-available">在售</span>
          </div>
        </div>
        <div class="product-description">
          <h3>商品描述</h3>
          <p>{{ product.description || '暂无描述' }}</p>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button class="btn btn-home" @click="goHome">🏠 返回主页</button>
          <button
            class="btn btn-primary btn-chat"
            @click="contactSeller"
            v-if="!isOwnProduct">
            💬 联系卖家
          </button>
          <button
            class="btn btn-warning btn-report"
            @click="openReportModal"
            v-if="!isOwnProduct">
            🚩 举报
          </button>
          <button
            class="btn btn-secondary btn-favorite"
            @click="toggleFavorite"
            :class="{ 'is-favorite': isFavorite }">
            {{ isFavorite ? '❤️ 已收藏' : '🤍 收藏' }}
          </button>
          <button class="btn btn-success btn-cart" @click="addToCart" v-if="!isOwnProduct">
            🛒 加入购物车
          </button>
          <button class="btn btn-bargain" @click="startBargain" v-if="!isOwnProduct">
            🔪 砍价购买
          </button>
          <button class="btn btn-primary btn-buy" @click="buyNow" v-if="!isOwnProduct">
            💳 立即购买
          </button>
          <button
            v-if="authStore.user?.role === 'ROLE_ADMIN'"
            class="btn btn-danger btn-delete"
            @click="deleteProduct">
            🗑️ 删除商品
          </button>
        </div>
      </div>
    </div>

    <!-- 卖家信用分 -->
    <div v-if="sellerCreditScore" class="seller-section">
      <h2>📊 卖家信用</h2>
      <CreditScoreCard :creditScore="sellerCreditScore" />
    </div>

    <!-- 商品评价 -->
    <div class="reviews-section">
      <ProductReviews
        :reviews="reviews"
        :averageRating="averageRating"
        :totalReviews="totalReviews"
      />
    </div>


    <!-- 相似商品推荐 -->
    <RecommendationSection
      v-if="similarProducts.length > 0"
      title="👀 看了又看"
      subtitle="其他用户也浏览过这些商品"
      :products="similarProducts"
    />

    <ReportModal
      v-if="showReportModal"
      :product-id="product.id"
      @close="showReportModal = false"
    />
  </div>
  <div v-else class="loading-container">
    <div class="loading-spinner"></div>
    <p>加载中...</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getCategoryLabel } from '@/utils/categoryData';
import { useAuthStore } from '@/store/auth';
import productService from '@/api/productService';
import axios from 'axios';
import { toast } from '@/services/toast';
import RecommendationSection from '@/components/RecommendationSection.vue';
import ReportModal from '@/components/ReportModal.vue';
import ProductReviews from '@/components/ProductReviews.vue';
import CreditScoreCard from '@/components/CreditScoreCard.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const product = ref(null);
const currentMediaIndex = ref(0);
const similarProducts = ref([]);
const isFavorite = ref(false);
const showReportModal = ref(false);
const reviews = ref([]);
const averageRating = ref(0);
const totalReviews = ref(0);
const sellerCreditScore = ref(null);

const openReportModal = () => {
  showReportModal.value = true;
};

// Check if current user owns this product
const isOwnProduct = computed(() => {
  if (!authStore.user || !product.value) return false;
  return authStore.user.id === product.value.userId;
});

const currentMedia = computed(() => {
  if (!product.value?.media || product.value.media.length === 0) return null;
  return product.value.media[currentMediaIndex.value];
});

// 切换到下一个媒体
const nextMedia = () => {
  if (product.value?.media && currentMediaIndex.value < product.value.media.length - 1) {
    currentMediaIndex.value++;
  }
};


// 切换到上一个媒体
const previousMedia = () => {
  if (currentMediaIndex.value > 0) {
    currentMediaIndex.value--;
  }
};

// 选择特定媒体
const selectMedia = (index) => {
  currentMediaIndex.value = index;
};

// 键盘导航支持
const handleKeydown = (event) => {
  if (event.key === 'ArrowLeft') {
    previousMedia();
  } else if (event.key === 'ArrowRight') {
    nextMedia();
  }
};

const fetchProduct = async () => {
  try {
    const response = await productService.getProductById(route.params.id);
    product.value = response.data;
    currentMediaIndex.value = 0; // 重置媒体索引

    // 检查是否已收藏
    await checkFavoriteStatus();

    // 获取商品评价
    await fetchProductReviews();

    // 获取卖家信用分
    await fetchSellerCreditScore();
  } catch (error) {
    console.error('获取商品详情失败:', error);
  }
};

// 获取商品评价
const fetchProductReviews = async () => {
  try {
    const response = await axios.get(`/api/reviews/product/${route.params.id}`);
    if (response.data.success) {
      reviews.value = response.data.reviews;
      averageRating.value = response.data.averageRating;
      totalReviews.value = response.data.totalReviews;
    }
  } catch (error) {
    console.error('获取评价失败:', error);
  }
};

// 获取卖家信用分
const fetchSellerCreditScore = async () => {
  if (!product.value?.userId) return;
  try {
    const response = await axios.get(`/api/credit-score/${product.value.userId}`);
    if (response.data.success) {
      sellerCreditScore.value = response.data.data;
    }
  } catch (error) {
    console.error('获取卖家信用分失败:', error);
  }
};

// 检查收藏状态
const checkFavoriteStatus = async () => {
  try {
    const response = await axios.get(`/api/favorites/check/${route.params.id}`);
    isFavorite.value = response.data.isFavorite;
  } catch (error) {
    console.error('检查收藏状态失败:', error);
  }
};

// 联系卖家
const contactSeller = () => {
  if (!authStore.isLoggedIn) {
    toast('请先登录', 'warning');
    router.push('/login');
    return;
  }

  // 检查是否是自己的商品
  if (isOwnProduct.value) {
    toast('不能联系自己', 'warning');
    return;
  }

  // 触发全局事件来打开聊天气泡
  window.dispatchEvent(new CustomEvent('open-chat', {
    detail: {
      username: product.value.username,
      displayName: product.value.displayName || product.value.username,
      userId: product.value.userId,
      avatarUrl: product.value.avatarUrl || null
    }
  }));
};

// 切换收藏状态
const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    toast('请先登录', 'warning');
    router.push('/login');
    return;
  }

  // 检查是否是自己的商品
  if (isOwnProduct.value) {
    toast('不能收藏自己的商品', 'warning');
    return;
  }

  try {
    if (isFavorite.value) {
      // 取消收藏
      await axios.delete(`/api/favorites/remove/${route.params.id}`);
      isFavorite.value = false;
      toast('已取消收藏', 'info');
    } else {
      // 添加收藏
      await axios.post(`/api/favorites/add/${route.params.id}`);
      isFavorite.value = true;
      toast('收藏成功', 'success');
    }
  } catch (error) {
    console.error('收藏操作失败:', error);
    toast('操作失败，请重试', 'error');
  }

};

// 加入购物车
const addToCart = async () => {
  // 检查是否是自己的商品
  if (isOwnProduct.value) {
    toast('不能将自己的商品加入购物车', 'warning');
    return;
  }

  if (!authStore.isLoggedIn) {
    toast('请先登录', 'warning');
    router.push('/login');
    return;
  }

  try {
    await axios.post('/api/cart/add', {
      productId: route.params.id,
      quantity: 1
    });
    toast('已加入购物车', 'success');
  } catch (error) {
    console.error('加入购物车失败:', error);
    toast('加入购物车失败，请重试', 'error');
  }
};

// 立即购买
const buyNow = async () => {
  // 检查是否是自己的商品
  if (isOwnProduct.value) {
    toast('不能购买自己的商品', 'warning');
    return;
  }

  if (!authStore.isLoggedIn) {
    toast('请先登录', 'warning');
    router.push('/login');
    return;
  }

  // 检查库存
  if (!product.value || product.value.stock < 1) {
    toast('商品库存不足', 'warning');
    return;
  }

  try {
    // 检查是否有未完成的订单
    const checkResponse = await axios.get('/api/orders/check-pending');
    if (checkResponse.data.hasPendingOrder) {
      const orderId = checkResponse.data.orderId;
      if (confirm('您有未完成的订单，是否前往支付？')) {
        router.push(`/payment/${orderId}`);
      }
      return;
    }

    // 创建订单（直接购买单个商品）
    const response = await axios.post('/api/orders/buy-now', {
      productId: route.params.id,
      quantity: 1
    });

    const order = response.data;
    toast('订单创建成功', 'success');

    // 跳转到支付页面（使用路径参数）
    router.push(`/payment/${order.id}`);
  } catch (error) {
    console.error('创建订单失败:', error);
    if (error.response?.data?.message) {
      toast(error.response.data.message, 'error');
    } else {
      toast('创建订单失败，请重试', 'error');
    }
  }
};

// 发起砍价
const startBargain = async () => {
  if (!authStore.isLoggedIn) {
    toast('请先登录', 'warning');
    router.push('/login');
    return;
  }

  if (isOwnProduct.value) {
    toast('不能对自己的商品发起砍价', 'warning');
    return;
  }

  try {
    const response = await axios.post('/api/bargain/start', {
      productId: route.params.id
    });
    const bargainActivity = response.data;
    toast('砍价活动创建成功', 'success');
    // 跳转到砍价页面
    router.push(`/bargain/${bargainActivity.id}`);
  } catch (error) {
    console.error('发起砍价失败:', error);
    if (error.response?.data?.message) {
      toast(error.response.data.message, 'error');
    } else {
      toast('发起砍价失败，请重试', 'error');
    }
  }
};

// 删除商品
const deleteProduct = async () => {
  if (!confirm('确定要删除这个商品吗？此操作不可恢复。')) {
    return;
  }

  try {
    await axios.delete(`/api/products/${route.params.id}`);
    toast('商品删除成功', 'success');
    router.push({ name: 'home' }); // 删除后返回主页
  } catch (error) {
    console.error('删除商品失败:', error);
    toast('删除商品失败，请重试', 'error');
  }
};

// 返回主页
const goHome = () => {
  router.push({ name: 'home' });
};

// Track product view for recommendation system
const trackProductView = async () => {
  try {
    // Send view tracking request (non-blocking, don't wait for response)
    axios.post('/api/history/view', {
      productId: route.params.id
    }).catch(err => {
      // Silent fail - tracking shouldn't affect user experience
      console.debug('View tracking failed:', err);
    });
  } catch (error) {
    // Silent fail
  }
};

// Fetch similar products based on collaborative filtering
const fetchSimilarProducts = async () => {
  try {
    const response = await axios.get(`/api/recommendations/products/${route.params.id}/similar`, {
      params: { limit: 8 }
    });
    similarProducts.value = response.data;
  } catch (error) {
    console.debug('Failed to fetch similar products:', error);
    // Silent fail - recommendations are optional
  }
};

onMounted(() => {
  fetchProduct();
  // Track product view for recommendation system
  trackProductView();
  // Fetch similar products
  fetchSimilarProducts();
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeydown);
});


// 清理事件监听器
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
}

.product-header {
  display: flex;
  gap: 40px;
  margin-bottom: 40px;
}

/* 媒体轮播容器 */
.media-carousel-container {
  flex: 1;
  max-width: 600px;
}

.main-media-display {
  position: relative;
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 15px;
}

.carousel-wrapper {
  position: relative;
  width: 100%;
  height: 400px;
}

.current-media {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image,
.main-video {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 导航按钮 */
.nav-button {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  z-index: 2;
  transition: all 0.3s ease;
}

.nav-button:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-50%) scale(1.1);
}

.nav-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.nav-button-left {
  left: 15px;
}

.nav-button-right {
  right: 15px;
}

/* 媒体计数器 */
.media-counter {
  position: absolute;
  bottom: 15px;
  right: 15px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 5px 12px;
  border-radius: 15px;
  font-size: 14px;
}

/* 无媒体占位符 */
.no-media-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #6c757d;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 10px;
  opacity: 0.5;
}

/* 缩略图导航 */
.thumbnail-nav {
  overflow-x: auto;
  padding: 10px 0;
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  min-width: min-content;
}

.thumbnail-item {
  width: 80px;
  height: 80px;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.thumbnail-item:hover {
  border-color: #007bff;
  transform: scale(1.05);
}

.thumbnail-item.active {
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.thumbnail-image,
.thumbnail-video-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-video {
  position: relative;
  width: 100%;
  height: 100%;
}

.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 16px;
  background: rgba(0, 0, 0, 0.6);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 商品信息部分 */
.product-info {
  flex: 1;
  min-width: 400px;
}

.product-title {
  font-size: 28px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 15px;
  line-height: 1.3;
}

.product-price {
  font-size: 32px;
  font-weight: bold;
  color: #e74c3c;
  margin-bottom: 20px;
}

.product-meta {
  margin-bottom: 25px;
}

.meta-item {
  display: flex;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.meta-label {
  font-weight: 500;
  color: #6c757d;
  min-width: 60px;
}
.meta-value {
  color: #212529;
}

.status-available {
  color: #28a745;
}

.btn-home {
  background: #6c757d;
  color: white;
}

.btn-home:hover {
  background: #5a6268;
}

.product-description h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #212529;
}

.product-description p {
  color: #6c757d;
  line-height: 1.6;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.btn {
  padding: 12px 30px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #545b62;
}

.btn-secondary.is-favorite {
  background: #dc3545;
}

.btn-secondary.is-favorite:hover {
  background: #c82333;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #218838;
}

.btn-bargain {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: white;
  box-shadow: 0 4px 6px rgba(255, 107, 107, 0.3);
}

.btn-bargain:hover {
  background: linear-gradient(135deg, #ff5252 0%, #ff7e43 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(255, 107, 107, 0.4);
}

.btn-warning {
  background: #ffc107;
  color: #212529;
}

.btn-warning:hover {
  background: #e0a800;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.seller-section {
  margin: 40px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
}

.seller-section h2 {
  margin-bottom: 20px;
  color: #333;
}

.reviews-section {
  margin: 40px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .product-header {
    flex-direction: column;
    gap: 20px;
  }

  .media-carousel-container {
    max-width: 100%;
  }

  .product-info {
    min-width: auto;
  }

  .action-buttons {
    flex-direction: column;
  }

  .nav-button {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .nav-button-left {
    left: 10px;
  }

  .nav-button-right {
    right: 10px;
  }
}

.low-stock {
  color: #ff4444;
  font-weight: bold;
}
</style>
