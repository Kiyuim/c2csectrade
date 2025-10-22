<template>
  <div class="admin-users">
    <div class="page-header">
      <button @click="goHome" class="home-btn">🏠 返回主页</button>
      <div class="header-content">
        <h1>👥 用户管理中心</h1>
        <p class="subtitle">管理和查看所有注册用户</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon">👤</div>
        <div class="stat-info">
          <div class="stat-label">总用户数</div>
          <div class="stat-value">{{ totalUsers }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">👑</div>
        <div class="stat-info">
          <div class="stat-label">管理员</div>
          <div class="stat-value">{{ adminCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🆕</div>
        <div class="stat-info">
          <div class="stat-label">今日新增</div>
          <div class="stat-value">{{ todayNewUsers }}</div>
        </div>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <div class="search-bar">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchKeyword"
          @input="handleSearch"
          type="text"
          placeholder="搜索用户名或邮箱..."
          class="search-input"
        />
      </div>
      <div class="filter-buttons">
        <button
          :class="['filter-btn', { active: roleFilter === 'all' }]"
          @click="roleFilter = 'all'">
          全部用户
        </button>
        <button
          :class="['filter-btn', { active: roleFilter === 'admin' }]"
          @click="roleFilter = 'admin'">
          管理员
        </button>
        <button
          :class="['filter-btn', { active: roleFilter === 'user' }]"
          @click="roleFilter = 'user'">
          普通用户
        </button>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="users-container">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button @click="fetchUsers" class="retry-btn">重试</button>
      </div>
      <div v-else-if="filteredUsers.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>未找到匹配的用户</p>
      </div>
      <div v-else class="users-grid">
        <div v-for="user in filteredUsers" :key="user.id" class="user-card">
          <div class="user-header">
            <img :src="getUserAvatar(user)" :alt="user.username" class="user-avatar" />
            <div class="user-badges">
              <span v-if="isAdmin(user)" class="badge badge-admin">👑 管理员</span>
              <span v-else class="badge badge-user">👤 用户</span>
            </div>
          </div>
          <div class="user-body">
            <h3 class="user-name">{{ user.displayName || user.username }}</h3>
            <p class="user-username">@{{ user.username }}</p>
            <div class="user-info">
              <div class="info-item">
                <span class="info-icon">📧</span>
                <span class="info-text">{{ user.email }}</span>
              </div>
              <div class="info-item">
                <span class="info-icon">🆔</span>
                <span class="info-text">ID: {{ user.id }}</span>
              </div>
            </div>
          </div>
          <div class="user-actions">
            <button @click="sendMessage(user)" class="action-btn primary">
              💬 发送消息
            </button>
            <button @click="viewUserProducts(user)" class="action-btn">
              📦 查看商品
            </button>
            <button @click="openEditUsername(user)" class="action-btn">
              ✏️ 修改用户名
            </button>
            <button @click="deleteUser(user)" class="action-btn danger">
              🗑️ 删除用户
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改用户名弹窗 -->
    <template v-if="showEditDialog">
      <div class="edit-dialog-overlay" @click="closeEditDialog">
        <div class="edit-dialog" @click.stop>
          <h2>✏️ 修改用户名</h2>
          <div class="edit-dialog-header">
            <span class="edit-dialog-label">当前用户名</span>
            <span class="edit-dialog-current">{{ editingUser?.username }}</span>
          </div>
          <div class="edit-dialog-divider"></div>
          <div class="edit-input-group">
            <label class="edit-input-label">新用户名</label>
            <input v-model="editUsername" placeholder="请输入新的用户名..." class="edit-input" />
            <div class="edit-dialog-tip">💡 新用户名仅支持中英文、数字和下划线</div>
          </div>
          <div class="edit-dialog-actions">
            <button @click="confirmEditUsername" class="confirm-btn" :disabled="!editUsername.trim()">
              ✓ 确认修改
            </button>
            <button @click="closeEditDialog" class="cancel-btn">
              ✕ 取消
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { toast } from '@/services/toast';

const router = useRouter();
const users = ref([]);
const loading = ref(false);
const error = ref('');
const searchKeyword = ref('');
const roleFilter = ref('all');
const showEditDialog = ref(false);
const editUsername = ref('');
const editingUser = ref(null);

const fetchUsers = async () => {
  loading.value = true;
  error.value = '';
  try {
    const { data } = await axios.get('/api/admin/users');
    users.value = data;
  } catch (e) {
    error.value = '加载用户失败';
    console.error(e);
  } finally {
    loading.value = false;
  }
};

// 统计信息
const totalUsers = computed(() => users.value.length);

const adminCount = computed(() => {
  return users.value.filter(u => isAdmin(u)).length;
});

const todayNewUsers = computed(() => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return users.value.filter(u => {
    if (u.createdAt) {
      const userDate = new Date(u.createdAt);
      return userDate >= today;
    }
    return false;
  }).length;
});

// 过滤用户
const filteredUsers = computed(() => {
  let result = users.value;

  // 角色筛选
  if (roleFilter.value === 'admin') {
    result = result.filter(u => isAdmin(u));
  } else if (roleFilter.value === 'user') {
    result = result.filter(u => !isAdmin(u));
  }

  // 搜索关键词筛选
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(u =>
      u.username.toLowerCase().includes(keyword) ||
      u.email.toLowerCase().includes(keyword) ||
      (u.displayName && u.displayName.toLowerCase().includes(keyword))
    );
  }

  return result;
});

// 判断是否是管理员
const isAdmin = (user) => {
  return user.roles && user.roles.some(r =>
    r.name === 'ROLE_ADMIN' || r.name === 'ADMIN'
  );
};

// 获取用户头像
const getUserAvatar = (user) => {
  if (user.avatarUrl) {
    return user.avatarUrl;
  }
  const name = user.displayName || user.username;
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=007bff&color=fff&size=100`;
};

// 搜索处理
const handleSearch = () => {
  // 搜索是响应式的，不需要额外处理
};

// 返回主页
const goHome = () => {
  router.push({ name: 'home' });
};

// 发送消息
const sendMessage = (user) => {
  // 触发聊天气泡并选择用户
  window.dispatchEvent(new CustomEvent('open-chat', {
    detail: {
      username: user.username,
      displayName: user.displayName || user.username,
      userId: user.id
    }
  }));
};

// 跳转到用户商品页面
const viewUserProducts = (user) => {
  router.push({
    name: 'user-products',
    params: { userId: user.id },
    query: { username: user.username, displayName: user.displayName }
  });
};

// 删除用户
const deleteUser = async (user) => {
  if (!confirm(`确定要删除用户 ${user.username} 吗？此操作不可恢复，该用户的所有数据都将被删除。`)) {
    return;
  }

  try {
    await axios.delete(`/api/admin/users/${user.id}`);
    toast('用户删除成功', 'success');
    fetchUsers();
  } catch (e) {
    console.error('删除用户失败:', e);
    toast('删除用户失败，请重试', 'error');
  }
};

// 打开修改用户名弹窗
const openEditUsername = (user) => {
  editingUser.value = user;
  editUsername.value = user.username;
  showEditDialog.value = true;
};

// 关闭弹窗
const closeEditDialog = () => {
  showEditDialog.value = false;
  editUsername.value = '';
  editingUser.value = null;
};

// 确认修改用户名
const confirmEditUsername = async () => {
  if (!editUsername.value.trim()) return;
  try {
    await axios.post(`/api/admin/users/${editingUser.value.id}/edit-username`, {
      username: editUsername.value
    });
    toast('用户名修改成功', 'success');
    fetchUsers();
    closeEditDialog();
  } catch (e) {
    toast('用户名修改失败', 'error');
  }
};

onMounted(fetchUsers);
</script>

<style scoped>
.admin-users {
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
  position: relative;
}

.home-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.header-content {
  text-align: center;
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

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 40px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

/* 搜索和筛选 */
.search-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-bar {
  position: relative;
  margin-bottom: 15px;
}

.search-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
}

.search-input {
  width: 100%;
  padding: 12px 12px 12px 45px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
}

.filter-buttons {
  display: flex;
  gap: 10px;
}

.filter-btn {
  padding: 8px 16px;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.filter-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.filter-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

/* 用户容器 */
.users-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-height: 400px;
}

/* 加载/错误/空状态 */
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

/* 用户网格 */
.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.user-card {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
}

.user-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #667eea;
}

.user-badges {
  display: flex;
  gap: 5px;
}

.badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge-admin {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.badge-user {
  background: #e0e0e0;
  color: #666;
}

.user-body {
  margin-bottom: 15px;
}

.user-name {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.user-username {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #667eea;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.info-icon {
  font-size: 16px;
}

.info-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
  padding: 10px;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.action-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.action-btn.primary:hover {
  transform: scale(1.05);
}

.action-btn.danger {
  background: #ff4444;
  color: white;
  border-color: transparent;
}

.action-btn.danger:hover {
  background: #cc0000;
  transform: scale(1.05);
}

/* 修改用户名弹窗 */
.edit-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(5px);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.edit-dialog {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  min-width: 450px;
  max-width: 500px;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.edit-dialog h2 {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 700;
  color: #333;
  text-align: center;
}

.edit-dialog-header {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
  border-radius: 12px;
  margin-bottom: 8px;
}

.edit-dialog-label {
  font-weight: 600;
  color: #666;
  font-size: 14px;
}

.edit-dialog-current {
  font-weight: 700;
  color: #667eea;
  background: white;
  border-radius: 8px;
  padding: 6px 16px;
  border: 2px solid #667eea;
  font-size: 15px;
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-dialog-divider {
  height: 2px;
  background: linear-gradient(90deg, transparent, #e0e0e0, transparent);
  margin: 20px 0;
}

.edit-input-group {
  margin-bottom: 28px;
}

.edit-input-label {
  display: block;
  font-weight: 600;
  color: #333;
  font-size: 14px;
  margin-bottom: 10px;
}

.edit-input {
  width: 100%;
  padding: 14px 18px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 16px;
  outline: none;
  transition: all 0.3s;
  box-sizing: border-box;
  font-family: inherit;
}

.edit-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  background: #fafbff;
}

.edit-dialog-tip {
  font-size: 13px;
  color: #999;
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.edit-dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-btn,
.cancel-btn {
  flex: 1;
  padding: 14px 24px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.confirm-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.cancel-btn {
  background: white;
  color: #666;
  border: 2px solid #e0e0e0;
}

.cancel-btn:hover {
  background: #f5f5f5;
  border-color: #ccc;
  transform: translateY(-2px);
}

/* 响应式 */
@media (max-width: 768px) {
  .users-grid {
    grid-template-columns: 1fr;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .filter-buttons {
    flex-wrap: wrap;
  }
}
</style>
