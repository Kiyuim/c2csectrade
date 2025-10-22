<template>
  <div v-if="products && products.length > 0" class="recommendation-section">
    <div class="section-header">
      <h2>{{ title }}</h2>
      <span v-if="subtitle" class="subtitle">{{ subtitle }}</span>
    </div>

    <div class="products-container">
      <!-- 左滑按钮 -->
      <button
        v-if="products.length > itemsPerView"
        @click="scrollLeft"
        class="scroll-button scroll-left"
        :disabled="currentIndex === 0">
        &#8249;
      </button>

      <!-- 商品网格容器 -->
      <div class="products-wrapper" ref="productsWrapper">
        <div
          class="products-grid"
          :style="gridTransform">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-card"
            @click="goToProduct(product.id)">

            <div class="product-image">
              <img
                v-if="getProductImage(product) && !failedImages[product.id]"
                :src="getProductImage(product)"
                :alt="product.name"
                :data-product-id="product.id"
                @error="handleImageError" />
              <img
                v-else-if="failedImages[product.id]"
                :src="placeholderImage"
                alt="暂无图片" />
              <div v-else class="no-image">
                <span>📷</span>
                <p>暂无图片</p>
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-price">¥{{ product.price }}</p>
              <div class="product-meta">
                <span v-if="product.category" class="category">{{ product.category }}</span>
                <span v-if="product.location" class="location">📍 {{ product.location }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右滑按钮 -->
      <button
        v-if="products.length > itemsPerView"
        @click="scrollRight"
        class="scroll-button scroll-right"
        :disabled="currentIndex >= maxIndex">
        &#8250;
      </button>
    </div>

    <!-- 指示器 -->
    <div v-if="products.length > itemsPerView" class="indicators">
      <span
        v-for="index in totalDots"
        :key="index"
        class="indicator-dot"
        :class="{ active: (index - 1) === Math.floor(currentIndex.value / itemsPerView.value) }"
        @click="goToSlide(index - 1)">
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  products: {
    type: Array,
    required: true
  }
});

const router = useRouter();
const productsWrapper = ref(null);
const currentIndex = ref(0);
const itemsPerView = ref(10); // 默认显示10个商品

// track failed images to avoid repeated attempts
const failedImages = ref({});

// small inline SVG placeholder
const placeholderImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="%23f5f5f5"/><text x="50%" y="50%" font-size="20" fill="%23cccccc" dy=".35em" text-anchor="middle">暂无图片</text></svg>';

// 响应式调整每屏显示的商品数量
const updateItemsPerView = () => {
  const width = window.innerWidth;
  if (width >= 1200) {
    itemsPerView.value = 4;
  } else if (width >= 768) {
    itemsPerView.value = 3;
  } else if (width >= 480) {
    itemsPerView.value = 2;
  } else {
    itemsPerView.value = 1;
  }
};

const maxIndex = computed(() => {
  return Math.max(0, props.products.length - itemsPerView.value);
});

const totalDots = computed(() => {
  return Math.ceil(props.products.length / itemsPerView.value);
});

const gridTransform = computed(() => {
  const translateX = -(currentIndex.value * (220 + 20)); // 220px卡片宽度 + 20px间距
  return { transform: `translateX(${translateX}px)` };
});

// 获取商品图片 - 参考其他文件的getFirstImage逻辑，但更健壮
const getProductImage = (product) => {
  if (!product) return null;

  // 优先使用 media 数组中的图片（mediaType === 1）
  if (product.media && product.media.length > 0) {
    const firstMedia = product.media.find(m => m.mediaType === 1);
    if (firstMedia && firstMedia.url) return firstMedia.url;
  }

  // 回退到 coverImage
  if (product.coverImage) {
    if (typeof product.coverImage === 'string') return product.coverImage;
    if (product.coverImage.url) return product.coverImage.url;
  }

  // 其他可能的字段
  if (product.imageUrl) return product.imageUrl;
  if (product.images && product.images.length > 0) return product.images[0];
  if (product.pictures && product.pictures.length > 0) return product.pictures[0];

  return null;
};

const goToProduct = (productId) => {
  router.push(`/products/${productId}`);
};

const handleImageError = (e) => {
  // mark as failed to avoid future attempts
  const productId = e.target.dataset.productId;
  if (productId) {
    failedImages.value[productId] = true;
  }

  // replace with placeholder instead of hiding
  e.target.src = placeholderImage;
  e.target.alt = '暂无图片';
};

const scrollLeft = () => {
  if (currentIndex.value > 0) {
    currentIndex.value = Math.max(0, currentIndex.value - 1);
  }
};

const scrollRight = () => {
  if (currentIndex.value < maxIndex.value) {
    currentIndex.value = Math.min(maxIndex.value, currentIndex.value + 1);
  }
};

const goToSlide = (dotIndex) => {
  currentIndex.value = Math.min(dotIndex * itemsPerView.value, maxIndex.value);
};

onMounted(() => {
  updateItemsPerView();
  window.addEventListener('resize', updateItemsPerView);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateItemsPerView);
});
</script>

<style scoped>
.recommendation-section {
  margin: 30px 0;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  margin-bottom: 20px;
  border-bottom: 2px solid #007bff;
  padding-bottom: 10px;
}

.section-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
  display: inline-block;
}

.subtitle {
  margin-left: 15px;
  font-size: 14px;
  color: #666;
  font-weight: normal;
}

.products-container {
  position: relative;
  display: flex;
  align-items: center;
  margin-top: 20px;
}

.products-wrapper {
  flex: 1;
  overflow: hidden;
  margin: 0 10px;
}

.products-grid {
  display: flex;
  transition: transform 0.3s ease;
  gap: 20px;
}

.product-card {
  flex: 0 0 200px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: #007bff;
}

.product-image {
  width: 100%;
  height: 160px;
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
  font-size: 40px;
  color: #ccc;
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 36px;
  line-height: 1.3;
}

.product-price {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
  margin: 8px 0;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 11px;
  color: #666;
}

.category {
  background: #e3f2fd;
  color: #1976d2;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
}

.location {
  color: #666;
  font-size: 10px;
}

.scroll-button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #007bff;
  background: white;
  color: #007bff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  transition: all 0.3s ease;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.2);
}

.scroll-button:hover:not(:disabled) {
  background: #007bff;
  color: white;
  transform: scale(1.1);
}

.scroll-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  border-color: #ccc;
  color: #ccc;
}

.scroll-left {
  margin-right: 10px;
}

.scroll-right {
  margin-left: 10px;
}

.indicators {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 15px;
}

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ddd;
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator-dot.active {
  background: #007bff;
  transform: scale(1.2);
}

.indicator-dot:hover {
  background: #007bff;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .product-card {
    flex: 0 0 180px;
  }

  .product-image {
    height: 140px;
  }
}

@media (max-width: 768px) {
  .products-container {
    margin: 0 -10px;
  }

  .products-wrapper {
    margin: 0 5px;
  }

  .product-card {
    flex: 0 0 160px;
  }

  .product-image {
    height: 120px;
  }

  .section-header h2 {
    font-size: 20px;
  }

  .scroll-button {
    width: 35px;
    height: 35px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .product-card {
    flex: 0 0 140px;
  }

  .product-image {
    height: 100px;
  }

  .product-info {
    padding: 10px;
  }

  .product-name {
    font-size: 12px;
    min-height: 32px;
  }

  .product-price {
    font-size: 16px;
  }
}
</style>
