<template>
  <div class="cart-page">
    <div class="page-header">
      <h1>🛒 购物车</h1>
      <p class="subtitle">共 {{ cartItems.length }} 件商品</p>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="cartItems.length === 0" class="empty-container">
      <div class="empty-icon">🛒</div>
      <p>购物车还是空的</p>
      <router-link to="/" class="btn btn-primary">去逛逛</router-link>
    </div>

    <div v-else class="cart-content">
      <div class="cart-items">
        <div v-for="item in cartItems" :key="item.cartItemId" class="cart-item">
          <div class="item-checkbox">
            <input
              type="checkbox"
              :id="'item-' + item.cartItemId"
              v-model="item.selected"
              @change="updateSelection"
              class="checkbox-input"
            />
            <label :for="'item-' + item.cartItemId" class="checkbox-label"></label>
          </div>
          <div class="item-image" @click="goToProduct(item.product.id)">
            <img
              v-if="getFirstImage(item.product)"
              :src="getFirstImage(item.product)"
              :alt="item.product.name" />
            <div v-else class="no-image">📷</div>
          </div>
          <div class="item-info">
            <h3 class="item-name" @click="goToProduct(item.product.id)">
              {{ item.product.name }}
            </h3>
            <div class="item-meta">
              <span>成色: {{ item.product.conditionLevel }}/10</span>
              <span v-if="item.product.stock">库存: {{ item.product.stock }}件</span>
            </div>
            <div class="item-price">¥{{ item.product.price }}</div>
          </div>
          <div class="item-quantity">
            <button @click="decreaseQuantity(item)" class="qty-btn">-</button>
            <input
              type="number"
              v-model.number="item.quantity"
              @change="updateQuantity(item)"
              min="1"
              :max="item.product.stock"
              class="qty-input" />
            <button @click="increaseQuantity(item)" class="qty-btn">+</button>
          </div>
          <div class="item-subtotal">
            ¥{{ (item.product.price * item.quantity).toFixed(2) }}
          </div>
          <button @click="removeItem(item)" class="btn-remove">
            🗑️ 删除
          </button>
        </div>
      </div>

      <div class="cart-summary">
        <h3>订单摘要</h3>
        <div class="select-all-row">
          <input
            type="checkbox"
            id="select-all"
            v-model="selectAll"
            @change="toggleSelectAll"
            class="checkbox-input"
          />
          <label for="select-all" class="checkbox-label">全选</label>
        </div>
        <div class="summary-row">
          <span>已选商品：</span>
          <span>{{ selectedQuantity }} 件</span>
        </div>
        <div class="summary-row total">
          <span>总计：</span>
          <span class="total-price">¥{{ selectedPrice.toFixed(2) }}</span>
        </div>
        <button
          class="btn btn-checkout"
          @click="checkout"
          :disabled="selectedQuantity === 0"
        >
          结算 ({{ selectedQuantity }})
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const cartItems = ref([]);
const loading = ref(true);
const selectAll = ref(false);


const selectedQuantity = computed(() => {
  return cartItems.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + item.quantity, 0);
});

const selectedPrice = computed(() => {
  return cartItems.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
});

const fetchCartItems = async () => {
  try {
    loading.value = true;
    const response = await axios.get('/api/cart/list');
    cartItems.value = response.data.map(item => ({
      ...item,
      selected: false
    }));
  } catch (error) {
    console.error('获取购物车失败:', error);
    alert('获取购物车失败');
  } finally {
    loading.value = false;
  }
};

const updateSelection = () => {
  const allSelected = cartItems.value.every(item => item.selected);
  selectAll.value = allSelected;
};

const toggleSelectAll = () => {
  cartItems.value.forEach(item => {
    item.selected = selectAll.value;
  });
};

const updateQuantity = async (item) => {
  try {
    await axios.put('/api/cart/update', {
      productId: item.product.id,
      quantity: item.quantity
    });
  } catch (error) {
    console.error('更新数量失败:', error);
    alert('更新数量失败');
    await fetchCartItems();
  }
};

const increaseQuantity = async (item) => {
  if (item.quantity < item.product.stock) {
    item.quantity++;
    await updateQuantity(item);
  } else {
    alert('已达到库存上限');
  }
};

const decreaseQuantity = async (item) => {
  if (item.quantity > 1) {
    item.quantity--;
    await updateQuantity(item);
  }
};

const removeItem = async (item) => {
  if (!confirm('确定要移除该商品吗？')) {
    return;
  }

  try {
    await axios.delete(`/api/cart/remove/${item.product.id}`);
    cartItems.value = cartItems.value.filter(i => i.cartItemId !== item.cartItemId);
    alert('已从购物车移除');
  } catch (error) {
    console.error('删除失败:', error);
    alert('删除失败');
  }
};

const goToProduct = (productId) => {
  router.push(`/products/${productId}`);
};

const getFirstImage = (product) => {
  if (product.media && product.media.length > 0) {
    const firstMedia = product.media.find(m => m.mediaType === 1);
    return firstMedia ? firstMedia.url : null;
  }
  return null;
};

const checkout = async () => {
  const selectedItems = cartItems.value.filter(item => item.selected);

  if (selectedItems.length === 0) {
    alert('请选择要结算的商品');
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

    // 从购物车创建订单（只包含选中的商品）
    const selectedProductIds = selectedItems.map(item => item.product.id);
    const response = await axios.post('/api/orders', {
      productIds: selectedProductIds
    });
    const order = response.data;

    alert('订单创建成功');

    // 跳转到支付页面（使用路径参数）
    router.push(`/payment/${order.id}`);
  } catch (error) {
    console.error('结算失败:', error);
    if (error.response?.data?.message) {
      alert(error.response.data.message);
    } else {
      alert('结算失败，请重试');
    }
  }
};

onMounted(() => {
  fetchCartItems();
});
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
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

.loading-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
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

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-container p {
  color: #666;
  margin-bottom: 20px;
}

.cart-content {
  display: flex;
  gap: 30px;
}

.cart-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.item-checkbox {
  display: flex;
  align-items: center;
}

.checkbox-input {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #007bff;
}

.checkbox-label {
  cursor: pointer;
}

.item-image {
  width: 100px;
  height: 100px;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
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
  background: #f0f0f0;
  font-size: 32px;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 16px;
  margin-bottom: 8px;
  cursor: pointer;
  color: #333;
}

.item-name:hover {
  color: #007bff;
}

.item-meta {
  display: flex;
  gap: 15px;
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.item-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff4d4f;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qty-btn:hover {
  border-color: #007bff;
  color: #007bff;
}

.qty-input {
  width: 50px;
  height: 28px;
  text-align: center;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.item-subtotal {
  font-size: 18px;
  font-weight: bold;
  color: #ff4d4f;
  min-width: 100px;
  text-align: right;
}

.btn-remove {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
}

.btn-remove:hover {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.cart-summary {
  width: 300px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  height: fit-content;
  position: sticky;
  top: 20px;
}

.cart-summary h3 {
  margin-bottom: 20px;
  font-size: 18px;
}

.select-all-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 15px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  color: #666;
}

.summary-row.total {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.total-price {
  color: #ff4d4f;
}

.btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

.btn-checkout {
  background: #ff4d4f;
  color: white;
  font-weight: bold;
}

.btn-checkout:hover:not(:disabled) {
  background: #ff7875;
}

.btn-checkout:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .cart-content {
    flex-direction: column;
  }

  .cart-summary {
    width: 100%;
    position: static;
  }

  .cart-item {
    flex-wrap: wrap;
  }

  .item-quantity {
    margin-left: auto;
  }
}
</style>

