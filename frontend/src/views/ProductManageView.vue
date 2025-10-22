<template>
  <div class="product-manage-page">
    <div class="manage-header">
      <h1>📦 我的商品管理</h1>
      <button @click="goHome" class="btn-home">🏠 返回主页</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="products.length === 0" class="no-products">
      <div class="empty-icon">📭</div>
      <p>您还没有发布任何商品</p>
      <button @click="goToPublish" class="btn-publish">立即发布</button>
    </div>
    <div v-else class="products-list">
      <div v-for="product in products" :key="product.id" class="product-manage-card">
        <div class="product-preview">
          <img
              v-if="getFirstImage(product)"
              :src="getFirstImage(product)"
              :alt="product.name"
              class="product-image" />
          <div v-else class="no-image">暂无图片</div>
        </div>

        <div class="product-details">
          <h3 class="product-name">{{ product.name }}</h3>
          <div class="product-meta">
            <span class="meta-price">¥{{ product.price }}</span>
            <span class="meta-stock">库存: {{ product.stock }}</span>
            <span class="meta-condition">{{ product.conditionLevel }}/10成新</span>
          </div>
          <p class="product-location">📍 {{ product.location }}</p>
        </div>

        <div class="product-actions">
          <button @click="editProduct(product)" class="btn-edit">✏️ 编辑</button>
          <button @click="deleteProduct(product.id)" class="btn-delete">🗑️ 删除</button>
        </div>
      </div>
    </div>

    <!-- 编辑商品弹窗 -->
    <div v-if="showEditDialog" class="edit-dialog-overlay" @click="closeEditDialog">
      <div class="edit-dialog" @click.stop>
        <div class="edit-dialog-header">
          <h2>✏️ 编辑商品</h2>
          <button @click="closeEditDialog" class="close-btn">&times;</button>
        </div>

        <div class="edit-dialog-body">
          <div class="form-group">
            <label>商品名称</label>
            <input v-model="editForm.name" type="text" class="form-input" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>价格（元）</label>
              <input v-model.number="editForm.price" type="number" step="0.01" class="form-input" />
            </div>
            <div class="form-group">
              <label>库存数量</label>
              <input v-model.number="editForm.stock" type="number" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label>成色</label>
            <select v-model.number="editForm.conditionLevel" class="form-input">
              <option :value="10">全新</option>
              <option :value="9">九成新</option>
              <option :value="8">八成新</option>
              <option :value="7">七成新</option>
              <option :value="6">六成新及以下</option>
            </select>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>主分类</label>
              <select v-model="editForm.mainCategory" @change="onMainCategoryChange" class="form-input">
                <option value="">请选择主分类</option>
                <option v-for="category in mainCategories" :key="category.value" :value="category.value">
                  {{ category.label }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>子分类</label>
              <select v-model="editForm.category" class="form-input" :disabled="!editForm.mainCategory">
                <option value="">请选择子分类</option>
                <option v-for="subCat in availableSubCategories" :key="subCat.value" :value="subCat.value">
                  {{ subCat.label }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>商品描述</label>
            <textarea v-model="editForm.description" class="form-textarea"></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>省份</label>
              <select v-model="editForm.province" @change="onProvinceChange" class="form-input">
                <option value="">请选择省份</option>
                <option v-for="province in provinces" :key="province.value" :value="province.value">
                  {{ province.label }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>城市</label>
              <select v-model="editForm.city" @change="onCityChange" class="form-input" :disabled="!editForm.province">
                <option value="">请选择城市</option>
                <option v-for="city in availableCities" :key="city.code" :value="city.name">
                  {{ city.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>商品图片/视频</label>

            <!-- 显示现有媒体 -->
            <div v-if="existingMedia.length > 0" class="existing-media-section">
              <p class="section-title">现有媒体文件：</p>
              <div class="media-preview-grid">
                <div v-for="(media, index) in existingMedia" :key="media.id" class="media-preview-item">
                  <img v-if="media.mediaType === 1" :src="media.url" alt="商品图片" />
                  <video v-if="media.mediaType === 2" :src="media.url" controls></video>
                  <button type="button" @click="removeExistingMedia(index)" class="remove-media-btn" title="删除">×</button>
                </div>
              </div>
            </div>

            <!-- 上传新媒体 -->
            <div class="upload-section">
              <label class="upload-label">添加新图片/视频：</label>
              <input type="file" @change="handleFileChange" multiple accept="image/*,video/*" class="form-file" />
              <small class="hint">可以添加新文件或删除现有文件（单个文件最大10MB）</small>
            </div>


            <!-- 新上传文件预览 -->
            <div v-if="newMediaPreviews.length > 0" class="new-media-section">
              <p class="section-title">新添加的文件：</p>
              <div class="media-preview-grid">
                <div v-for="(preview, index) in newMediaPreviews" :key="'new-' + index" class="media-preview-item">
                  <img v-if="preview.type === 'image'" :src="preview.url" alt="预览" />
                  <video v-if="preview.type === 'video'" :src="preview.url" controls></video>
                  <button type="button" @click="removeNewMedia(index)" class="remove-media-btn" title="删除">×</button>
                </div>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button @click="saveEdit" class="btn-confirm" :disabled="saving">
              {{ saving ? '保存中...' : '✓ 保存修改' }}
            </button>
            <button @click="closeEditDialog" class="btn-cancel">✕ 取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { toast } from '@/services/toast';
import { provinces, cities } from '@/utils/locationData';
import { getSubCategories, categories } from '@/utils/categoryData';

const router = useRouter();
const products = ref([]);
const loading = ref(false);

const mainCategories = computed(() => {
  return categories.map(cat => ({
    value: cat.value,
    label: cat.label
  }));
});

const availableSubCategories = computed(() => {
  if (!editForm.value.mainCategory) return [];
  return getSubCategories(editForm.value.mainCategory);
});

const showEditDialog = ref(false);
const editingProduct = ref(null);
const saving = ref(false);
const newFiles = ref([]);
const newMediaPreviews = ref([]);
const existingMedia = ref([]);
const deletedMediaIds = ref([]);

const editForm = ref({
  name: '',
  price: 0,
  stock: 0,
  conditionLevel: 9,
  description: '',
  province: '',
  city: '',
  location: ''
});

const availableCities = computed(() => {
  if (!editForm.value.province) return [];
  return cities[editForm.value.province] || [];
});

const fetchMyProducts = async () => {
  loading.value = true;
  try {
    const response = await axios.get('/api/products/my-products');
    products.value = response.data;
  } catch (error) {
    toast('加载商品失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
};

const getFirstImage = (product) => {
  if (product.media && product.media.length > 0) {
    const firstMedia = product.media.find(media => media.mediaType === 1);
    return firstMedia ? firstMedia.url : null;
  }
  return null;
};

const editProduct = (product) => {
  editingProduct.value = product;
  const [province, city] = (product.location || '').split(' ');

  // 根据当前分类找到对应的主分类
  let mainCategory = '';
  if (product.category) {
    for (const cat of categories) {
      if (cat.subcategories.some(sub => sub.value === product.category)) {
        mainCategory = cat.value;
        break;
      }
    }
  }

  editForm.value = {
    name: product.name,
    price: product.price,
    stock: product.stock,
    conditionLevel: product.conditionLevel,
    description: product.description,
    province: province || '',
    city: city || '',
    location: product.location,
    category: product.category || '',
    mainCategory: mainCategory
  };

  // 初始化媒体数据
  existingMedia.value = product.media ? [...product.media] : [];
  newMediaPreviews.value = [];
  newFiles.value = [];
  deletedMediaIds.value = [];

  showEditDialog.value = true;
};

const onMainCategoryChange = () => {
  editForm.value.category = ''; // 重置子分类
};

const onProvinceChange = () => {
  editForm.value.city = '';
  editForm.value.location = editForm.value.province;
};

const onCityChange = () => {
  editForm.value.location = editForm.value.province + ' ' + editForm.value.city;
};

const handleFileChange = (event) => {
  const files = Array.from(event.target.files);

  // 验证单个文件大小
  for (const file of files) {
    if (file.size > 10 * 1024 * 1024) {
      toast(`文件 ${file.name} 超过10MB，请选择较小的文件`, 'error');
      event.target.value = '';
      return;
    }
  }

  // 为新文件创建预览
  files.forEach(file => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const type = file.type.startsWith('image/') ? 'image' : 'video';
      newMediaPreviews.value.push({
        type,
        url: e.target.result
      });
    };
    reader.readAsDataURL(file);
    newFiles.value.push(file);
  });

  event.target.value = ''; // 清空input，允许重复选择同一文件
};

const removeExistingMedia = (index) => {
  const media = existingMedia.value[index];
  if (media.id) {
    deletedMediaIds.value.push(media.id);
  }
  existingMedia.value.splice(index, 1);
  toast('已标记删除，保存后生效', 'info');
};

const removeNewMedia = (index) => {
  newMediaPreviews.value.splice(index, 1);
  newFiles.value.splice(index, 1);
};

const saveEdit = async () => {
  // 优先检查商品分类，使用更明显的提示
  if (!editForm.value.category) {
    alert('⚠️ 请先选择商品分类！\n\n商品分类是必填项，请在"主分类"中选择一个大类，然后在"子分类"中选择具体分类。');
    toast('❌ 请选择商品分类', 'error');
    return;
  }

  if (!editForm.value.name || !editForm.value.price) {
    alert('⚠️ 请填写完整信息！\n\n商品名称和价格是必填项。');
    toast('❌ 请填写完整信息', 'error');
    return;
  }

  saving.value = true;
  try {
    const formData = new FormData();
    formData.append('productData', JSON.stringify({
      name: editForm.value.name,
      price: editForm.value.price,
      stock: editForm.value.stock,
      conditionLevel: editForm.value.conditionLevel,
      description: editForm.value.description,
      location: editForm.value.location,
      category: editForm.value.category
    }));

    // 如果有新文件，添加到表单
    if (newFiles.value.length > 0) {
      newFiles.value.forEach(file => formData.append('files', file));
    }

    await axios.put(`/api/products/${editingProduct.value.id}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    toast('✅ 商品更新成功', 'success');
    closeEditDialog();
    fetchMyProducts();
  } catch (error) {
    console.error('更新商品失败:', error);

    // 友好的错误提示，不暴露HTTP状态码
    const status = error?.response?.status;
    const errorData = error?.response?.data;
    let errorMsg = '';

    if (status === 400) {
      errorMsg = '❌ 请检查商品信息是否完整正确';
    } else if (status === 401 || status === 403) {
      errorMsg = '❌ 您没有权限修改此商品';
    } else if (status === 404) {
      errorMsg = '❌ 商品不存在或已被删除';
    } else if (status === 500) {
      errorMsg = '❌ 服务器错误，请稍后重试';
    } else if (typeof errorData === 'string' && errorData && !errorData.includes('status code')) {
      errorMsg = '❌ ' + errorData;
    } else {
      errorMsg = '❌ 修改失败，请稍后重试';
    }
    toast(errorMsg, 'error');
  } finally {
    saving.value = false;
  }
};

const closeEditDialog = () => {
  showEditDialog.value = false;
  editingProduct.value = null;
  newFiles.value = [];
  newMediaPreviews.value = [];
  existingMedia.value = [];
  deletedMediaIds.value = [];
};

const deleteProduct = async (id) => {
  if (!confirm('确定要删除这个商品吗？此操作不可恢复。')) {
    return;
  }

  try {
    await axios.delete(`/api/products/${id}`);
    toast('商品删除成功', 'success');
    fetchMyProducts();
  } catch (error) {
    console.error('删除商品失败:', error);
    toast('删除失败', 'error');
  }
};

const goHome = () => {
  router.push({ name: 'home' });
};

const goToPublish = () => {
  router.push({ name: 'product-create' });
};

onMounted(() => {
  fetchMyProducts();
});
</script>

<style scoped>
.product-manage-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 20px 30px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.manage-header h1 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.btn-home {
  padding: 10px 20px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-home:hover {
  background: #5a6268;
  transform: translateY(-2px);
}

.loading,
.no-products {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.btn-publish {
  margin-top: 20px;
  padding: 12px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-publish:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.products-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.product-manage-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 20px;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.product-manage-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.product-preview {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

.product-details {
  flex: 1;
}

.product-name {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #333;
}

.product-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 8px;
}

.meta-price {
  color: #e74c3c;
  font-size: 20px;
  font-weight: bold;
}

.meta-stock,
.meta-condition {
  color: #666;
  font-size: 14px;
}

.product-location {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.product-actions {
  display: flex;
  gap: 10px;
  flex-direction: column;
}

.btn-edit,
.btn-delete {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-edit {
  background: #007bff;
  color: white;
}

.btn-edit:hover {
  background: #0056b3;
}

.btn-delete {
  background: #dc3545;
  color: white;
}

.btn-delete:hover {
  background: #c82333;
}

/* 编辑弹窗 */
.edit-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.3s ease;
}

.edit-dialog {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  animation: slideIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.edit-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 30px;
  border-bottom: 1px solid #e0e0e0;
}

.edit-dialog-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.edit-dialog-body {
  padding: 24px 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-textarea {
  min-height: 100px;
  resize: vertical;
  font-family: inherit;
}

.form-file {
  width: 100%;
  padding: 10px;
  border: 2px dashed #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.btn-confirm,
.btn-cancel {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-confirm:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-confirm:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-cancel {
  background: #f0f0f0;
  color: #666;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

/* 媒体预览样式 */
.existing-media-section,
.new-media-section,
.upload-section {
  margin-top: 15px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin: 0 0 10px 0;
}

.upload-label {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 8px;
  display: block;
}

.media-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 10px;
}

.media-preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e0e0e0;
  background: #f9f9f9;
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
  width: 28px;
  height: 28px;
  background: rgba(220, 53, 69, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  padding: 0;
}

.remove-media-btn:hover {
  background: rgba(200, 35, 51, 1);
  transform: scale(1.1);
}

@media (max-width: 768px) {
  .product-manage-card {
    flex-direction: column;
    text-align: center;
  }

  .product-actions {
    flex-direction: row;
    width: 100%;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
