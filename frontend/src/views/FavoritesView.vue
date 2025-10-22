<template>
  <div class="favorites-page">
    <div class="page-header">
      <h1>❤️ 我的收藏</h1>
      <p class="subtitle">共收藏 {{ favorites.length }} 件商品</p>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="favorites.length === 0" class="empty-container">
      <div class="empty-icon">📋</div>
      <p>您还没有收藏任何商品</p>
      <router-link to="/" class="btn btn-primary">去逛逛</router-link>
    </div>

    <div v-else class="favorites-grid">
      <div
        v-for="product in favorites"
        :key="product.id"
        class="product-card"
        @click="goToProduct(product.id)">
        <div class="product-image">
          <img
            v-if="getFirstImage(product)"
            :src="getFirstImage(product)"
            :alt="product.name" />
          <div v-else class="no-image">📷</div>
        </div>
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <div class="product-price">¥{{ product.price }}</div>
          <div class="product-meta">
            <span>成色: {{ product.conditionLevel }}/10</span>
            <span v-if="product.stock">库存: {{ product.stock }}</span>
          </div>
        </div>
        <button
          @click.stop="removeFavorite(product.id)"
          class="btn-remove">
          🗑️ 移除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const favorites = ref([]);
const loading = ref(true);

const fetchFavorites = async () => {
  try {
    loading.value = true;
    const response = await axios.get('/api/favorites/list');
    favorites.value = response.data;
  } catch (error) {
    console.error('获取收藏列表失败:', error);
    alert('获取收藏列表失败');
  } finally {
    loading.value = false;
  }
};

const removeFavorite = async (productId) => {
  if (!confirm('确定要取消收藏吗？')) {
    return;
  }

  try {
    await axios.delete(`/api/favorites/remove/${productId}`);
    favorites.value = favorites.value.filter(p => p.id !== productId);
    alert('已取消收藏');
  } catch (error) {
    console.error('取消收藏失败:', error);
    alert('取消收藏失败');
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

onMounted(() => {
  fetchFavorites();
});
</script>

<style scoped>
.favorites-page {
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

.empty-container p {
  color: #666;
  margin-bottom: 20px;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 48px;
  opacity: 0.3;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  font-size: 20px;
  font-weight: bold;
  color: #e74c3c;
  margin-bottom: 8px;
}

.product-meta {
  display: flex;
  gap: 15px;
  font-size: 14px;
  color: #666;
}

.btn-remove {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-remove:hover {
  background: #dc3545;
  color: white;
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

@media (max-width: 768px) {
  .favorites-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }
}
</style>

