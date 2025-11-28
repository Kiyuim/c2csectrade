<template>
  <div class="home">
    <div class="header-section">
      <div class="header-top" v-if="!isAdmin">
        <div class="quick-actions-left">
          <button @click="goToOrderHistory" class="action-btn order-btn" title="查看订单">
            📦 我的订单
            <span v-if="pendingOrderCount > 0" class="badge">{{ pendingOrderCount }}</span>
          </button>
          <button @click="goToPaymentPasswordSetup" class="action-btn payment-btn" :title="hasPaymentPassword ? '修改支付密码' : '设置支付密码'">
            🔐 {{ hasPaymentPassword ? '修改支付密码' : '设置支付密码' }}
          </button>
        </div>
        <div class="user-info-bar" v-if="isLoggedIn">
          <div class="user-name-display">
            <span class="user-greeting">👋 您好，{{ authStore.user?.displayName || authStore.user?.username }}</span>
            <CreditBadge v-if="authStore.user?.creditLevel" :level="authStore.user.creditLevel" />
          </div>
          <div class="balance-display">
            <span class="balance-label">💰 余额：</span>
            <span class="balance-amount">¥{{ userBalance.toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <h1>🛒 二手物品交易平台</h1>
      <div v-if="isAdmin" class="admin-notice">
        <span class="admin-badge">👑 管理员模式</span>
        <p class="admin-text">您正在以管理员身份浏览，专注于平台管理功能</p>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <div class="search-bar">
        <input
            v-model="searchKeyword"
            @input="handleSearch"
            type="text"
            placeholder="🔍 智能搜索：输入关键词即可模糊匹配商品名称、描述、分类..."
            class="search-input"
        />
        <button @click="performSearch" class="search-btn">🔍 搜索</button>
      </div>

      <div class="filters-container">
        <!-- 筛选条件 -->
        <div class="filter-section">
          <h3 class="filter-title">🔍 筛选条件</h3>

          <div class="filter-row">
            <div class="filter-group">
              <label class="filter-label">📦 主分类</label>
              <select v-model="filters.mainCategory" @change="onMainCategoryChange" class="filter-select">
                <option value="">全部分类</option>
                <option v-for="category in categories" :key="category.value" :value="category.value">
                  {{ category.icon }} {{ category.label.split(' ')[1] }}
                </option>
              </select>
            </div>

            <div class="filter-group">
              <label class="filter-label">🏷️ 具体分类</label>
              <select v-model="filters.subCategory" class="filter-select" :disabled="!filters.mainCategory">
                <option value="">全部</option>
                <option v-for="subCat in availableSubCategories" :key="subCat.value" :value="subCat.value">
                  {{ subCat.label }}
                </option>
              </select>
            </div>

            <div class="filter-group">
              <label class="filter-label">💰 价格区间</label>
              <div class="price-range">
                <input v-model.number="filters.minPrice" type="number" placeholder="最低价" class="filter-input small" />
                <span class="range-separator">-</span>
                <input v-model.number="filters.maxPrice" type="number" placeholder="最高价" class="filter-input small" />
              </div>
            </div>
          </div>

          <div class="filter-row">
            <div class="filter-group">
              <label class="filter-label">✨ 成色</label>
              <select v-model.number="filters.conditionLevel" class="filter-select">
                <option :value="null">全部</option>
                <option :value="10">全新</option>
                <option :value="9">九成新</option>
                <option :value="8">八成新</option>
                <option :value="7">七成新</option>
              </select>
            </div>

            <div class="filter-group">
              <label class="filter-label">📍 位置</label>
              <input v-model="filters.location" type="text" placeholder="输入城市名称" class="filter-input" />
            </div>

            <div class="filter-actions">
              <button @click="applyFilters" class="apply-filters-btn">✅ 应用筛选</button>
              <button @click="clearFilters" class="clear-filters-btn">🔄 清除筛选</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 猜你喜欢推荐 -->
    <RecommendationSection
        v-if="recommendedProducts.length > 0"
        title="🎯 猜你喜欢"
        subtitle="基于您的浏览历史和兴趣"
        :products="recommendedProducts"
    />

    <!-- 商品列表 -->
    <div v-if="loading" class="loading">正在加载商品...</div>
    <div v-else-if="products.length === 0" class="no-products">暂无商品</div>
    <div v-else class="product-grid">
      <div
          v-for="product in displayedProducts"
          :key="product.id"
          class="product-card"
          @click="goToProduct(product.id)"
      >
        <div class="product-image">
          <img v-if="getFirstImage(product)" :src="getFirstImage(product)" :alt="product.name" />
          <div v-else class="no-image">暂无图片</div>
        </div>
        <div class="product-info">
          <h3 class="product-name" v-html="product.highlightedName || product.name"></h3>
          <div class="product-price">¥{{ product.price }}</div>
          <div class="product-meta">
            <span v-if="product.conditionLevel">{{ product.conditionLevel }}成新</span>
            <span v-if="product.location" class="location">📍{{ product.location }}</span>
          </div>
          <div class="seller-info">
            <img
                :src="getSellerAvatar(product)"
                :alt="getSellerName(product)"
                class="seller-avatar"
                @error="onAvatarError($event, getSellerName(product))"
                @click.stop="startChat(product)"
                :title="'点击与 ' + getSellerName(product) + ' 私聊'"
            />
            <span class="seller-name">{{ getSellerName(product) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页控件 -->
    <div v-if="!loading && totalPages > 1" class="pagination">
      <button
          class="pagination-btn"
          :disabled="currentPage === 1"
          @click="goToPage(1)">
        首页
      </button>
      <button
          class="pagination-btn"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)">
        &#8249; 上一页
      </button>

      <div class="pagination-numbers">
        <button
            v-for="page in displayedPageNumbers"
            :key="page"
            class="pagination-number"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)">
          {{ page }}
        </button>
      </div>

      <button
          class="pagination-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)">
        下一页 &#8250;
      </button>
      <button
          class="pagination-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(totalPages)">
        末页
      </button>

      <div class="pagination-info">
        <span>跳转到</span>
        <input
            type="number"
            v-model.number="jumpToPageInput"
            @keyup.enter="jumpToPage"
            min="1"
            :max="totalPages"
            class="page-input">
        <button class="pagination-btn-small" @click="jumpToPage">Go</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import productService from '@/api/productService';
import { categories } from '@/utils/categoryData';
import RecommendationSection from '@/components/RecommendationSection.vue';
import CreditBadge from '@/components/CreditBadge.vue';
import axios from 'axios';

const router = useRouter();
const authStore = useAuthStore();

const isLoggedIn = computed(() => authStore.isLoggedIn);
const isAdmin = computed(() => authStore.user?.role === 'ROLE_ADMIN');

const products = ref([]);
const recommendedProducts = ref([]);
const loading = ref(false);
const searchKeyword = ref('');
const hasPaymentPassword = ref(false);
const userBalance = ref(0);
const pendingOrderCount = ref(0);
const filters = ref({
  minPrice: null,
  maxPrice: null,
  conditionLevel: null,
  location: '',
  mainCategory: '',
  subCategory: ''
});

// 分页相关
const currentPage = ref(1);
const pageSize = ref(12); // 每页显示12个商品
const jumpToPageInput = ref(1);

// 排序
const sortBy = ref('default');

let searchTimeout = null;

// 可用的子分类列表
const availableSubCategories = computed(() => {
  if (!filters.value.mainCategory) return [];
  const mainCat = categories.find(cat => cat.value === filters.value.mainCategory);
  return mainCat ? mainCat.subcategories : [];
});

// 当主分类改变时，清除子分类选择
const onMainCategoryChange = () => {
  filters.value.subCategory = '';
};

// 获取需要筛选的分类
const getCategoryFilter = () => {
  // 如果选择了子分类，使用子分类
  if (filters.value.subCategory) {
    return filters.value.subCategory;
  }
  // 如果只选择了主分类，返回该主分类下所有子分类的逗号分隔字符串
  if (filters.value.mainCategory) {
    const mainCat = categories.find(cat => cat.value === filters.value.mainCategory);
    if (mainCat) {
      return mainCat.subcategories.map(sub => sub.value).join(',');
    }
  }
  return '';
};

// 分页计算属性
const totalProducts = computed(() => products.value.length);

const totalPages = computed(() => {
  return Math.ceil(totalProducts.value / pageSize.value);
});

// 排序后的商品列表
const sortedProducts = computed(() => {
  const productsCopy = [...products.value];

  switch (sortBy.value) {
    case 'price-asc':
      // 价格从低到高
      return productsCopy.sort((a, b) => {
        const priceA = parseFloat(a.price) || 0;
        const priceB = parseFloat(b.price) || 0;
        return priceA - priceB;
      });

    case 'price-desc':
      // 价格从高到低
      return productsCopy.sort((a, b) => {
        const priceA = parseFloat(a.price) || 0;
        const priceB = parseFloat(b.price) || 0;
        return priceB - priceA;
      });

    case 'time-desc':
      // 最新发布（创建时间从新到旧）
      return productsCopy.sort((a, b) => {
        const timeA = new Date(a.createdAt || 0).getTime();
        const timeB = new Date(b.createdAt || 0).getTime();
        return timeB - timeA;
      });

    case 'time-asc':
      // 最早发布（创建时间从旧到新）
      return productsCopy.sort((a, b) => {
        const timeA = new Date(a.createdAt || 0).getTime();
        const timeB = new Date(b.createdAt || 0).getTime();
        return timeA - timeB;
      });

    case 'default':
    default:
      // 综合排序：结合时间和其他因素
      return productsCopy.sort((a, b) => {
        // 优先显示较新的商品，同时考虑其他因素
        const timeA = new Date(a.createdAt || 0).getTime();
        const timeB = new Date(b.createdAt || 0).getTime();
        return timeB - timeA;
      });
  }
});

const displayedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return sortedProducts.value.slice(start, end);
});

// 显示的页码范围
const displayedPageNumbers = computed(() => {
  const pages = [];
  const maxVisible = 5; // 最多显示5个页码
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages.value, start + maxVisible - 1);

  // 调整start确保显示足够的页码
  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1);
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return pages;
});



const fetchProducts = async () => {
  loading.value = true;
  try {
    const categoryFilter = getCategoryFilter();
    const params = {
      keyword: searchKeyword.value || undefined,
      minPrice: filters.value.minPrice || undefined,
      maxPrice: filters.value.maxPrice || undefined,
      conditionLevel: filters.value.conditionLevel || undefined,
      location: filters.value.location || undefined,
      categories: categoryFilter || undefined
    };
    const response = await productService.getAllProducts(params);
    products.value = response.data;
  } catch (error) {
    console.error('获取商品列表失败:', error);
    products.value = [];
  } finally {
    loading.value = false;
  }
};

const performSearch = async () => {
  loading.value = true;
  try {
    const categoryFilter = getCategoryFilter();
    const params = {
      keyword: searchKeyword.value || undefined,
      minPrice: filters.value.minPrice || undefined,
      maxPrice: filters.value.maxPrice || undefined,
      conditionLevel: filters.value.conditionLevel || undefined,
      location: filters.value.location || undefined,
      categories: categoryFilter || undefined
    };
    const response = await productService.getAllProducts(params);
    products.value = response.data;
  } catch (error) {
    console.error('搜索失败:', error);
    products.value = [];
  } finally {
    loading.value = false;
  }
};

// 分页跳转函数
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    jumpToPageInput.value = page;
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const jumpToPage = () => {
  if (jumpToPageInput.value >= 1 && jumpToPageInput.value <= totalPages.value) {
    goToPage(jumpToPageInput.value);
  } else {
    jumpToPageInput.value = currentPage.value;
  }
};

// 防抖搜索
const handleSearch = () => {
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    if (searchKeyword.value.trim()) {
      performSearch();
    } else {
      fetchProducts();
    }
  }, 500);
};

const applyFilters = () => {
  performSearch();
};

const clearFilters = () => {
  searchKeyword.value = '';
  filters.value = {
    minPrice: null,
    maxPrice: null,
    conditionLevel: null,
    location: '',
    mainCategory: '',
    subCategory: ''
  };
  fetchProducts();
};

const getFirstImage = (product) => {
  if (product.media && product.media.length > 0) {
    const firstMedia = product.media.find(m => m.mediaType === 1);
    return firstMedia ? firstMedia.url : null;
  }
  return null;
};

const getSellerAvatar = (product) => {
  if (product.avatarUrl) return product.avatarUrl;
  if (product.user && product.user.avatarUrl) return product.user.avatarUrl;
  const username = getSellerName(product);
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(username)}&background=007bff&color=fff&size=40`;
};

const onAvatarError = (e, name) => {
  e.target.onerror = null;
  e.target.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=007bff&color=fff&size=40`;
};

import toast from '@/utils/toast';

const getSellerName = (product) => {
  return product.displayName || product.username || product.user?.displayName || product.user?.username || `用户${product.userId}`;
};

const startChat = (product) => {
  if (!isLoggedIn.value) {
    toast.warning('请先登录再与卖家私聊');
    router.push('/login');
    return;
  }

  const currentUserId = authStore.user?.id;
  const isAdmin = authStore.user?.role === 'ROLE_ADMIN';

  if (!isAdmin && currentUserId === product.userId) {
    toast.warning('不能与自己私聊');
    return;
  }

  window.dispatchEvent(new CustomEvent('open-chat', {
    detail: {
      username: product.username || product.displayName,
      displayName: product.displayName || product.username,
      userId: product.userId,
      avatarUrl: product.avatarUrl || null
    }
  }));
};

const goToProduct = (productId) => {
  router.push(`/products/${productId}`);
};

const goToOrderHistory = () => {
  router.push('/order-history');
};

const goToPaymentPasswordSetup = () => {
  router.push('/payment-password/setup');
};

// 检查支付密码状态
const checkPaymentPasswordStatus = async () => {
  if (!isLoggedIn.value || isAdmin.value) return;
  try {
    const response = await axios.get('/api/users/payment-password/check');
    hasPaymentPassword.value = response.data.hasPaymentPassword;
  } catch (error) {
    console.error('检查支付密码状态失败:', error);
  }
};

// 获取用户余额
const fetchUserBalance = async () => {
  if (!isLoggedIn.value || isAdmin.value) return;
  try {
    const response = await axios.get('/api/users/me');
    userBalance.value = response.data.balance || 0;
  } catch (error) {
    console.error('获取用户余额失败:', error);
  }
};

// 获取未完成订单数量
const fetchPendingOrderCount = async () => {
  if (!isLoggedIn.value || isAdmin.value) return;
  try {
    const response = await axios.get('/api/orders');
    const orders = response.data;
    // 统计待支付订单数量（未过期的）
    pendingOrderCount.value = orders.filter(order => {
      if (order.status !== 'PENDING') return false;
      if (!order.expireTime) return true;
      const now = new Date().getTime();
      const expireTime = new Date(order.expireTime).getTime();
      return now < expireTime;
    }).length;
  } catch (error) {
    console.error('获取订单数量失败:', error);
  }
};

// Fetch personalized recommendations
const fetchRecommendations = async () => {
  try {
    const response = await axios.get('/api/recommendations/for-you', {
      params: { limit: 8 }
    });
    recommendedProducts.value = response.data;
  } catch (error) {
    console.debug('Failed to fetch recommendations:', error);
    // Silent fail - recommendations are optional
  }
};

onMounted(() => {
  fetchProducts();
  fetchRecommendations();
  checkPaymentPasswordStatus();
  fetchUserBalance();
  fetchPendingOrderCount();
});
</script>

<style scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.header-section {
  display: flex;
  flex-direction: column;
  margin-bottom: 30px;
  gap: 20px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.quick-actions-left {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

h1 {
  color: #333;
  font-size: 2.5em;
  margin: 0;
  text-align: center;
}

.user-info-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.user-name-display {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-greeting {
  color: white;
  font-size: 16px;
  font-weight: 500;
}

.balance-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.balance-label {
  color: white;
  font-size: 16px;
  font-weight: 500;
}

.balance-amount {
  color: #ffd700;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.admin-notice {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(255, 215, 0, 0.3);
  border: 2px solid #f0c419;
}

.admin-badge {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 8px;
}

.admin-text {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.action-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  position: relative;
}

.badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ff4d4f;
  color: white;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(255, 77, 79, 0.4);
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.order-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.order-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.payment-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.payment-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4);
}

.search-section {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 30px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 25px;
}

.search-input {
  flex: 1;
  padding: 14px 18px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.search-btn {
  padding: 14px 35px;
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.3);
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 123, 255, 0.4);
}

.filters-container {
  display: grid;
  gap: 20px;
}

.filter-section {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  padding: 25px;
  border-radius: 12px;
  border: 2px solid #e9ecef;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.filter-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 3px solid #007bff;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 15px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 15px;
  font-weight: 600;
  color: #495057;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin: 40px 0;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  flex-wrap: wrap;
}

.pagination-btn,
.pagination-btn-small {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  color: #333;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.pagination-btn-small {
  padding: 6px 12px;
  font-size: 13px;
}

.pagination-btn:hover:not(:disabled),
.pagination-btn-small:hover:not(:disabled) {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-numbers {
  display: flex;
  gap: 5px;
}

.pagination-number {
  min-width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  color: #333;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-number:hover {
  background: #e3f2fd;
  border-color: #007bff;
  color: #007bff;
}

.pagination-number.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
  font-weight: bold;
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 20px;
  padding-left: 20px;
  border-left: 1px solid #ddd;
}

.pagination-info span {
  color: #666;
  font-size: 14px;
}

.page-input {
  width: 50px;
  padding: 6px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  text-align: center;
  font-size: 14px;
}

.page-input:focus {
  outline: none;
  border-color: #007bff;
}

.filter-label {
  font-size: 15px;
  font-weight: 600;
  color: #495057;
}

.filter-select,
.filter-input {
  padding: 12px 16px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  font-size: 15px;
  color: #333;
  background: white;
  transition: all 0.3s;
}

.filter-select:focus,
.filter-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.filter-select:disabled {
  background-color: #f1f3f5;
  color: #adb5bd;
  cursor: not-allowed;
  opacity: 0.6;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-input.small {
  width: auto;
  flex: 1;
}

.range-separator {
  color: #6c757d;
  font-weight: 600;
  font-size: 16px;
}

.filter-actions {
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  align-items: center;
  margin-top: 10px;
}

.apply-filters-btn,
.clear-filters-btn {
  padding: 12px 28px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  white-space: nowrap;
}

.apply-filters-btn {
  background: linear-gradient(135deg, #28a745 0%, #218838 100%);
  color: white;
}

.apply-filters-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(40, 167, 69, 0.4);
}

.clear-filters-btn {
  background: linear-gradient(135deg, #6c757d 0%, #5a6268 100%);
  color: white;
}

.clear-filters-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(108, 117, 125, 0.4);
}

.loading,
.no-products {
  text-align: center;
  padding: 60px 20px;
  font-size: 18px;
  color: #666;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 25px;
  margin-top: 20px;
}

.product-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.product-image {
  width: 100%;
  height: 220px;
  overflow: hidden;
  background: #f5f5f5;
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
  color: #999;
  font-size: 16px;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 24px;
  font-weight: bold;
  color: #e74c3c;
  margin-bottom: 10px;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.location {
  color: #007bff;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.seller-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}

.seller-avatar:hover {
  transform: scale(1.1);
}

.seller-name {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    align-items: stretch;
  }

  h1 {
    font-size: 1.8em;
    text-align: center;
  }

  .quick-actions {
    justify-content: center;
    width: 100%;
  }

  .action-btn {
    flex: 1;
    justify-content: center;
    min-width: 140px;
  }

  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
  }


  .filter-row {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    flex-direction: column;
    width: 100%;
  }

  .apply-filters-btn,
  .clear-filters-btn {
    width: 100%;
  }

  .price-range {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input.small {
    width: 100%;
  }

  .pagination {
    padding: 15px 10px;
    gap: 5px;
  }

  .pagination-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .pagination-number {
    min-width: 30px;
    height: 30px;
    font-size: 12px;
  }

  .pagination-info {
    margin-left: 0;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid #ddd;
    padding-top: 10px;
    width: 100%;
    justify-content: center;
  }
}
:deep(.highlight) {
  color: #e74c3c;
  font-weight: bold;
  background-color: rgba(231, 76, 60, 0.1);
  padding: 0 4px;
  border-radius: 4px;
}
</style>
