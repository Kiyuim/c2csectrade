<template>
  <div class="order-history-page">
    <div class="page-header">
      <button @click="goToHome" class="btn-back-home">🏠 返回主页</button>
      <h1>📦 订单中心</h1>
      <p class="subtitle">查看您的订单和砍价活动</p>
    </div>

    <!-- 标签页导航 -->
    <div class="tabs-nav">
      <button
        @click="currentTab = 'orders'"
        :class="['tab-button', { active: currentTab === 'orders' }]"
      >
        📦 我买到的
      </button>
      <button
        @click="currentTab = 'seller-orders'"
        :class="['tab-button', { active: currentTab === 'seller-orders' }]"
      >
        💰 我卖出的
      </button>
      <button
        @click="currentTab = 'bargains'"
        :class="['tab-button', { active: currentTab === 'bargains' }]"
      >
        🔪 砍价活动
      </button>
    </div>

    <!-- 订单列表标签页 -->
    <div v-show="currentTab === 'orders'">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="orders.length === 0" class="empty-container">
        <div class="empty-icon">📦</div>
        <p>暂无订单记录</p>
        <router-link to="/" class="btn btn-primary">去购物</router-link>
      </div>

      <div v-else class="orders-list">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <div class="order-info">
            <span class="order-number">订单号: {{ order.id }}</span>
            <span class="order-date">{{ formatDate(order.createdAt) }}</span>
          </div>
          <div class="order-status" :class="getStatusClass(order.status, order)">
            {{ getStatusText(order.status, order) }}
          </div>
        </div>

        <div class="order-body">
          <div class="order-amount">
            <span class="label">订单金额：</span>
            <span class="amount">¥{{ order.totalAmount }}</span>
          </div>

          <div v-if="order.paymentMethod" class="order-payment">
            <span class="label">支付方式：</span>
            <span class="payment-badge">
              {{ getPaymentIcon(order.paymentMethod) }} {{ getPaymentText(order.paymentMethod) }}
            </span>
          </div>
        </div>

        <div class="order-actions">
          <!-- 待支付订单：检查是否过期 -->
          <button
            v-if="order.status === 'PENDING' && !isOrderExpired(order)"
            @click="goToPay(order.id)"
            class="btn btn-primary"
          >
            去支付
          </button>
          <button
            v-if="order.status === 'PENDING' && !isOrderExpired(order)"
            @click="cancelOrder(order.id)"
            class="btn btn-cancel"
          >
            取消订单
          </button>
          <!-- 已支付订单 -->
          <button
            v-if="order.status === 'PAID'"
            @click="confirmDelivery(order.id)"
            class="btn btn-success"
          >
            确认收货
          </button>
          <!-- 已完成订单 -->
          <button
            v-if="order.status === 'DELIVERED' && !order.hasReviewed"
            @click="goToReview(order.id)"
            class="btn btn-warning"
          >
            ⭐ 评价订单
          </button>
          <button
            v-if="order.status === 'DELIVERED' && order.hasReviewed"
            class="btn btn-default"
            disabled
          >
            ✅ 已评价
          </button>
          <!-- 已过期或已取消订单 -->
          <button
            v-if="order.status === 'EXPIRED' || order.status === 'CANCELED' || (order.status === 'PENDING' && isOrderExpired(order))"
            class="btn btn-default"
            disabled
          >
            {{ order.status === 'CANCELED' ? '已取消' : '已过期' }}
          </button>
        </div>
      </div>
    </div>
    </div>

    <!-- 我卖出的订单标签页 -->
    <div v-show="currentTab === 'seller-orders'">
      <div v-if="sellerOrdersLoading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="sellerOrders.length === 0" class="empty-container">
        <div class="empty-icon">💰</div>
        <p>暂无卖出的订单</p>
        <router-link to="/" class="btn btn-primary">去发布商品</router-link>
      </div>

      <div v-else class="orders-list">
        <div v-for="order in sellerOrders" :key="order.id" class="order-card seller-order">
          <div class="order-header">
            <div class="order-info">
              <span class="order-number">订单号: {{ order.id }}</span>
              <span class="order-date">{{ formatDate(order.createdAt) }}</span>
              <span class="seller-badge">💰 卖家</span>
            </div>
            <div class="order-status" :class="getStatusClass(order.status, order)">
              {{ getStatusText(order.status, order) }}
            </div>
          </div>

          <div class="order-body">
            <div class="order-amount">
              <span class="label">订单金额：</span>
              <span class="amount">¥{{ order.totalAmount }}</span>
            </div>

            <div v-if="order.paymentMethod" class="order-payment">
              <span class="label">支付方式：</span>
              <span class="payment-badge">
                {{ getPaymentIcon(order.paymentMethod) }} {{ getPaymentText(order.paymentMethod) }}
              </span>
            </div>

            <div class="buyer-info">
              <span class="label">买家ID：</span>
              <span class="buyer-id">{{ order.userId }}</span>
            </div>
          </div>

          <div class="order-actions">
            <button
              v-if="order.status === 'DELIVERED'"
              class="btn btn-success"
              disabled
            >
              ✅ 交易完成
            </button>
            <button
              v-if="order.status === 'PAID'"
              class="btn btn-info"
              disabled
            >
              ⏳ 等待买家确认收货
            </button>
            <button
              v-if="order.status === 'PENDING'"
              class="btn btn-warning"
              disabled
            >
              ⏰ 等待买家付款
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 砍价活动列表标签页 -->
    <div v-show="currentTab === 'bargains'">
      <div v-if="bargainsLoading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="bargains.length === 0" class="empty-container">
        <div class="empty-icon">🔪</div>
        <p>暂无砍价活动</p>
        <router-link to="/" class="btn btn-primary">去砍价</router-link>
      </div>

      <div v-else class="bargains-list">
        <div v-for="bargain in bargains" :key="bargain.id" class="bargain-card">
          <div class="bargain-header">
            <div class="bargain-info">
              <span class="bargain-product">{{ bargain.productName }}</span>
              <span class="bargain-date">{{ formatDate(bargain.createdAt) }}</span>
            </div>
            <div class="bargain-status" :class="getBargainStatusClass(bargain.status)">
              {{ getBargainStatusText(bargain.status) }}
            </div>
          </div>

          <div class="bargain-body">
            <div class="bargain-image-container">
              <img
                v-if="bargain.productImage"
                :src="bargain.productImage"
                :alt="bargain.productName"
                class="bargain-product-image"
              />
              <div v-else class="bargain-image-placeholder">📷</div>
            </div>

            <div class="bargain-details">
              <div class="price-info">
                <div class="price-row">
                  <span class="label">原价：</span>
                  <span class="original-price">¥{{ bargain.originalPrice }}</span>
                </div>
                <div class="price-row">
                  <span class="label">当前价：</span>
                  <span class="current-price">¥{{ bargain.currentPrice }}</span>
                </div>
                <div class="price-row">
                  <span class="label">目标价：</span>
                  <span class="target-price">¥{{ bargain.targetPrice }}</span>
                </div>
              </div>

              <div class="bargain-progress">
                <div class="progress-bar-mini">
                  <div
                    class="progress-fill-mini"
                    :style="{ width: getBargainProgress(bargain) + '%' }"
                  ></div>
                </div>
                <span class="progress-text-mini">已砍 {{ getBargainProgress(bargain).toFixed(0) }}%</span>
              </div>

              <div class="bargain-help-count">
                {{ bargain.helpCount || 0 }} 人助力
              </div>

              <div v-if="bargain.status === 'ACTIVE' && bargain.expireTime" class="bargain-time">
                ⏰ {{ getTimeRemaining(bargain.expireTime) }}
              </div>
            </div>
          </div>

          <div class="bargain-actions">
            <!-- 进行中 -->
            <button
              v-if="bargain.status === 'ACTIVE'"
              @click="goToBargain(bargain.id)"
              class="btn btn-primary"
            >
              继续砍价
            </button>
            <!-- 成功 -->
            <button
              v-if="bargain.status === 'SUCCESS'"
              @click="goToBargain(bargain.id)"
              class="btn btn-success"
            >
              去购买
            </button>
            <!-- 已完成/已过期/已失败 -->
            <button
              v-if="bargain.status === 'COMPLETED' || bargain.status === 'EXPIRED' || bargain.status === 'FAILED'"
              @click="goToBargain(bargain.id)"
              class="btn btn-default"
            >
              查看详情
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const currentTab = ref('orders'); // 当前标签页
const orders = ref([]);
const loading = ref(true);
const sellerOrders = ref([]);
const sellerOrdersLoading = ref(false);
const bargains = ref([]);
const bargainsLoading = ref(false);

// 监听标签页切换
watch(currentTab, (newTab) => {
  if (newTab === 'seller-orders' && sellerOrders.value.length === 0) {
    fetchSellerOrders();
  } else if (newTab === 'bargains' && bargains.value.length === 0) {
    fetchBargains();
  }
});

const paymentMethods = {
  alipay: { icon: '💙', text: '支付宝' },
  wechat: { icon: '💚', text: '微信支付' },
  bank: { icon: '💳', text: '银行卡' },
  balance: { icon: '💰', text: '余额支付' }
};

const fetchOrders = async () => {
  try {
    loading.value = true;
    const response = await axios.get('/api/orders');
    orders.value = response.data;

    // 为每个已完成的订单检查是否已评价
    for (const order of orders.value) {
      if (order.status === 'DELIVERED') {
        await checkReviewStatus(order);
      }
    }
  } catch (error) {
    console.error('获取订单失败:', error);
    alert('获取订单失败');
  } finally {
    loading.value = false;
  }
};

// 检查订单是否已评价
const checkReviewStatus = async (order) => {
  try {
    const response = await axios.get(`/api/reviews/check/${order.id}`);
    order.hasReviewed = response.data.hasReviewed;
  } catch (error) {
    console.error('检查评价状态失败:', error);
    order.hasReviewed = false;
  }
};

// 跳转到评价页面
const goToReview = (orderId) => {
  router.push(`/review/${orderId}`);
};

// 获取卖家订单列表
const fetchSellerOrders = async () => {
  try {
    sellerOrdersLoading.value = true;
    const response = await axios.get('/api/orders/seller');
    sellerOrders.value = response.data;
  } catch (error) {
    console.error('获取卖家订单失败:', error);
    alert('获取卖家订单失败');
  } finally {
    sellerOrdersLoading.value = false;
  }
};

// 获取砍价活动列表
const fetchBargains = async () => {
  try {
    bargainsLoading.value = true;
    const response = await axios.get('/api/bargain/my-bargains');
    bargains.value = response.data;
  } catch (error) {
    console.error('获取砍价活动失败:', error);
    alert('获取砍价活动失败');
  } finally {
    bargainsLoading.value = false;
  }
};

// 跳转到砍价详情页
const goToBargain = (bargainId) => {
  router.push(`/bargain/${bargainId}`);
};

// 获取砍价进度百分比
const getBargainProgress = (bargain) => {
  const total = bargain.originalPrice - bargain.targetPrice;
  const current = bargain.originalPrice - bargain.currentPrice;
  return (current / total) * 100;
};

// 获取砍价状态样式类
const getBargainStatusClass = (status) => {
  const statusMap = {
    'ACTIVE': 'status-active',
    'SUCCESS': 'status-success',
    'COMPLETED': 'status-completed',
    'EXPIRED': 'status-expired',
    'FAILED': 'status-failed',
    'ABANDONED': 'status-abandoned'
  };
  return statusMap[status] || '';
};

// 获取砍价状态文本
const getBargainStatusText = (status) => {
  const statusMap = {
    'ACTIVE': '进行中',
    'SUCCESS': '砍价成功',
    'COMPLETED': '已购买',
    'EXPIRED': '已过期',
    'FAILED': '已失败',
    'ABANDONED': '已放弃'
  };
  return statusMap[status] || status;
};

// 获取剩余时间
const getTimeRemaining = (expireTime) => {
  const now = new Date().getTime();
  const expire = new Date(expireTime).getTime();
  const distance = expire - now;

  if (distance < 0) {
    return '已过期';
  }

  const hours = Math.floor(distance / (1000 * 60 * 60));
  const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));

  if (hours > 0) {
    return `剩余 ${hours}小时 ${minutes}分钟`;
  } else {
    return `剩余 ${minutes}分钟`;
  }
};

// 判断订单是否已过期
const isOrderExpired = (order) => {
  if (!order.expireTime) return false;
  const now = new Date().getTime();
  const expireTime = new Date(order.expireTime).getTime();
  return now >= expireTime;
};

const goToPay = (orderId) => {
  router.push(`/payment/${orderId}`);
};

const cancelOrder = async (orderId) => {
  if (!confirm('确定要取消该订单吗？')) {
    return;
  }

  try {
    await axios.post(`/api/orders/${orderId}/cancel`);
    alert('订单已取消');
    fetchOrders();
  } catch (error) {
    console.error('取消订单失败:', error);
    alert('取消订单失败');
  }
};

const confirmDelivery = async (orderId) => {
  if (!confirm('确认已收到商品吗？')) {
    return;
  }

  try {
    await axios.post(`/api/orders/${orderId}/confirm`);
    alert('确认收货成功');
    fetchOrders();
  } catch (error) {
    console.error('确认收货失败:', error);
    alert('确认收货失败');
  }
};

const getStatusClass = (status, order) => {
  // 如果是待支付订单，检查是否已过期
  if (status === 'PENDING' && order && isOrderExpired(order)) {
    return 'status-expired';
  }

  const statusMap = {
    'PENDING': 'status-pending',
    'PAID': 'status-paid',
    'DELIVERED': 'status-delivered',
    'CANCELED': 'status-canceled',
    'EXPIRED': 'status-expired'
  };
  return statusMap[status] || '';
};

const getStatusText = (status, order) => {
  // 如果是待支付订单，检查是否已过期
  if (status === 'PENDING' && order && isOrderExpired(order)) {
    return '已过期';
  }

  const statusMap = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'DELIVERED': '已完成',
    'CANCELED': '已取消',
    'EXPIRED': '已过期'
  };
  return statusMap[status] || status;
};

const getPaymentIcon = (method) => {
  return paymentMethods[method]?.icon || '💳';
};

const getPaymentText = (method) => {
  return paymentMethods[method]?.text || method;
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const goToHome = () => {
  router.push('/');
};

onMounted(() => {
  fetchOrders();
  fetchBargains();
});
</script>

<style scoped>
.order-history-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
}

.btn-back-home {
  position: absolute;
  left: 0;
  top: 0;
  padding: 12px 24px;
  background: linear-gradient(135deg, #36d1dc 0%, #5b86e5 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(54, 209, 220, 0.3);
}

.btn-back-home:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(54, 209, 220, 0.5);
}

.page-header h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 16px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-container {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-container p {
  color: #666;
  font-size: 18px;
  margin-bottom: 30px;
}

/* 标签页导航 */
.tabs-nav {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 0;
}

.tab-button {
  padding: 12px 30px;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 16px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  bottom: -2px;
}

.tab-button:hover {
  color: #667eea;
}

.tab-button.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
}

/* 砍价活动列表 */
.bargains-list {
  display: grid;
  gap: 20px;
}

.bargain-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.bargain-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.bargain-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.bargain-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.bargain-product {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.bargain-date {
  font-size: 13px;
  color: #999;
}

.bargain-status {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.status-active {
  background: #e3f2fd;
  color: #2196F3;
}

.status-success {
  background: #e8f5e9;
  color: #4CAF50;
}

.status-completed {
  background: #f3e5f5;
  color: #9c27b0;
}

.status-expired {
  background: #fff3e0;
  color: #ff9800;
}

.status-failed {
  background: #ffebee;
  color: #f44336;
}

.status-abandoned {
  background: #f5f5f5;
  color: #757575;
}

.bargain-body {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.bargain-image-container {
  flex-shrink: 0;
}

.bargain-product-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.bargain-image-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 48px;
  opacity: 0.3;
}

.bargain-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.price-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.price-row .label {
  font-size: 14px;
  color: #666;
  min-width: 60px;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.current-price {
  font-size: 20px;
  font-weight: bold;
  color: #e74c3c;
}

.target-price {
  font-size: 16px;
  color: #4CAF50;
  font-weight: 600;
}

.bargain-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-bar-mini {
  flex: 1;
  height: 10px;
  background: #f0f0f0;
  border-radius: 5px;
  overflow: hidden;
}

.progress-fill-mini {
  height: 100%;
  background: linear-gradient(90deg, #ff6b6b 0%, #4CAF50 100%);
  transition: width 0.3s ease;
  border-radius: 5px;
}

.progress-text-mini {
  font-size: 14px;
  color: #666;
  font-weight: 600;
  white-space: nowrap;
}

.bargain-help-count {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.bargain-time {
  font-size: 13px;
  color: #ff9800;
  font-weight: 500;
}

.bargain-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.empty-container p {
  color: #666;
  font-size: 16px;
  margin-bottom: 20px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.order-number {
  font-weight: 500;
  color: #333;
}

.order-date {
  font-size: 13px;
  color: #999;
}

.order-status {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.status-pending {
  background: #fff3e0;
  color: #f57c00;
}

.status-paid {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-delivered {
  background: #e3f2fd;
  color: #1565c0;
}

.status-canceled {
  background: #fce4ec;
  color: #c2185b;
}

.status-expired {
  background: #f5f5f5;
  color: #999;
}

.order-body {
  padding: 20px;
}

.order-amount,
.order-payment {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.label {
  color: #666;
  margin-right: 8px;
}

.amount {
  font-size: 20px;
  font-weight: bold;
  color: #ff4d4f;
}

.payment-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: #f0f0f0;
  border-radius: 12px;
  font-size: 14px;
  color: #333;
}

.order-actions {
  display: flex;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #e9ecef;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-cancel {
  background: white;
  color: #666;
  border: 1px solid #ddd;
}

.btn-cancel:hover {
  background: #f5f5f5;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #218838;
}

.btn-default {
  background: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .order-actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }
}
</style>

