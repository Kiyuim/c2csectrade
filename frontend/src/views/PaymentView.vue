<template>
  <div class="payment-page">
    <div class="payment-container">
      <div class="payment-header">
        <h1>💳 收银台</h1>
        <p class="order-info">订单号: {{ orderId }}</p>
      </div>

      <!-- 订单信息 -->
      <div class="order-summary" v-if="order">
        <h3>订单摘要</h3>

        <!-- 倒计时提示 -->
        <div class="countdown-alert" :class="{ warning: remainingMinutes < 5 }">
          <span class="countdown-icon">⏰</span>
          <span v-if="!isExpired">请在 <strong>{{ countdownText }}</strong> 内完成支付</span>
          <span v-else class="expired-text">订单已过期</span>
        </div>


        <div class="summary-item">
          <span>商品总额</span>
          <span class="amount">¥{{ order.totalAmount }}</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item total">
          <span>应付金额</span>
          <span class="total-amount">¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <!-- 支付方式选择 -->
      <div class="payment-methods">
        <h3>选择支付方式</h3>
        <div class="methods-list">
          <div
            v-for="method in paymentMethods"
            :key="method.id"
            class="method-item"
            :class="{
              active: selectedMethod === method.id,
              disabled: method.disabled
            }"
            @click="!method.disabled && selectMethod(method.id)"
          >
            <div class="method-icon">{{ method.icon }}</div>
            <div class="method-info">
              <div class="method-name">
                {{ method.name }}
                <span v-if="method.disabled" class="insufficient-label">（余额不足）</span>
              </div>
              <div class="method-desc" :class="{ warning: method.disabled }">
                {{ method.description }}
              </div>
            </div>
            <div class="method-radio">
              <div class="radio-dot" v-if="selectedMethod === method.id"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 支付按钮 -->
      <div class="payment-actions">
        <button class="btn btn-home" @click="goHome">🏠 返回主页</button>
        <button class="btn btn-cancel" @click="cancelOrder">取消订单</button>
        <button
          class="btn btn-pay"
          @click="confirmPayment"
          :disabled="!selectedMethod || processing"
        >
          <span v-if="!processing">立即支付 ¥{{ order?.totalAmount || 0 }}</span>
          <span v-else>处理中...</span>
        </button>
      </div>

      <!-- 支付密码弹窗 -->
      <div v-if="showPasswordDialog" class="password-overlay" @click="closePasswordDialog">
        <div class="password-dialog" @click.stop>
          <div class="dialog-header">
            <h3>请输入支付密码</h3>
            <button @click="closePasswordDialog" class="close-btn">×</button>
          </div>
          <div class="dialog-body">
            <div class="payment-info">
              <div class="payment-method-icon">{{ currentMethodIcon }}</div>
              <div class="payment-amount">¥{{ order?.totalAmount || 0 }}</div>
            </div>
            <div class="password-input-container">
              <input
                v-for="i in 6"
                :key="i"
                type="password"
                maxlength="1"
                class="password-digit"
                :ref="el => passwordInputs[i - 1] = el"
                v-model="passwordDigits[i - 1]"
                @input="handlePasswordInput(i - 1)"
                @keydown="handleKeyDown($event, i - 1)"
              />
            </div>
            <p class="password-tip">为了您的资金安全，请输入6位支付密码</p>
          </div>
          <div class="dialog-actions">
            <button class="btn btn-cancel" @click="closePasswordDialog">取消</button>
            <button
              class="btn btn-confirm"
              @click="submitPayment"
              :disabled="password.length !== 6 || processing"
            >
              确认支付
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const route = useRoute();
const orderId = ref(route.params.orderId || route.query.orderId);
const order = ref(null);
const selectedMethod = ref(null);
const processing = ref(false);
const showPasswordDialog = ref(false);
const passwordDigits = ref(['', '', '', '', '', '']);
const passwordInputs = ref([]);
const remainingTime = ref(0);
const isExpired = ref(false);
const hasPaymentPassword = ref(null); // 缓存支付密码状态
const userBalance = ref(0); // 用户余额
let countdownTimer = null;

const paymentMethods = computed(() => [
  {
    id: 'alipay',
    name: '支付宝',
    icon: '💙',
    description: '推荐使用支付宝快捷支付'
  },
  {
    id: 'wechat',
    name: '微信支付',
    icon: '💚',
    description: '使用微信安全支付'
  },
  {
    id: 'bank',
    name: '银行卡',
    icon: '💳',
    description: '储蓄卡、信用卡均可'
  },
  {
    id: 'balance',
    name: '余额支付',
    icon: '💰',
    description: `当前余额：¥${userBalance.value.toFixed(2)}`,
    disabled: userBalance.value < (order.value?.totalAmount || 0)
  }
]);

const password = computed(() => passwordDigits.value.join(''));

const currentMethodIcon = computed(() => {
  const method = paymentMethods.value.find(m => m.id === selectedMethod.value);
  return method ? method.icon : '💳';
});

const fetchOrderDetails = async () => {
  try {
    const response = await axios.get(`/api/orders/${orderId.value}`);
    order.value = response.data;

    // 检查订单状态
    if (order.value.status === 'EXPIRED' || order.value.status === 'CANCELED') {
      alert(`该订单已${order.value.status === 'EXPIRED' ? '过期' : '取消'}`);
      router.push('/order-history');
      return;
    }

    if (order.value.status !== 'PENDING') {
      alert('该订单不可支付');
      router.push('/order-history');
      return;
    }

    // 检查订单是否已经过期（客户端检查）
    if (order.value.expireTime) {
      const now = new Date().getTime();
      const expireTime = new Date(order.value.expireTime).getTime();
      if (now >= expireTime) {
        isExpired.value = true;
        alert('订单已过期，商品已恢复到购物车');
        router.push({
          path: '/payment-result',
          query: {
            orderId: orderId.value,
            status: 'failed',
            message: '订单支付超时已自动关闭'
          }
        });
        return;
      }
    }

    // 启动倒计时
    startCountdown();

    // 获取用户余额
    await fetchUserBalance();
  } catch (error) {
    console.error('获取订单详情失败:', error);
    alert('获取订单详情失败');
    router.push('/order-history');
  }
};

// 获取用户余额
const fetchUserBalance = async () => {
  try {
    const response = await axios.get('/api/users/me');
    userBalance.value = response.data.balance || 0;
  } catch (error) {
    console.error('获取用户余额失败:', error);
    userBalance.value = 0;
  }
};

const selectMethod = (methodId) => {
  selectedMethod.value = methodId;
};

const confirmPayment = async () => {
  if (!selectedMethod.value) {
    alert('请选择支付方式');
    return;
  }

  // 检查用户是否已设置支付密码（每次都重新检查，避免缓存问题）
  try {
    const response = await axios.get('/api/users/payment-password/check', {
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    });
    hasPaymentPassword.value = response.data.hasPaymentPassword;

    if (!hasPaymentPassword.value) {
      if (confirm('您还未设置支付密码，是否现在设置？')) {
        // 保存当前路由信息，以便设置完成后返回
        sessionStorage.setItem('returnToPayment', orderId.value);
        router.push('/payment-password/setup');
      }
      return;
    }
  } catch (error) {
    console.error('检查支付密码失败:', error);
    alert('检查支付密码状态失败，请重试');
    return;
  }

  showPasswordDialog.value = true;
  // 自动聚焦第一个输入框
  setTimeout(() => {
    if (passwordInputs.value[0]) {
      passwordInputs.value[0].focus();
    }
  }, 100);
};

const closePasswordDialog = () => {
  showPasswordDialog.value = false;
  passwordDigits.value = ['', '', '', '', '', ''];
};

const handlePasswordInput = (index) => {
  // 如果输入了数字，自动跳到下一个输入框
  if (passwordDigits.value[index] && index < 5) {
    passwordInputs.value[index + 1]?.focus();
  }
};

const handleKeyDown = (event, index) => {
  // 按退格键时跳到上一个输入框
  if (event.key === 'Backspace' && !passwordDigits.value[index] && index > 0) {
    passwordInputs.value[index - 1]?.focus();
  }
};

const remainingMinutes = computed(() => Math.floor(remainingTime.value / 60));

const countdownText = computed(() => {
  if (remainingTime.value <= 0) return '0分0秒';
  const minutes = Math.floor(remainingTime.value / 60);
  const seconds = remainingTime.value % 60;
  return `${minutes}分${seconds}秒`;
});

const startCountdown = () => {
  if (!order.value || !order.value.expireTime) return;

  const updateCountdown = () => {
    const now = new Date().getTime();
    const expireTime = new Date(order.value.expireTime).getTime();
    const diff = Math.floor((expireTime - now) / 1000);

    if (diff <= 0) {
      remainingTime.value = 0;
      isExpired.value = true;
      clearInterval(countdownTimer);

      // 订单已过期，跳转到失败页面
      alert('订单已过期，商品已恢复到购物车');
      router.push({
        path: '/payment-result',
        query: {
          orderId: orderId.value,
          status: 'failed',
          message: '订单支付超时已自动关闭'
        }
      });
    } else {
      remainingTime.value = diff;
    }
  };

  updateCountdown();
  countdownTimer = setInterval(updateCountdown, 1000);
};

const submitPayment = async () => {
  if (password.value.length !== 6) {
    alert('请输入完整的支付密码');
    return;
  }

  processing.value = true;

  try {
    // 模拟支付处理延迟
    await new Promise(resolve => setTimeout(resolve, 1500));

    // 调用支付接口
    await axios.post(`/api/orders/${orderId.value}/pay`, {
      paymentMethod: selectedMethod.value,
      password: password.value
    });

    // 支付成功，跳转到成功页面
    closePasswordDialog();
    router.push({
      path: '/payment-result',
      query: {
        orderId: orderId.value,
        status: 'success',
        amount: order.value.totalAmount,
        method: selectedMethod.value
      }
    });
  } catch (error) {
    console.error('支付失败:', error);
    // 支付失败，跳转到失败页面
    router.push({
      path: '/payment-result',
      query: {
        orderId: orderId.value,
        status: 'failed',
        message: error.response?.data?.message || '支付失败，请重试'
      }
    });
  } finally {
    processing.value = false;
    closePasswordDialog();
  }
};

const goHome = () => {
  router.push('/');
};

const cancelOrder = async () => {
  if (!confirm('确定要取消订单吗？订单将被关闭，商品将恢复到购物车。')) {
    return;
  }

  try {
    await axios.post(`/api/orders/${orderId.value}/cancel`);
    alert('订单已取消');
    router.push('/');
  } catch (error) {
    console.error('取消订单失败:', error);
    alert('取消订单失败');
  }
};

onMounted(() => {
  if (!orderId.value) {
    alert('订单信息错误');
    router.push('/order-history');
    return;
  }
  fetchOrderDetails();
});

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});
</script>

<style scoped>
.payment-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.payment-container {
  max-width: 800px;
  margin: 0 auto;
}

.payment-header {
  text-align: center;
  color: white;
  margin-bottom: 30px;
}

.payment-header h1 {
  font-size: 36px;
  margin-bottom: 10px;
}

.order-info {
  font-size: 14px;
  opacity: 0.9;
}

.order-summary {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.order-summary h3 {
  font-size: 18px;
  margin-bottom: 16px;
  color: #333;
}

.countdown-alert {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.countdown-alert.warning {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
}

.countdown-icon {
  font-size: 18px;
}

.expired-text {
  font-weight: bold;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  font-size: 15px;
}

.summary-item .amount {
  color: #666;
}

.summary-item.total {
  font-size: 18px;
  font-weight: bold;
}

.summary-item.total .total-amount {
  color: #ff4d4f;
  font-size: 24px;
}

.summary-divider {
  height: 1px;
  background: #eee;
  margin: 12px 0;
}

.payment-methods {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.payment-methods h3 {
  font-size: 18px;
  margin-bottom: 16px;
  color: #333;
}

.methods-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.method-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.method-item:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.method-item.active {
  border-color: #667eea;
  background: #f8f9ff;
}

.method-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f5f5;
}

.method-item.disabled:hover {
  border-color: #e8e8e8;
  background: #f5f5f5;
}

.method-icon {
  font-size: 32px;
  margin-right: 16px;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.insufficient-label {
  color: #ff4d4f;
  font-size: 12px;
  font-weight: normal;
  margin-left: 8px;
}

.method-desc {
  font-size: 13px;
  color: #999;
}

.method-desc.warning {
  color: #ff4d4f;
  font-weight: 500;
}

.method-radio {
  width: 20px;
  height: 20px;
  border: 2px solid #ddd;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.method-item.active .method-radio {
  border-color: #667eea;
}

.radio-dot {
  width: 10px;
  height: 10px;
  background: #667eea;
  border-radius: 50%;
}

.payment-actions {
  display: flex;
  gap: 16px;
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

.btn-home {
  background: linear-gradient(135deg, #36d1dc 0%, #5b86e5 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(54, 209, 220, 0.4);
}

.btn-home:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(54, 209, 220, 0.5);
}

.btn-cancel {
  background: white;
  color: #666;
  border: 2px solid #e8e8e8;
}

.btn-cancel:hover {
  background: #f5f5f5;
}

.btn-pay {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-pay:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.btn-pay:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 支付密码弹窗 */
.password-overlay {
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
}

.password-dialog {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #eee;
}

.dialog-header h3 {
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #666;
}

.dialog-body {
  padding: 32px 24px;
}

.payment-info {
  text-align: center;
  margin-bottom: 32px;
}

.payment-method-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.payment-amount {
  font-size: 32px;
  font-weight: bold;
  color: #ff4d4f;
}

.password-input-container {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.password-digit {
  width: 48px;
  height: 56px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  transition: all 0.3s;
}

.password-digit:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.password-tip {
  text-align: center;
  font-size: 13px;
  color: #999;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #eee;
}

.dialog-actions .btn {
  flex: 1;
  padding: 12px;
}

.btn-confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

