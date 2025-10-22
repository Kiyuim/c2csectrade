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
        <div class="summary-row">
          <span>商品数量：</span>
          <span>{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-row total">
          <span>总计：</span>
          <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <button class="btn btn-checkout" @click="checkout">
          结算
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

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0);
});

const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    return sum + (item.product.price * item.quantity);
  }, 0);
});

const fetchCartItems = async () => {
  try {
    loading.value = true;
    const response = await axios.get('/api/cart/list');
    cartItems.value = response.data;
  } catch (error) {
    console.error('获取购物车失败:', error);
    alert('获取购物车失败');
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
    alert('更新数量失败');
    await fetchCartItems(); // 重新加载
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

const checkout = () => {
  alert('结算功能即将开放，敬请期待！');
  // TODO: 实现结算功能
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
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.cart-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 30px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.cart-item {
  display: grid;
  grid-template-columns: 120px 1fr 150px 120px 60px;
  gap: 20px;
  align-items: center;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 15px;
  transition: all 0.3s ease;
}

.cart-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.item-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 48px;
  opacity: 0.3;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  margin: 0;
}

.item-name:hover {
  color: #007bff;
}

.item-meta {
  display: flex;
  gap: 15px;
  font-size: 14px;
  color: #666;
}

.item-price {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 5px;
}

.qty-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
}

.qty-btn:hover {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.qty-input {
  width: 60px;
  height: 32px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.item-subtotal {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  text-align: right;
}

.btn-remove {
  background: none;
  border: none;
  color: #dc3545;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-remove:hover {
  color: #c82333;
  transform: scale(1.1);
}

.cart-summary {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 20px;
}

.cart-summary h3 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 16px;
  color: #666;
}

.summary-row.total {
  border-top: 2px solid #e0e0e0;
  padding-top: 15px;
  margin-top: 15px;
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.total-price {
  color: #e74c3c;
  font-size: 24px;
}

.btn {
  padding: 12px 30px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  transition: all 0.3s ease;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

.btn-checkout {
  width: 100%;
  background: #28a745;
  color: white;
  margin-top: 20px;
}

.btn-checkout:hover {
  background: #218838;
}

@media (max-width: 968px) {
  .cart-content {
    grid-template-columns: 1fr;
  }

  .cart-item {
    grid-template-columns: 80px 1fr;
    gap: 10px;
  }

  .item-quantity,
  .item-subtotal {
    grid-column: 2;
  }

  .btn-remove {
    grid-column: 2;
    justify-self: end;
  }
}
</style>

