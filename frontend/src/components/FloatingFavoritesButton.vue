<template>
  <div class="floating-favorites">
    <!-- 浮动按钮 -->
    <button @click="togglePanel" class="favorites-button" :class="{ active: showPanel }">
      <span class="icon">❤️</span>
      <span class="badge" v-if="favoriteCount > 0">{{ favoriteCount }}</span>
    </button>

    <!-- 收藏夹面板 -->
    <transition name="slide-fade">
      <div v-if="showPanel" class="favorites-panel">
        <div class="panel-header">
          <h3>❤️ 我的收藏</h3>
          <button @click="closePanel" class="close-btn">✕</button>
        </div>

        <div v-if="loading" class="panel-loading">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="favorites.length === 0" class="panel-empty">
          <div class="empty-icon">📋</div>
          <p>还没有收藏商品</p>
        </div>

        <div v-else class="favorites-list">
          <div v-for="product in favorites" :key="product.id" class="favorite-item">
            <div class="item-image" @click="goToProduct(product.id)">
              <img v-if="getFirstImage(product)" :src="getFirstImage(product)" :alt="product.name" />
              <div v-else class="no-image">📷</div>
            </div>
            <div class="item-info" @click="goToProduct(product.id)">
              <h4 class="item-title">{{ product.name }}</h4>
              <div class="item-price">¥{{ product.price }}</div>
              <div class="item-meta">
                <span v-if="product.stock">库存: {{ product.stock }}</span>
              </div>
            </div>
            <button @click.stop="removeFavorite(product.id)" class="remove-btn" title="移除">
              🗑️
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { usePanelState } from '@/utils/panelState';

const router = useRouter();
const showPanel = ref(false);
const favorites = ref([]);
const favoriteCount = ref(0);
const loading = ref(false);
const { panelState, setActivePanel, clearActivePanel } = usePanelState();

// 监听面板状态，如果其他面板打开则关闭当前面板
watch(() => panelState.value.activePanel, (newPanel) => {
  if (newPanel !== 'favorites' && newPanel !== null) {
    showPanel.value = false;
  }
});

const togglePanel = async () => {
  showPanel.value = !showPanel.value;
  if (showPanel.value) {
    setActivePanel('favorites');
    await fetchFavorites();
  } else {
    clearActivePanel();
  }
};

const closePanel = () => {
  showPanel.value = false;
  clearActivePanel();
};

const fetchFavorites = async () => {
  try {
    loading.value = true;
    const response = await axios.get('/api/favorites/list');
    favorites.value = response.data;
    favoriteCount.value = favorites.value.length;
  } catch (error) {
    console.error('获取收藏列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const removeFavorite = async (productId) => {
  try {
    await axios.delete(`/api/favorites/remove/${productId}`);
    favorites.value = favorites.value.filter(p => p.id !== productId);
    favoriteCount.value = favorites.value.length;
  } catch (error) {
    console.error('移除收藏失败:', error);
  }
};

const goToProduct = (productId) => {
  router.push(`/products/${productId}`);
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
    const response = await axios.get('/api/favorites/count');
    favoriteCount.value = response.data.count;
  } catch (error) {
    console.error('获取收藏数量失败:', error);
  }
};

onMounted(() => {
  fetchCount();
});
</script>

<style scoped>
.floating-favorites {
  position: fixed;
  bottom: 170px;
  left: 30px;
  z-index: 1001;
}

.favorites-button {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.favorites-button:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(245, 87, 108, 0.5);
}

.favorites-button.active {
  background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%);
}

.favorites-button .icon {
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

.favorites-panel {
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
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
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
  border-top: 3px solid #f5576c;
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

.favorites-list {
  overflow-y: auto;
  max-height: 520px;
  padding: 12px;
}

.favorite-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 8px;
  background: #f8f9fa;
  transition: all 0.3s ease;
  position: relative;
}

.favorite-item:hover {
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
  color: #f5576c;
  margin-bottom: 4px;
}

.item-meta {
  font-size: 12px;
  color: #666;
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
  .favorites-panel {
    width: 90vw;
    right: -15vw;
  }
}
</style>

