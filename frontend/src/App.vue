<template>
  <div id="app">
    <nav class="navbar" v-if="isLoggedIn">
      <div class="nav-content">
        <!-- 左侧用户头像下拉菜单 -->
        <div class="user-menu" @click="toggleUserMenu">
          <img :src="userAvatar" :alt="userDisplayName" class="user-avatar" />
          <span class="username">{{ userDisplayName }}</span>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/store/auth';
import { useRouter } from 'vue-router';
import FloatingChatButton from '@/components/FloatingChatButton.vue';
import FloatingPublishButton from '@/components/FloatingPublishButton.vue';
import FloatingFavoritesButton from '@/components/FloatingFavoritesButton.vue';
import FloatingCartButton from '@/components/FloatingCartButton.vue';
import FloatingManageProductsButton from '@/components/FloatingManageProductsButton.vue';

const authStore = useAuthStore();
const router = useRouter();
const showUserMenu = ref(false);

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

const closeUserMenu = () => {
  showUserMenu.value = false;
};

const changeDisplayName = () => {
  // 创建用户名修改对话框
  const dialog = document.createElement('div');
  dialog.className = 'avatar-dialog-overlay';
  dialog.innerHTML = `
    <div class="avatar-dialog name-dialog">
      <div class="avatar-dialog-header">
        <h3>✏️ 修改显示名称</h3>
        <button class="close-dialog">&times;</button>
      </div>
      <div class="avatar-dialog-body">
        <div class="form-group inline-form-group">
          <label class="form-label">
            <span class="label-icon">👤</span>
            当前用户名：
          </label>
          <span class="name-badge">${userDisplayName.value}</span>
        </div>

        <div class="form-group inline-form-group">
          <label for="newDisplayName" class="form-label">
            <span class="label-icon">✨</span>
            新用户名：
          </label>
          <input
            type="text"
            id="newDisplayName"
            class="avatar-url-input inline-input"
            placeholder="请输入新的显示名称"
            maxlength="20"
          />
          <div class="name-rules">
            <div class="rule-item">
              <span class="rule-icon">📏</span>
              <span>长度：2-20个字符</span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">🔒</span>
              <span>不能包含敏感词汇</span>
            </div>
            <div class="rule-item">
              <span class="rule-icon">✅</span>
              <span>不能与其他用户重复</span>
            </div>
          </div>
        </div>

        <div class="form-actions centered-actions">
          <button class="btn-confirm avatar-url-btn">
            <span class="btn-icon">✓</span>
            确认修改
          </button>
          <button class="btn-cancel avatar-default-btn">
            <span class="btn-icon">✕</span>
            取消
          </button>
        </div>

        <div class="status-messages">
          <div class="loading-message" style="display: none;">
            <div class="spinner-small"></div>
            <span>正在验证...</span>
          </div>
          <div class="error-message" style="display: none;"></div>
        </div>
      </div>
    </div>
  `;

  document.body.appendChild(dialog);
  const nameInput = dialog.querySelector('#newDisplayName');
  nameInput.focus();

  // 关闭对话框
  const closeDialog = () => {
    document.body.removeChild(dialog);
    showUserMenu.value = false;
  };

  dialog.querySelector('.close-dialog').onclick = closeDialog;
  dialog.querySelector('.btn-cancel').onclick = closeDialog;
  dialog.onclick = (e) => {
    if (e.target === dialog) closeDialog();
  };

  // 确认修改
  dialog.querySelector('.btn-confirm').onclick = async () => {
    const newName = nameInput.value.trim();
    const loadingMsg = dialog.querySelector('.loading-message');
    const errorMsg = dialog.querySelector('.error-message');

    // 重置消息
    loadingMsg.style.display = 'none';
    errorMsg.style.display = 'none';

    if (!newName) {
      errorMsg.textContent = '请输入新用户名';
      errorMsg.style.display = 'block';
      return;
    }

    if (newName.length < 2 || newName.length > 20) {
      errorMsg.textContent = '用户名长度必须在2-20个字符之间';
      errorMsg.style.display = 'block';
      return;
    }

    if (newName === userDisplayName.value) {
      errorMsg.textContent = '新用户名与当前用户名相同';
      errorMsg.style.display = 'block';
      return;
    }

    loadingMsg.style.display = 'block';

    try {
      const token = localStorage.getItem('jwt_token');
      const response = await fetch('/api/user/display-name', {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ displayName: newName })
      });

      const data = await response.json();

      if (response.ok) {
        // 更新用户名成功（不弹窗）
        const user = JSON.parse(localStorage.getItem('user'));
        user.displayName = newName;
        localStorage.setItem('user', JSON.stringify(user));
        // 更新store中的用户信息
        authStore.updateUser({ displayName: newName });
        closeDialog();
      } else {
        loadingMsg.style.display = 'none';
        errorMsg.textContent = data.message || '修改失败，请重试';
        errorMsg.style.display = 'block';
      }
    } catch (error) {
      console.error('Update display name error:', error);
      loadingMsg.style.display = 'none';
      errorMsg.textContent = '网络错误，请重试';
      errorMsg.style.display = 'block';
    }
  };

  // 回车键确认
  nameInput.onkeypress = (e) => {
    if (e.key === 'Enter') {
      dialog.querySelector('.btn-confirm').click();
    }
  };
};

const changeAvatar = () => {
  // 创建一个隐藏的文件上传对话框
  const dialog = document.createElement('div');
  dialog.className = 'avatar-dialog-overlay';
  dialog.innerHTML = `
    <div class="avatar-dialog">
      <div class="avatar-dialog-header">
        <h3>更换头像</h3>
        <button class="close-dialog">&times;</button>
      </div>
      <div class="avatar-dialog-body">
        <div class="avatar-option">
          <label class="avatar-upload-btn">
            📁 选择本地图片上传
            <input type="file" accept="image/*" class="avatar-file-input" style="display: none;">
          </label>
        </div>
        <div class="divider">或</div>
        <div class="avatar-option">
          <input type="text" class="avatar-url-input" placeholder="输入图片URL地址" />
          <button class="avatar-url-btn">🔗 使用URL</button>
        </div>
        <div class="avatar-option">
          <button class="avatar-default-btn">🎨 使用默认头像</button>
        </div>
      </div>
    </div>
  `;

  document.body.appendChild(dialog);

  // 关闭对话框
  const closeDialog = () => {
    document.body.removeChild(dialog);
    showUserMenu.value = false;
  };

  dialog.querySelector('.close-dialog').onclick = closeDialog;
  dialog.onclick = (e) => {
    if (e.target === dialog) closeDialog();
  };

  // 文件上传
  dialog.querySelector('.avatar-file-input').onchange = async (e) => {
    const file = e.target.files[0];
    if (file) {
      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        return;
      }
      // 验证文件大小 (最大5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('图片大小不能超过5MB');
        return;
      }

      try {
        const token = localStorage.getItem('jwt_token');
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch('/api/user/avatar/upload', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          },
          body: formData
        });

        const data = await response.json();
        if (response.ok) {
          // 更新用户信息（不弹窗）
          const user = JSON.parse(localStorage.getItem('user'));
          user.avatarUrl = data.avatarUrl;
          localStorage.setItem('user', JSON.stringify(user));
          // 同步更新authStore
          authStore.updateUser({ avatarUrl: data.avatarUrl });
          closeDialog();
        } else {
          alert('上传失败: ' + (data.message || data));
        }
      } catch (error) {
        console.error('Upload error:', error);
        alert('上传失败，请重试');
      }
      closeDialog();
    }
  };

  // URL输入
  dialog.querySelector('.avatar-url-btn').onclick = async () => {
    const url = dialog.querySelector('.avatar-url-input').value.trim();
    if (url) {
      try {
        const token = localStorage.getItem('jwt_token');
        const response = await fetch('/api/user/avatar/url', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ avatarUrl: url })
        });

        const data = await response.json();
        if (response.ok) {
          // 更新用户信息（不弹窗）
          const user = JSON.parse(localStorage.getItem('user'));
          user.avatarUrl = data.avatarUrl;
          localStorage.setItem('user', JSON.stringify(user));
          // 同步更新authStore
          authStore.updateUser({ avatarUrl: data.avatarUrl });
          closeDialog();
        } else {
          alert('设置失败: ' + (data.message || data));
        }
      } catch (error) {
        console.error('Set avatar error:', error);
        alert('设置失败，请重试');
      }
      closeDialog();
    } else {
      alert('请输入有效的图片URL');
    }
  };

  // 使用默认头像
  dialog.querySelector('.avatar-default-btn').onclick = async () => {
    try {
      const token = localStorage.getItem('jwt_token');
      const response = await fetch('/api/user/avatar/reset', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const data = await response.json();
      if (response.ok) {
        // 更新用户信息（不弹窗）
        const user = JSON.parse(localStorage.getItem('user'));
        user.avatarUrl = null;
        localStorage.setItem('user', JSON.stringify(user));
        // 同步更新authStore
        authStore.updateUser({ avatarUrl: null });
        closeDialog();
      } else {
        alert('重置失败: ' + (data.message || data));
      }
    } catch (error) {
      console.error('Reset avatar error:', error);
      alert('重置失败，请重试');
    }
    closeDialog();
  };
};

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
  showUserMenu.value = false;
};

// 点击外部关闭下拉菜单
const handleClickOutside = (event) => {
  const userMenu = document.querySelector('.user-menu');
  if (userMenu && !userMenu.contains(event.target)) {
    closeUserMenu();
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
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
