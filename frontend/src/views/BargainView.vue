<template>
  <div class="bargain-container">
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="bargainActivity" class="bargain-content">
      <!-- 返回按钮 -->
      <button class="btn-back" @click="goBack">← 返回</button>

      <!-- 商品信息卡片 -->
      <div class="product-card">
        <img
          v-if="product?.media?.[0]"
          :src="product.media[0].url"
          :alt="product.name"
          class="product-image"
        />
        <div class="product-placeholder" v-else>📷</div>
        <div class="product-info">
          <h2>{{ product?.name }}</h2>
          <div class="original-price">原价：¥{{ bargainActivity.originalPrice }}</div>
        </div>
      </div>

      <!-- 砍价状态卡片 -->
      <div class="bargain-status-card">
        <div class="status-header">
          <h1 v-if="bargainActivity.status === 'SUCCESS'" class="status-success">
            🎉 砍价成功！
          </h1>
          <h1 v-else-if="bargainActivity.status === 'EXPIRED'" class="status-expired">
            ⏰ 活动已过期
          </h1>
          <h1 v-else-if="bargainActivity.status === 'FAILED'" class="status-failed">
            😢 砍价失败
          </h1>
          <h1 v-else-if="bargainActivity.status === 'COMPLETED'" class="status-completed">
            ✅ 已购买
          </h1>
          <h1 v-else class="status-ongoing">🔪 砍价进行中</h1>
        </div>

        <!-- 失败原因说明 -->
        <div v-if="bargainActivity.status === 'FAILED'" class="failure-reason">
          <p>😢 很遗憾，该商品已被其他用户购买或已下架</p>
        </div>

        <!-- 价格展示 -->
        <div class="price-section">
          <div class="current-price-label">当前价格</div>
          <div class="current-price">¥{{ bargainActivity.currentPrice }}</div>
          <div class="target-price">目标价：¥{{ bargainActivity.targetPrice }}</div>
        </div>

        <!-- 进度条 -->
        <div class="progress-section">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
          </div>
          <div class="progress-text">
            已砍 {{ progressPercentage.toFixed(1) }}%
            <span class="remaining">还差 ¥{{ remainingAmount }}</span>
          </div>
        </div>

        <!-- 倒计时 -->
        <div v-if="bargainActivity.status === 'ACTIVE'" class="countdown">
          <span class="countdown-icon">⏰</span>
          剩余时间：{{ countdown }}
          <div class="countdown-note">砍价活动限时24小时</div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <!-- 砍价成功 -->
          <button
            v-if="bargainActivity.status === 'SUCCESS'"
            class="btn-buy-success"
            @click="buyAtBargainPrice"
          >
            💰 立即购买（¥{{ bargainActivity.currentPrice }}）
          </button>

          <!-- 砍价进行中 -->
          <template v-else-if="bargainActivity.status === 'ACTIVE'">
            <!-- 如果是活动发起者 -->
            <div v-if="isOwner" class="owner-actions">
              <button class="btn-share" @click="shareBargain">
                📤 分享给好友
              </button>
              <button class="btn-abandon" @click="abandonAndBuy">
                💳 放弃砍价，直接购买（¥{{ bargainActivity.currentPrice }}）
              </button>
            </div>
            <!-- 如果是助力者 -->
            <div v-else class="helper-actions">
              <button
                v-if="!hasHelped"
                class="btn-help"
                @click="helpBargain"
                :disabled="helping"
              >
                {{ helping ? '砍价中...' : '🎁 帮TA砍一刀' }}
              </button>
              <div v-else class="already-helped">
                ✅ 您已经帮忙砍过了
              </div>
            </div>
          </template>

          <!-- 已过期 -->
          <button
            v-else-if="bargainActivity.status === 'EXPIRED'"
            class="btn-expired"
            @click="goToProduct"
          >
            查看商品详情
          </button>

          <!-- 砍价失败（商品被其他人买走或下架） -->
          <button
            v-else-if="bargainActivity.status === 'FAILED'"
            class="btn-failed"
            @click="goBack"
          >
            返回首页
          </button>

          <!-- 已完成购买 -->
          <button
            v-else-if="bargainActivity.status === 'COMPLETED'"
            class="btn-completed"
            @click="goBack"
          >
            返回首页
          </button>
        </div>
      </div>

      <!-- 助力列表 -->
      <div class="help-list-card">
        <h3>💪 助力榜（{{ helpList.length }}人）</h3>
        <div v-if="helpList.length === 0" class="empty-help">
          还没有人助力，快去邀请好友吧！
        </div>
        <div v-else class="help-list">
          <div
            v-for="help in helpList"
            :key="help.id"
            class="help-item"
          >
            <div class="helper-avatar">{{ help.helperName.charAt(0) }}</div>
            <div class="helper-info">
              <div class="helper-name">{{ help.helperName }}</div>
              <div class="helper-time">{{ formatTime(help.createdAt) }}</div>
            </div>
            <div class="cut-amount">-¥{{ help.cutAmount }}</div>
          </div>
        </div>
      </div>

      <!-- 分享弹窗 -->
      <div v-if="showShareModal" class="modal-overlay" @click="showShareModal = false">
        <div class="modal-content" @click.stop>
          <h3>分享砍价链接</h3>
          <div class="share-link-container">
            <input
              ref="shareLinkInput"
              v-model="shareLinkUrl"
              readonly
              class="share-link-input"
            />
            <button class="btn-copy" @click="copyLink">
              {{ copied ? '✅ 已复制' : '📋 复制' }}
            </button>
          </div>
          <p class="share-tip">将链接分享给好友，让他们帮你砍价吧！</p>
          <button class="btn-close" @click="showShareModal = false">关闭</button>
        </div>
      </div>
    </div>

    <div v-else class="error-container">
      <h2>😢 砍价活动不存在</h2>
      <button class="btn-back" @click="goBack">返回主页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import axios from 'axios';
import { toast } from '@/services/toast';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const bargainActivity = ref(null);
const product = ref(null);
const helpList = ref([]);
const countdown = ref('');
const helping = ref(false);
const showShareModal = ref(false);
const shareLinkUrl = ref('');
const copied = ref(false);
const shareLinkInput = ref(null);

let countdownInterval = null;

// 是否是活动发起者
const isOwner = computed(() => {
  if (!authStore.user || !bargainActivity.value) return false;
  return authStore.user.id === bargainActivity.value.userId;
});

// 是否已经助力过
const hasHelped = computed(() => {
  if (!authStore.user || !helpList.value) return false;
  return helpList.value.some(help => help.helperId === authStore.user.id);
});

// 进度百分比
const progressPercentage = computed(() => {
  if (!bargainActivity.value) return 0;
  const total = bargainActivity.value.originalPrice - bargainActivity.value.targetPrice;
  const current = bargainActivity.value.originalPrice - bargainActivity.value.currentPrice;
  return (current / total) * 100;
});

// 还需砍多少
const remainingAmount = computed(() => {
  if (!bargainActivity.value) return 0;
  const remaining = bargainActivity.value.currentPrice - bargainActivity.value.targetPrice;
  return Math.max(0, remaining).toFixed(2);
});

// 获取砍价活动详情
const fetchBargainActivity = async () => {
  try {
    const response = await axios.get(`/api/bargain/${route.params.id}`);
    bargainActivity.value = response.data.activity;
    helpList.value = response.data.helpList || [];

    // 获取商品信息
    if (bargainActivity.value.productId) {
      const productResponse = await axios.get(`/api/products/${bargainActivity.value.productId}`);
      product.value = productResponse.data;
    }

    // 启动倒计时
    if (bargainActivity.value.status === 'ACTIVE') {
      startCountdown();
    }
  } catch (error) {
    console.error('获取砍价活动失败:', error);
    toast('获取砍价活动失败', 'error');
  } finally {
    loading.value = false;
  }
};

// 开始倒计时
const startCountdown = () => {
  const updateCountdown = () => {
    if (!bargainActivity.value) return;

    const now = new Date().getTime();
    const expireTime = new Date(bargainActivity.value.expireTime).getTime();
    const distance = expireTime - now;

    if (distance < 0) {
      countdown.value = '已过期';
      bargainActivity.value.status = 'EXPIRED';
      clearInterval(countdownInterval);
      return;
    }

    const hours = Math.floor(distance / (1000 * 60 * 60));
    const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((distance % (1000 * 60)) / 1000);

    countdown.value = `${hours}小时 ${minutes}分钟 ${seconds}秒`;
  };

  updateCountdown();
  countdownInterval = setInterval(updateCountdown, 1000);
};

// 帮助砍价
const helpBargain = async () => {
  if (!authStore.isLoggedIn) {
    toast('请先登录', 'warning');
    router.push('/login');
    return;
  }

  if (isOwner.value) {
    toast('不能为自己的砍价活动助力', 'warning');
    return;
  }

  if (hasHelped.value) {
    toast('您已经帮忙砍过了', 'warning');
    return;
  }

  helping.value = true;

  try {
    const response = await axios.post(`/api/bargain/help/${route.params.id}`);
    const result = response.data;

    toast(`成功砍掉 ¥${result.help.cutAmount}！`, 'success');

    // 更新数据
    bargainActivity.value = result.activity;
    helpList.value.unshift(result.help);

    // 检查是否砍价成功
    if (result.activity.status === 'SUCCESS') {
      toast('🎉 恭喜！砍价成功了！', 'success');
    }
  } catch (error) {
    console.error('助力失败:', error);
    if (error.response?.data?.message) {
      toast(error.response.data.message, 'error');
    } else {
      toast('助力失败，请重试', 'error');
    }
  } finally {
    helping.value = false;
  }
};

// 放弃砍价，以当前价格购买
const abandonAndBuy = async () => {
  if (!confirm(`确定要放弃砍价，以当前价格 ¥${bargainActivity.value.currentPrice} 购买吗？`)) {
    return;
  }

  try {
    // 调用放弃砍价并购买的API
    const response = await axios.post(`/api/bargain/abandon-and-buy/${route.params.id}`);
    const order = response.data;

    toast('订单创建成功', 'success');
    // 跳转到支付页面
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

// 以砍价成功的价格购买
const buyAtBargainPrice = async () => {
  try {
    // 直接使用砍价价格创建订单
    const response = await axios.post(`/api/bargain/buy/${route.params.id}`);
    const order = response.data;

    toast('订单创建成功', 'success');
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

// 分享链接
const shareBargain = () => {
  shareLinkUrl.value = window.location.href;
  showShareModal.value = true;
  copied.value = false;
};

// 复制链接
const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLinkUrl.value);
    copied.value = true;
    toast('链接已复制', 'success');
    setTimeout(() => {
      copied.value = false;
    }, 2000);
  } catch (error) {
    // 如果clipboard API不可用，使用传统方法
    if (shareLinkInput.value) {
      shareLinkInput.value.select();
      document.execCommand('copy');
      copied.value = true;
      toast('链接已复制', 'success');
      setTimeout(() => {
        copied.value = false;
      }, 2000);
    }
  }
};

// 格式化时间
const formatTime = (time) => {
  const date = new Date(time);
  const now = new Date();
  const diff = now - date;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;

  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};

// 返回
const goBack = () => {
  router.push('/');
};

// 查看商品详情
const goToProduct = () => {
  if (product.value) {
    router.push(`/product/${product.value.id}`);
  } else {
    router.push('/');
  }
};

onMounted(() => {
  fetchBargainActivity();
});

onUnmounted(() => {
  if (countdownInterval) {
    clearInterval(countdownInterval);
  }
});
</script>

<style scoped>
.bargain-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.btn-back {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 16px;
  margin-bottom: 20px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-5px);
}

/* 商品卡片 */
.product-card {
  background: white;
  border-radius: 15px;
  padding: 20px;
  display: flex;
  gap: 20px;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.product-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 10px;
}

.product-placeholder {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 10px;
  font-size: 40px;
}

.product-info h2 {
  margin: 0 0 10px 0;
  font-size: 20px;
  color: #333;
}

.original-price {
  color: #999;
  text-decoration: line-through;
  font-size: 14px;
}

/* 砍价状态卡片 */
.bargain-status-card {
  background: white;
  border-radius: 15px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.status-header h1 {
  margin: 0 0 20px 0;
  text-align: center;
  font-size: 28px;
}

.status-success {
  color: #4CAF50;
}

.status-expired {
  color: #ff9800;
}

.status-failed {
  color: #f44336;
}

.status-completed {
  color: #2196F3;
}

.status-ongoing {
  color: #2196F3;
}

.failure-reason {
  text-align: center;
  padding: 15px;
  background: #ffebee;
  border-radius: 10px;
  margin-bottom: 20px;
  color: #c62828;
}

/* 价格部分 */
.price-section {
  text-align: center;
  margin-bottom: 30px;
}

.current-price-label {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
}

.current-price {
  font-size: 48px;
  font-weight: bold;
  color: #e74c3c;
  margin-bottom: 10px;
}

.target-price {
  color: #999;
  font-size: 16px;
}

/* 进度条 */
.progress-section {
  margin-bottom: 20px;
}

.progress-bar {
  height: 20px;
  background: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff6b6b 0%, #4CAF50 100%);
  transition: width 0.5s ease;
  border-radius: 10px;
}

.progress-text {
  text-align: center;
  color: #666;
  font-size: 14px;
}

.remaining {
  margin-left: 10px;
  color: #e74c3c;
  font-weight: bold;
}

/* 倒计时 */
.countdown {
  text-align: center;
  padding: 15px;
  background: #fff3cd;
  border-radius: 10px;
  margin-bottom: 20px;
  font-size: 16px;
  color: #856404;
}

.countdown-icon {
  margin-right: 5px;
}

.countdown-note {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

/* 操作按钮 */
.action-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.owner-actions,
.helper-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.btn-buy-success,
.btn-help,
.btn-share,
.btn-abandon,
.btn-expired,
.btn-failed,
.btn-completed {
  padding: 15px 30px;
  border: none;
  border-radius: 10px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-buy-success {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4);
}

.btn-buy-success:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(76, 175, 80, 0.5);
}

.btn-help {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
}

.btn-help:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.5);
}

.btn-help:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-share {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-share:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.btn-abandon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(245, 87, 108, 0.4);
}

.btn-abandon:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 87, 108, 0.5);
}

.btn-expired,
.btn-failed,
.btn-completed {
  background: #6c757d;
  color: white;
}

.btn-expired:hover,
.btn-failed:hover,
.btn-completed:hover {
  background: #5a6268;
}

.already-helped {
  text-align: center;
  padding: 15px;
  background: #d4edda;
  color: #155724;
  border-radius: 10px;
  font-size: 16px;
}

/* 助力列表 */
.help-list-card {
  background: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.help-list-card h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #333;
}

.empty-help {
  text-align: center;
  padding: 30px;
  color: #999;
}

.help-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.help-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.help-item:hover {
  background: #e9ecef;
  transform: translateX(5px);
}

.helper-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  margin-right: 15px;
}

.helper-info {
  flex: 1;
}

.helper-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 3px;
}

.helper-time {
  font-size: 12px;
  color: #999;
}

.cut-amount {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
}

/* 分享弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 15px;
  padding: 30px;
  max-width: 500px;
  width: 100%;
}

.modal-content h3 {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #333;
  text-align: center;
}

.share-link-container {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.share-link-input {
  flex: 1;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
}

.btn-copy {
  padding: 12px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.3s ease;
}

.btn-copy:hover {
  background: #0056b3;
}

.share-tip {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.btn-close {
  width: 100%;
  padding: 12px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
}

.btn-close:hover {
  background: #5a6268;
}

/* 加载状态 */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: white;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(255, 255, 255, 0.3);
  border-top: 5px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container h2 {
  color: white;
  margin-bottom: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .bargain-container {
    padding: 10px;
  }

  .product-card {
    flex-direction: column;
    text-align: center;
  }

  .current-price {
    font-size: 36px;
  }

  .btn-buy-success,
  .btn-help,
  .btn-share,
  .btn-abandon {
    font-size: 16px;
  }
}
</style>

