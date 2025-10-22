<template>
  <div class="product-create-page">
    <div class="create-header">
      <h1>📝 发布商品</h1>
      <button @click="goBack" class="btn-back">← 返回</button>
    </div>

    <form @submit.prevent="handleSubmit" class="create-form">
      <div class="form-group">
        <label>商品名称 *</label>
        <input v-model="product.name" type="text" placeholder="请输入商品名称" required />
      </div>

      <div class="form-group">
        <label>商品分类 *</label>
        <select v-model="selectedMainCategory" @change="onMainCategoryChange" required>
          <option value="">请选择主分类</option>
          <option v-for="category in mainCategories" :key="category.value" :value="category.value">
            {{ category.label }}
          </option>
        </select>
      </div>

      <div v-if="selectedMainCategory" class="form-group">
        <label>子分类 *</label>
        <select v-model="product.category" required>
          <option value="">请选择子分类</option>
          <option v-for="subCat in availableSubCategories" :key="subCat.value" :value="subCat.value">
            {{ subCat.label }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>商品描述 *</label>
        <textarea v-model="product.description" placeholder="请详细描述商品信息" rows="5" required></textarea>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>价格 (¥) *</label>
          <input v-model.number="product.price" type="number" step="0.01" min="0" placeholder="0.00" required />
        </div>

        <div class="form-group">
          <label>库存数量 *</label>
          <input v-model.number="product.stock" type="number" min="1" placeholder="1" required />
        </div>

        <div class="form-group">
          <label>成色 *</label>
          <select v-model.number="product.conditionLevel" required>
            <option :value="10">全新 (10成新)</option>
            <option :value="9">九成新</option>
            <option :value="8">八成新</option>
            <option :value="7">七成新</option>
            <option :value="6">六成新</option>
            <option :value="5">五成新</option>
          </select>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>省份 *</label>
          <select v-model="selectedProvince" @change="onProvinceChange" required>
            <option value="">请选择省份</option>
            <option v-for="province in provinces" :key="province.code" :value="province.code">
              {{ province.name }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>城市 *</label>
          <select v-model="product.location" required :disabled="!selectedProvince">
            <option value="">请选择城市</option>
            <option v-for="city in availableCities" :key="city.code" :value="city.name">
              {{ city.name }}
            </option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label>商品图片/视频</label>
        <input type="file" @change="handleFileChange" accept="image/*,video/*" multiple class="file-input" />
        <p class="help-text">支持上传多个图片或视频，单个文件不超过10MB</p>

        <div v-if="mediaPreviews.length > 0" class="media-preview-grid">
          <div v-for="(preview, index) in mediaPreviews" :key="index" class="media-preview-item">
            <img v-if="preview.type === 'image'" :src="preview.url" alt="预览" />
            <video v-if="preview.type === 'video'" :src="preview.url" controls></video>
            <button type="button" @click="removeMedia(index)" class="remove-media-btn">×</button>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" @click="goBack" class="btn-cancel">取消</button>
        <button type="submit" class="btn-submit" :disabled="submitting">
          {{ submitting ? '发布中...' : '发布商品' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import productService from '@/api/productService';
import { provinces, cities } from '@/utils/locationData';
import { getMainCategories, getSubCategories } from '@/utils/categoryData';
import { toast } from '@/services/toast';

const router = useRouter();

const product = reactive({
  name: '',
  description: '',
  price: null,
  stock: 1,
  conditionLevel: 9,
  location: '',
  category: ''
});

const selectedProvince = ref('');
const selectedMainCategory = ref('');
const mediaFiles = ref([]);
const mediaPreviews = ref([]);
const submitting = ref(false);

// 分类数据
const mainCategories = getMainCategories();

const availableSubCategories = computed(() => {
  if (!selectedMainCategory.value) return [];
  return getSubCategories(selectedMainCategory.value);
});

const availableCities = computed(() => {
  if (!selectedProvince.value) return [];
  return cities[selectedProvince.value] || [];
});

const onProvinceChange = () => {
  product.location = '';
};

const onMainCategoryChange = () => {
  product.category = '';
};

const handleFileChange = (event) => {
  const files = Array.from(event.target.files);

  files.forEach(file => {
    if (file.size > 10 * 1024 * 1024) {
      toast('文件大小不能超过10MB', 'error');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const type = file.type.startsWith('image/') ? 'image' : 'video';
      mediaPreviews.value.push({
        type,
        url: e.target.result
      });
      mediaFiles.value.push(file);
    };
    reader.readAsDataURL(file);
  });
};

const removeMedia = (index) => {
  mediaPreviews.value.splice(index, 1);
  mediaFiles.value.splice(index, 1);
};

const handleSubmit = async () => {
  if (!product.category) {
    toast('请选择商品分类', 'warning');
    return;
  }

  submitting.value = true;

  try {
    const formData = new FormData();

    // 将商品数据组装成JSON对象，与后端接口保持一致
    const productData = {
      name: product.name,
      description: product.description,
      price: product.price,
      stock: product.stock,
      conditionLevel: product.conditionLevel,
      location: product.location,
      category: product.category
    };

    // 后端期望接收 'productData' 参数（JSON字符串）
    formData.append('productData', JSON.stringify(productData));

    // 添加文件
    mediaFiles.value.forEach((file) => {
      formData.append('files', file);
    });

    await productService.createProduct(formData);
    toast('✅ 商品发布成功！', 'success');
    router.push('/');
  } catch (error) {
    console.error('发布商品失败:', error);

    // 友好的错误提示，不暴露HTTP状态码
    const status = error?.response?.status;
    const errorData = error?.response?.data;
    let errorMsg = '';

    if (status === 400) {
      errorMsg = '❌ 请检查商品信息是否完整正确';
    } else if (status === 401 || status === 403) {
      errorMsg = '❌ 请先登录后再发布商品';
    } else if (status === 500) {
      errorMsg = '❌ 服务器错误，请稍后重试';
    } else if (typeof errorData === 'string' && errorData && !errorData.includes('status code')) {
      errorMsg = '❌ ' + errorData;
    } else {
      errorMsg = '❌ 发布商品失败，请稍后重试';
    }

    toast(errorMsg, 'error');
  } finally {
    submitting.value = false;
  }
};

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.product-create-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.create-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.create-header h1 {
  font-size: 28px;
  color: #333;
}

.btn-back {
  padding: 10px 20px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.btn-back:hover {
  background: #5a6268;
}

.create-form {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  color: #333;
  background-color: white;
  transition: border-color 0.3s;
}

.form-group select option {
  color: #333;
  background-color: white;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #007bff;
}

.form-group select:disabled {
  background-color: #e9ecef;
  color: #6c757d;
  cursor: not-allowed;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.file-input {
  padding: 8px !important;
}

.help-text {
  margin-top: 5px;
  font-size: 14px;
  color: #6c757d;
}

.media-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.media-preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e0e0e0;
}

.media-preview-item img,
.media-preview-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-media-btn {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 30px;
  height: 30px;
  background: rgba(220, 53, 69, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  transition: background 0.3s;
}

.remove-media-btn:hover {
  background: rgba(200, 35, 51, 1);
}

.form-actions {
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  margin-top: 30px;
}

.btn-cancel,
.btn-submit {
  padding: 12px 30px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel {
  background: #6c757d;
  color: white;
}

.btn-cancel:hover {
  background: #5a6268;
}

.btn-submit {
  background: #28a745;
  color: white;
}

.btn-submit:hover:not(:disabled) {
  background: #218838;
}

.btn-submit:disabled {
  background: #94d3a2;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .create-header {
    flex-direction: column;
    gap: 15px;
  }

  .create-form {
    padding: 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>

