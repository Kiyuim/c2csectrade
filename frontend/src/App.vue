<template>
  <div id="app">
    <nav class="navbar" v-if="isLoggedIn">
      <div class="nav-content">
        <!-- 左侧用户头像下拉菜单 -->
        <div class="user-menu" @click="toggleUserMenu">
          <img :src="userAvatar" :alt="userDisplayName" class="user-avatar" />
          <div class="user-name-section">
            <span class="username">{{ userDisplayName }}</span>
            <CreditBadge v-if="authStore.user?.creditLevel" :level="authStore.user.creditLevel" />
          </div>
          <div v-if="showUserMenu" class="dropdown-menu" @click.stop>
            <div class="dropdown-item" @click="changeAvatar">
              <span class="icon">👤</span> 更换头像
            </div>
            <div class="dropdown-item" @click="changeDisplayName">
              <span class="icon">✏️</span> 修改用户名
            </div>
            <div v-if="isAdmin" class="dropdown-divider"></div>
            <div v-if="isAdmin" class="dropdown-item" @click="goToAdminUsers">
              <span class="icon">👥</span> 用户管理
            </div>
            <div v-if="isAdmin" class="dropdown-item" @click="goToAdminChat">
              <span class="icon">📢</span> 系统消息
            </div>
            <div v-if="isAdmin" class="dropdown-item" @click="goToAdminReports">
              <span class="icon">🚩</span> 举报管理
            </div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item logout" @click="handleLogout">
              <span class="icon">🚪</span> 退出登录
            </div>
          </div>
        </div>
      </div>
    </nav>
    <router-view />
    <!-- 左下角发布商品浮动按钮 -->
    <FloatingPublishButton v-if="isLoggedIn" />
    <!-- 左下角管理商品浮动按钮 -->
    <FloatingManageProductsButton v-if="isLoggedIn" />
    <!-- 右下角聊天气泡按钮 -->
    <FloatingChatButton v-if="isLoggedIn" />
    <!-- 收藏夹浮动按钮 -->
    <FloatingFavoritesButton v-if="isLoggedIn" />
    <!-- 购物车浮动按钮 -->
    <FloatingCartButton v-if="isLoggedIn" />
    <!-- 举报管理浮动按钮（仅管理员可见） -->
    <FloatingReportsButton v-if="isLoggedIn && isAdmin" />

    <!-- 系统消息通知 -->
    <SystemMessageNotification
      v-for="(notification, index) in systemNotifications"
      :key="notification.id"
      :message="notification.message"
      :duration="5000"
      :style="{ top: `${80 + index * 120}px` }"
      @close="removeNotification(notification.id)"
    />

    <!-- 头像更换对话框 -->
    <div v-if="showAvatarDialog" class="avatar-dialog-overlay" @click="closeAvatarDialog">
      <div class="avatar-dialog" @click.stop>
        <div class="avatar-dialog-header">
          <h3>更换头像</h3>
          <button @click="closeAvatarDialog" class="close-dialog">×</button>
        </div>
        <div class="avatar-dialog-body">
          <div class="avatar-option">
            <label for="avatarFileInput" class="avatar-upload-btn">
              📁 上传图片
            </label>
            <input
              id="avatarFileInput"
              type="file"
              accept="image/*"
              @change="handleAvatarUpload"
              style="display: none;"
            />
          </div>
          <div class="divider">或</div>
          <div class="avatar-option">
            <input
              v-model="avatarUrlInput"
              type="text"
              placeholder="输入图片URL"
              class="avatar-url-input"
            />
            <button @click="setAvatarByUrl" class="avatar-url-btn">设置头像</button>
          </div>
          <div class="avatar-option">
            <button @click="resetToDefaultAvatar" class="avatar-default-btn">
              恢复默认头像
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户名修改对话框 -->
    <div v-if="showDisplayNameDialog" class="avatar-dialog-overlay" @click="closeDisplayNameDialog">
      <div class="avatar-dialog" @click.stop>
        <div class="avatar-dialog-header">
          <h3>修改用户名</h3>
          <button @click="closeDisplayNameDialog" class="close-dialog">×</button>
        </div>
        <div class="avatar-dialog-body">
          <div class="inline-form-group">
            <label class="form-label">
              <span class="label-icon">👤</span>
              当前用户名
            </label>
            <span class="name-badge">{{ userDisplayName }}</span>
          </div>
          <div class="inline-form-group">
            <label class="form-label">
              <span class="label-icon">✏️</span>
              新用户名
            </label>
            <input
              v-model="newDisplayName"
              type="text"
              placeholder="输入新用户名"
              class="avatar-url-input inline-input"
              maxlength="20"
            />
          </div>
          <div class="name-rules">
            <div class="rule-item">
              <span class="rule-icon">📏</span>
              <span>长度: 2-20个字符</span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">🚫</span>
              <span>不包含敏感词汇</span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">✨</span>
              <span>不与其他用户重复</span>
            </div>
          </div>
          <div class="centered-actions">
            <button @click="updateDisplayName" class="avatar-url-btn" :disabled="isUpdatingName">
              <span v-if="isUpdatingName" class="spinner-small"></span>
              {{ isUpdatingName ? '修改中...' : '确认修改' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/store/auth';
import { useRouter } from 'vue-router';
import axios from 'axios';
import emitter from '@/eventBus';
import FloatingChatButton from '@/components/FloatingChatButton.vue';
import FloatingPublishButton from '@/components/FloatingPublishButton.vue';
import FloatingFavoritesButton from '@/components/FloatingFavoritesButton.vue';
import FloatingCartButton from '@/components/FloatingCartButton.vue';
import FloatingManageProductsButton from '@/components/FloatingManageProductsButton.vue';
import FloatingReportsButton from '@/components/FloatingReportsButton.vue';
import SystemMessageNotification from '@/components/SystemMessageNotification.vue';
import CreditBadge from '@/components/CreditBadge.vue';

const authStore = useAuthStore();
const router = useRouter();
const showUserMenu = ref(false);
const systemNotifications = ref([]);
let notificationId = 0;

const isLoggedIn = computed(() => authStore.isLoggedIn);
const currentUser = computed(() => authStore.user);
const isAdmin = computed(() => currentUser.value?.role === 'ROLE_ADMIN');

const userAvatar = computed(() => {
  if (currentUser.value?.avatarUrl) {
    return currentUser.value.avatarUrl;
  }
  const displayName = currentUser.value?.displayName || currentUser.value?.username || 'User';
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=007bff&color=fff&size=100`;
});

const userDisplayName = computed(() => {
  return currentUser.value?.displayName || currentUser.value?.username || 'User';
});

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value;
};

const goToAdminUsers = () => {
  router.push('/admin/users');
  showUserMenu.value = false;
};

const goToAdminChat = () => {
  router.push('/admin/chat');
  showUserMenu.value = false;
};

const goToAdminReports = () => {
  router.push('/admin/reports');
  showUserMenu.value = false;
};

const handleLogout = () => {
  authStore.logout();
  showUserMenu.value = false;
  router.push('/login');
};

// 头像相关
const showAvatarDialog = ref(false);
const avatarUrlInput = ref('');

const changeAvatar = () => {
  showAvatarDialog.value = true;
  showUserMenu.value = false;
  avatarUrlInput.value = '';
};

const closeAvatarDialog = () => {
  showAvatarDialog.value = false;
  avatarUrlInput.value = '';
};

import toast from '@/utils/toast';
import Swal from 'sweetalert2';

// ... imports

// ...

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    toast.warning('请选择图片文件');
    return;
  }

  // 验证文件大小 (5MB)
  if (file.size > 5 * 1024 * 1024) {
    toast.warning('图片大小不能超过5MB');
    return;
  }

  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await axios.post('/api/users/avatar/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    if (response.data.avatarUrl) {
      authStore.updateUser({ avatarUrl: response.data.avatarUrl });
      toast.success('头像上传成功！');
      closeAvatarDialog();
    }
  } catch (error) {
    console.error('上传头像失败:', error);
    toast.error(error.response?.data || '上传失败，请重试');
  }
};

const setAvatarByUrl = async () => {
  if (!avatarUrlInput.value.trim()) {
    toast.warning('请输入图片URL');
    return;
  }

  try {
    const response = await axios.post('/api/users/avatar/url', {
      avatarUrl: avatarUrlInput.value.trim()
    });

    if (response.data.avatarUrl) {
      authStore.updateUser({ avatarUrl: response.data.avatarUrl });
      toast.success('头像设置成功！');
      closeAvatarDialog();
    }
  } catch (error) {
    console.error('设置头像失败:', error);
    toast.error(error.response?.data || '设置失败，请重试');
  }
};

const resetToDefaultAvatar = async () => {
  const result = await Swal.fire({
    title: '确定要恢复默认头像吗？',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#3085d6',
    cancelButtonColor: '#d33',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  });

  if (!result.isConfirmed) return;

  try {
    await axios.post('/api/users/avatar/reset');
    authStore.updateUser({ avatarUrl: null });
    toast.success('已恢复默认头像');
    closeAvatarDialog();
  } catch (error) {
    console.error('重置头像失败:', error);
    toast.error(error.response?.data || '重置失败，请重试');
  }
};

// 用户名相关
const showDisplayNameDialog = ref(false);
const newDisplayName = ref('');
const isUpdatingName = ref(false);

const changeDisplayName = () => {
  showDisplayNameDialog.value = true;
  showUserMenu.value = false;
  newDisplayName.value = '';
};

const closeDisplayNameDialog = () => {
  showDisplayNameDialog.value = false;
  newDisplayName.value = '';
  isUpdatingName.value = false;
};

const updateDisplayName = async () => {
  const trimmedName = newDisplayName.value.trim();

  if (!trimmedName) {
    toast.warning('用户名不能为空');
    return;
  }

  if (trimmedName.length < 2 || trimmedName.length > 20) {
    toast.warning('用户名长度必须在2-20个字符之间');
    return;
  }

  isUpdatingName.value = true;

  try {
    const response = await axios.put('/api/users/display-name', {
      displayName: trimmedName
    });

    if (response.data.displayName) {
      authStore.updateUser({ displayName: response.data.displayName });
      toast.success('用户名修改成功！');
      closeDisplayNameDialog();
    }
  } catch (error) {
    console.error('修改用户名失败:', error);
    const errorMsg = error.response?.data?.message || error.response?.data || '修改失败，请重试';
    toast.error(errorMsg);
  } finally {
    isUpdatingName.value = false;
  }
};

// Close dropdown when clicking outside
const handleClickOutside = (event) => {
  if (!event.target.closest('.user-menu')) {
    showUserMenu.value = false;
  }
};

if (typeof window !== 'undefined') {
  window.addEventListener('click', handleClickOutside);
}

// 系统消息通知处理
const addSystemNotification = (message) => {
  const id = ++notificationId;
  systemNotifications.value.push({
    id,
    message
  });
};

const removeNotification = (id) => {
  const index = systemNotifications.value.findIndex(n => n.id === id);
  if (index > -1) {
    systemNotifications.value.splice(index, 1);
  }
};

// 监听系统消息
const handleSystemMessage = (message) => {
  console.log('[App] 收到消息:', message);
  console.log('[App] 是系统消息:', message.isSystemMessage);
  console.log('[App] 接收者:', message.recipient);
  console.log('[App] 当前用户:', currentUser.value?.username);

  // 只显示发给当前用户的系统消息
  if (message.isSystemMessage && message.recipient === currentUser.value?.username) {
    console.log('[App] ✅ 显示系统消息通知');
    addSystemNotification(message.content);
    // 播放通知音效（可选）
    // new Audio('/notification.mp3').play().catch(() => {});
  } else if (message.isSystemMessage) {
    console.log('[App] ⚠️ 系统消息不是发给当前用户的，忽略');
  }
};

// 检查并显示未读系统消息
const checkUnreadSystemMessages = async () => {
  if (!isLoggedIn.value) return;

  try {
    const token = localStorage.getItem('jwt_token');
    if (!token) return;

    // 获取未读系统消息
    const response = await axios.get('/api/chat/history/系统', {
      headers: { 'Authorization': `Bearer ${token}` },
      params: { limit: 10 }
    });

    // 只显示未读的系统消息
    const unreadSystemMessages = response.data.filter(msg => !msg.isRead && msg.isSystemMessage);

    // 从 localStorage 获取已显示过的消息 ID 列表
    const displayedMessagesKey = `displayed_system_messages_${authStore.user?.username || 'guest'}`;
    const displayedMessageIds = JSON.parse(localStorage.getItem(displayedMessagesKey) || '[]');
    
    // 过滤出尚未显示过的消息（使用 timestamp + content 作为唯一标识）
    const newMessages = unreadSystemMessages.filter(msg => {
      const msgId = `${msg.timestamp}_${msg.content.substring(0, 50)}`; // 使用时间戳和内容前50字符作为唯一标识
      return !displayedMessageIds.includes(msgId);
    });

    // 为每条新消息显示通知
    newMessages.forEach((msg, index) => {
      setTimeout(() => {
        addSystemNotification(msg.content);
        // 记录已显示的消息唯一标识
        const msgId = `${msg.timestamp}_${msg.content.substring(0, 50)}`;
        displayedMessageIds.push(msgId);
        localStorage.setItem(displayedMessagesKey, JSON.stringify(displayedMessageIds));
      }, index * 500); // 每条消息间隔500ms显示，避免重叠
    });

    if (newMessages.length > 0) {
      console.log(`[App] 显示 ${newMessages.length} 条新的未读系统消息`);
    }
  } catch (error) {
    console.error('[App] 检查未读系统消息失败:', error);
  }
};

onMounted(() => {
  // 监听全局系统消息事件
  emitter.on('chat-message', handleSystemMessage);

  // 如果用户已登录，检查未读系统消息
  if (isLoggedIn.value) {
    checkUnreadSystemMessages();
  }
});

onUnmounted(() => {
  emitter.off('chat-message', handleSystemMessage);
});

</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.navbar {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid #e0e0e0;
}

.nav-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 24px;
  transition: background-color 0.2s;
  position: relative;
}

.user-menu:hover {
  background-color: #f5f5f5;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  border: 2px solid #e0e0e0;
}

.user-name-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.dropdown-menu {
  position: absolute;
  top: 60px;
  left: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  padding: 8px 0;
  z-index: 1000;
  border: 1px solid #e0e0e0;
}

.dropdown-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #333;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-item.logout {
  color: #dc3545;
}

.dropdown-item .icon {
  font-size: 16px;
}

.dropdown-divider {
  height: 1px;
  background-color: #e0e0e0;
  margin: 4px 0;
}

.auth-buttons {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 20px;
  border-radius: 6px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
}

.btn-login {
  color: #007bff;
  background: transparent;
  border: 1px solid #007bff;
}

.btn-login:hover {
  background: #007bff;
  color: white;
}

.btn-register {
  background: #007bff;
  color: white;
}

.btn-register:hover {
  background: #0056b3;
}

/* 头像对话框样式 */
.avatar-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.avatar-dialog {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.avatar-dialog-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.avatar-dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-dialog {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-dialog:hover {
  background: #f5f5f5;
  color: #333;
}

.avatar-dialog-body {
  padding: 20px;
}

.avatar-option {
  margin-bottom: 15px;
}

.avatar-upload-btn {
  display: block;
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.avatar-upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.divider {
  text-align: center;
  color: #999;
  margin: 20px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40%;
  height: 1px;
  background: #e0e0e0;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.avatar-url-input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 10px;
  transition: border-color 0.3s;
}

.avatar-url-input:focus {
  outline: none;
  border-color: #667eea;
}

.avatar-url-btn,
.avatar-default-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-url-btn {
  background: #28a745;
  color: white;
}

.avatar-url-btn:hover {
  background: #218838;
  transform: translateY(-1px);
}

.avatar-default-btn {
  background: #f8f9fa;
  color: #333;
  border: 1px solid #dee2e6;
}

.avatar-default-btn:hover {
  background: #e9ecef;
}

/* 用户名修改对话框样式调整 */
.inline-form-group {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}

.inline-form-group .form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  white-space: nowrap;
  font-weight: 500;
  color: #333;
}

.inline-form-group .label-icon {
  font-size: 16px;
}

.inline-form-group .inline-input {
  flex: 1;
  margin: 0;
}

.name-badge {
  display: inline-block;
  padding: 6px 12px;
  background: #f8f9fb;
  border: 1px solid #e8e9ee;
  border-radius: 8px;
  color: #333;
  font-weight: 600;
}

.name-rules {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #666;
  font-size: 13px;
}
.rule-item { display: flex; gap: 8px; align-items: center; }
.rule-icon { font-size: 14px; }
.centered-actions { display:flex; gap:12px; justify-content:center; margin-top:10px; }
.spinner-small { width:16px; height:16px; border:2px solid #f3f3f3; border-top:2px solid #667eea; border-radius:50%; animation: spin 1s linear infinite; display:inline-block; margin-right:8px; }
@keyframes spin { from{transform:rotate(0deg);} to{transform:rotate(360deg);} }
</style>
