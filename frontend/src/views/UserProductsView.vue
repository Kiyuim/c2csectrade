<template>
  <div class="user-products-page">
    <div class="page-header">
      <div class="header-content">
        <h1>📦 {{ displayName || username }} 的商品</h1>
        <p class="subtitle">该用户发布的所有商品</p>
      </div>
      <button @click="goBack" class="back-btn">← 返回</button>
    </div>

    <div class="products-container">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button @click="fetchUserProducts" class="retry-btn">重试</button>
      </div>
      <div v-else-if="products.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>该用户还没有发布任何商品</p>
      </div>
      <div v-else class="products-grid">
        <div v-for="product in products" :key="product.id" class="product-card">
          <div class="product-image" @click="goToProduct(product.id)">
            <img
              v-if="getFirstImage(product)"
              :src="getFirstImage(product)"
              :alt="product.name"
            />
            <div v-else class="no-image">暂无图片</div>
          </div>
          <div class="product-info">
            <h3 class="product-name" @click="goToProduct(product.id)">{{ product.name }}</h3>
            <p class="product-price">¥{{ product.price }}</p>
            <div class="product-meta">
              <span v-if="product.conditionLevel">{{ product.conditionLevel }}成新</span>
              <span v-if="product.location" class="location">📍{{ product.location }}</span>
            </div>
            <div class="product-actions">
              <button @click="goToProduct(product.id)" class="view-btn">查看详情</button>
              <button @click="deleteProduct(product.id)" class="delete-btn">🗑️ 删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { toast } from '@/services/toast';

const route = useRoute();
const router = useRouter();

const products = ref([]);
const loading = ref(false);
const error = ref('');
const username = ref(route.query.username || '');
const displayName = ref(route.query.displayName || '');
const userId = ref(route.params.userId);

const fetchUserProducts = async () => {
  loading.value = true;
  error.value = '';
  try {
    const response = await axios.get('/api/products');
    // 过滤出该用户的商品
    products.value = response.data.filter(p => p.userId === parseInt(userId.value));
  } catch (e) {
    error.value = '加载商品失败';
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const getFirstImage = (product) => {
  if (product.media && product.media.length > 0) {
    const firstMedia = product.media.find(m => m.mediaType === 1);
    return firstMedia ? firstMedia.url : null;
  }
  if (product.imageUrls && product.imageUrls.length > 0) {
    return product.imageUrls[0];
  }
  return null;
};

const goToProduct = (productId) => {
  router.push({ name: 'product-detail', params: { id: productId } });
};

const deleteProduct = async (productId) => {
  if (!confirm('确定要删除这个商品吗？此操作不可恢复。')) {
    return;
  }

  try {
    await axios.delete(`/api/products/${productId}`);
    toast('商品删除成功', 'success');
    fetchUserProducts(); // 重新加载商品列表
  } catch (error) {
    console.error('删除商品失败:', error);
    toast('删除商品失败，请重试', 'error');
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  fetchUserProducts();
});
</script>

<style scoped>
.user-products-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.page-header {
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h1 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 28px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.back-btn {
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.back-btn:hover {
  background: #5568d3;
}

.products-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-height: 400px;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-icon,
.empty-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.retry-btn {
  margin-top: 16px;
  padding: 10px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.retry-btn:hover {
  background: #764ba2;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.product-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #999;
  font-size: 14px;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 10px 0;
  color: #333;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-name:hover {
  color: #667eea;
}

.product-price {
  font-size: 20px;
  font-weight: bold;
  color: #e74c3c;
  margin: 0 0 10px 0;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
  margin-bottom: 15px;
}

.location {
  color: #667eea;
}

.product-actions {
  display: flex;
  gap: 10px;
}

.view-btn,
.delete-btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.view-btn {
  background: #667eea;
  color: white;
}

.view-btn:hover {
  background: #5568d3;
}

.delete-btn {
  background: #ff4444;
  color: white;
}

.delete-btn:hover {
  background: #cc0000;
  transform: scale(1.05);
}

@media (max-width: 768px) {
  .products-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
}
</style>

