<template>
  <div class="floating-cart">
    <!-- 浮动按钮 -->
    <button @click="togglePanel" class="cart-button" :class="{ active: showPanel }">
      <span class="icon">🛒</span>
      <span class="badge" v-if="cartCount > 0">{{ cartCount }}</span>
    </button>

    <!-- 购物车面板 -->
    <transition name="slide-fade">
      <div v-if="showPanel" class="cart-panel">
        <div class="panel-header">
          <h3>🛒 购物车</h3>
          <button @click="closePanel" class="close-btn">✕</button>
        </div>

        <div v-if="loading" class="panel-loading">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="cartItems.length === 0" class="panel-empty">
          <div class="empty-icon">🛒</div>
          <p>购物车是空的</p>
        </div>

        <div v-else class="cart-content">
          <div class="cart-list">
            <div v-for="item in cartItems" :key="item.cartItemId" class="cart-item">
              <div class="item-image" @click="goToProduct(item.product.id)">
                <img v-if="getFirstImage(item.product)" :src="getFirstImage(item.product)" :alt="item.product.name" />
                <div v-else class="no-image">📷</div>
              </div>
              <div class="item-info" @click="goToProduct(item.product.id)">
                <h4 class="item-title">{{ item.product.name }}</h4>
                <div class="item-price">¥{{ item.product.price }}</div>
                <div class="item-quantity">
                  <button @click.stop="decreaseQuantity(item)" class="qty-btn">-</button>
                  <span class="qty-value">{{ item.quantity }}</span>
                  <button @click.stop="increaseQuantity(item)" class="qty-btn">+</button>
                </div>
              </div>
              <button @click.stop="removeItem(item)" class="remove-btn" title="移除">
                🗑️
              </button>
            </div>
          </div>

          <div class="cart-summary">
            <div class="summary-row">
              <span>总计:</span>
              <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
            </div>
            <button class="checkout-btn" @click="goToCheckout">
              结算 ({{ totalQuantity }}件)
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { usePanelState } from '@/utils/panelState';

const router = useRouter();
const showPanel = ref(false);
const cartItems = ref([]);
const cartCount = ref(0);
const loading = ref(false);
const { panelState, setActivePanel, clearActivePanel } = usePanelState();

// 监听面板状态，如果其他面板打开则关闭当前面板
watch(() => panelState.value.activePanel, (newPanel) => {
  if (newPanel !== 'cart' && newPanel !== null) {
    showPanel.value = false;
  }
});

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0);
});

const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    return sum + (item.product.price * item.quantity);
  }, 0);
});

const togglePanel = async () => {
  showPanel.value = !showPanel.value;
  if (showPanel.value) {
    setActivePanel('cart');
    await fetchCartItems();
  } else {
    clearActivePanel();
  }
};

const closePanel = () => {
  showPanel.value = false;
  clearActivePanel();
};

const fetchCartItems = async () => {
  try {
    loading.value = true;
    const response = await axios.get('/api/cart/list');
    cartItems.value = response.data;
    cartCount.value = cartItems.value.length;
  } catch (error) {
    console.error('获取购物车失败:', error);
  } finally {
    loading.value = false;
  }
};

const updateQuantity = async (item) => {
  try {
    await axios.put('/api/cart/update', {
      productId: item.product.id,
      quantity: item.quantity
    });
  } catch (error) {
    console.error('更新数量失败:', error);
    await fetchCartItems();
  }
};

const increaseQuantity = async (item) => {
  if (item.quantity < item.product.stock) {
    item.quantity++;
    await updateQuantity(item);
  }
};

const decreaseQuantity = async (item) => {
  if (item.quantity > 1) {
    item.quantity--;
    await updateQuantity(item);
  }
};

const removeItem = async (item) => {
  try {
    await axios.delete(`/api/cart/remove/${item.product.id}`);
    cartItems.value = cartItems.value.filter(i => i.cartItemId !== item.cartItemId);
    cartCount.value = cartItems.value.length;
  } catch (error) {
    console.error('删除失败:', error);
  }
};

const goToProduct = (productId) => {
  router.push(`/products/${productId}`);
  closePanel();
};

const goToCheckout = () => {
  router.push('/cart');
  closePanel();
};

const getFirstImage = (product) => {
  if (product.media && product.media.length > 0) {
    const firstMedia = product.media.find(m => m.mediaType === 1);
    return firstMedia ? firstMedia.url : null;
  }
  return null;
};

const fetchCount = async () => {
  try {
    const response = await axios.get('/api/cart/count');
    cartCount.value = response.data.count;
  } catch (error) {
    console.error('获取购物车数量失败:', error);
  }
};

onMounted(() => {
  fetchCount();
});
</script>

<style scoped>
.floating-cart {
  position: fixed;
  bottom: 100px;
  left: 30px;
  z-index: 1001;
}

.cart-button {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.4);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.cart-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(79, 172, 254, 0.5);
}

.cart-button.active {
  background: linear-gradient(135deg, #00f2fe 0%, #4facfe 100%);
}

.cart-button .icon {
  font-size: 28px;
}

.badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4444;
  color: white;
  border-radius: 12px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
}

.cart-panel {
  position: absolute;
  bottom: 70px;
  left: 0;
  width: 420px;
  max-height: 600px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.panel-loading,
.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #4facfe;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.cart-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.cart-list {
  overflow-y: auto;
  max-height: 400px;
  padding: 12px;
}

.cart-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 8px;
  background: #f8f9fa;
  transition: all 0.3s ease;
  position: relative;
}

.cart-item:hover {
  background: #e9ecef;
  transform: translateX(-4px);
}

.item-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  flex-shrink: 0;
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  opacity: 0.3;
}

.item-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-price {
  font-size: 16px;
  font-weight: bold;
  color: #4facfe;
  margin-bottom: 6px;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-btn {
  width: 24px;
  height: 24px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.qty-btn:hover {
  background: #4facfe;
  color: white;
  border-color: #4facfe;
}

.qty-value {
  font-size: 14px;
  font-weight: 500;
  min-width: 20px;
  text-align: center;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: white;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.7;
  transition: all 0.3s ease;
}

.remove-btn:hover {
  opacity: 1;
  background: #ff4444;
  transform: scale(1.1);
}

.cart-summary {
  padding: 16px 20px;
  background: #f8f9fa;
  border-top: 1px solid #e0e0e0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 16px;
}

.total-price {
  font-size: 20px;
  font-weight: bold;
  color: #4facfe;
}

.checkout-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.checkout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.4);
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  transform: translateY(10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateY(10px);
  opacity: 0;
}

@media (max-width: 768px) {
  .cart-panel {
    width: 90vw;
    right: -15vw;
  }
}
</style>

