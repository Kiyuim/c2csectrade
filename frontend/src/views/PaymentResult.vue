<template>
  <div class="result-page">
    <div class="result-container">
      <!-- 成功状态 -->
      <div v-if="status === 'success'" class="result-content success">
        <div class="result-icon">
          <div class="icon-circle">✓</div>
        </div>
        <h1 class="result-title">支付成功！</h1>
        <p class="result-message">您的订单已支付成功，商家将尽快为您发货</p>

        <div class="result-details">
          <div class="detail-item">
            <span class="label">订单号：</span>
            <span class="value">{{ orderId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">支付金额：</span>
            <span class="value amount">¥{{ amount }}</span>
          </div>
          <div class="detail-item">
            <span class="label">支付方式：</span>
            <span class="value">{{ paymentMethodName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">支付时间：</span>
            <span class="value">{{ currentTime }}</span>
          </div>
        </div>

        <div class="result-actions">
          <button class="btn btn-primary" @click="goToOrders">
            查看订单
          </button>
          <button class="btn btn-secondary" @click="goToHome">
            返回首页
          </button>
        </div>
      </div>

      <!-- 失败状态 -->
      <div v-else class="result-content failed">
        <div class="result-icon">
          <div class="icon-circle failed">✕</div>
        </div>
        <h1 class="result-title">支付失败</h1>
        <p class="result-message">{{ message || '支付过程中出现错误，请重试' }}</p>

        <div class="result-details">
          <div class="detail-item">
            <span class="label">订单号：</span>
            <span class="value">{{ orderId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">失败原因：</span>
            <span class="value error">{{ message || '未知错误' }}</span>
          </div>
        </div>

        <div class="result-actions">
          <button class="btn btn-primary" @click="retryPayment">
            重新支付
          </button>
          <button class="btn btn-secondary" @click="goToOrders">
            查看订单
          </button>
          <button class="btn btn-secondary" @click="goToHome">
            返回主页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const orderId = ref(route.query.orderId);
const status = ref(route.query.status || 'failed');
const amount = ref(route.query.amount);
const method = ref(route.query.method);
const message = ref(route.query.message);
const currentTime = ref('');

const paymentMethods = {
  alipay: '支付宝',
  wechat: '微信支付',
  bank: '银行卡',
  balance: '余额支付'
};

const paymentMethodName = computed(() => {
  return paymentMethods[method.value] || '未知';
});

const goToOrders = () => {
  router.push('/order-history');
};

const goToHome = () => {
  router.push('/');
};

const retryPayment = () => {
  router.push(`/payment/${orderId.value}`);
};

onMounted(() => {
  // 格式化当前时间
  const now = new Date();
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
});
</script>

<style scoped>
.result-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.result-container {
  max-width: 600px;
  width: 100%;
}

.result-content {
  background: white;
  border-radius: 20px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.result-icon {
  margin-bottom: 24px;
}

.icon-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: white;
  font-size: 60px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  animation: scaleIn 0.5s ease-out;
}

.icon-circle.failed {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
}

@keyframes scaleIn {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.result-title {
  font-size: 32px;
  color: #333;
  margin-bottom: 12px;
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

.result-message {
  font-size: 16px;
  color: #666;
  margin-bottom: 40px;
  animation: fadeInUp 0.6s ease-out 0.3s both;
}

@keyframes fadeInUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.result-details {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 32px;
  text-align: left;
  animation: fadeInUp 0.6s ease-out 0.4s both;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item .label {
  color: #999;
  font-size: 14px;
}

.detail-item .value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.detail-item .value.amount {
  color: #ff4d4f;
  font-size: 18px;
  font-weight: bold;
}

.detail-item .value.error {
  color: #ff4d4f;
}

.result-actions {
  display: flex;
  gap: 16px;
  animation: fadeInUp 0.6s ease-out 0.5s both;
}

.btn {
  flex: 1;
  padding: 16px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.btn-secondary {
  background: white;
  color: #666;
  border: 2px solid #e8e8e8;
}

.btn-secondary:hover {
  background: #f5f5f5;
  border-color: #d9d9d9;
}
</style>

