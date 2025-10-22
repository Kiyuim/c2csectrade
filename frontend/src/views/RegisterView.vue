<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <h1>🛒 加入我们</h1>
          <p class="subtitle">注册账户，开始交易二手物品</p>
        </div>

        <form @submit.prevent="handleRegister" class="register-form">
          <div class="form-group">
            <label for="username">
              <span class="icon">👤</span> 用户名
            </label>
            <input
              type="text"
              id="username"
              v-model="username"
              placeholder="请输入用户名（用于登录）"
              required
            />
          </div>

          <div class="form-group">
            <label for="displayName">
              <span class="icon">✨</span> 显示名称
            </label>
            <input
              type="text"
              id="displayName"
              v-model="displayName"
              placeholder="请输入显示名称（可选，用于展示）"
            />
          </div>

          <div class="form-group">
            <label for="email">
              <span class="icon">📧</span> 邮箱
            </label>
            <input
              type="email"
              id="email"
              v-model="email"
              placeholder="请输入邮箱地址"
              required
            />
          </div>

          <div class="form-group">
            <label for="password">
              <span class="icon">🔒</span> 密码
            </label>
            <input
              type="password"
              id="password"
              v-model="password"
              placeholder="请输入密码（至少6位）"
              required
              minlength="6"
            />
          </div>

          <div class="form-group">
            <label for="captcha">
              <span class="icon">🔢</span> 验证码
            </label>
            <div class="captcha-wrapper">
              <input
                type="text"
                id="captcha"
                v-model="captchaCode"
                placeholder="请输入验证码"
                required
              />
              <img
                :src="captchaUrl"
                alt="验证码"
                @click="refreshCaptcha"
                class="captcha-image"
                title="点击刷新验证码"
              />
            </div>
          </div>

          <button type="submit" class="register-btn" :disabled="loading">
            <span v-if="!loading">✨ 立即注册</span>
            <span v-else>⏳ 注册中...</span>
          </button>

          <div v-if="message" :class="['message', successful ? 'success-message' : 'error-message']">
            {{ successful ? '✅' : '⚠️' }} {{ message }}
          </div>
        </form>

        <div class="register-footer">
          <p>已有账户？ <router-link to="/login" class="login-link">立即登录</router-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/store/auth';
import { useRouter } from 'vue-router';

const username = ref('');
const displayName = ref('');
const email = ref('');
const password = ref('');
const loading = ref(false);
const successful = ref(false);
const message = ref('');
const captchaCode = ref('');
const captchaId = ref('');
const captchaUrl = ref('');

const authStore = useAuthStore();
const router = useRouter();

const refreshCaptcha = async () => {
  try {
    const timestamp = new Date().getTime();
    const response = await fetch(`/api/captcha/generate?t=${timestamp}`, {
      method: 'GET',
      credentials: 'include'
    });

    if (!response.ok) {
      throw new Error('验证码获取失败');
    }

    const captchaIdFromHeader = response.headers.get('Captcha-ID');
    if (captchaIdFromHeader) {
      captchaId.value = captchaIdFromHeader;
    }

    const blob = await response.blob();
    if (captchaUrl.value) {
      URL.revokeObjectURL(captchaUrl.value);
    }
    captchaUrl.value = URL.createObjectURL(blob);
    captchaCode.value = '';
  } catch (error) {
    console.error('Failed to load captcha:', error);
    message.value = '验证码加载失败，请点击图片刷新重试';
  }
};

onMounted(() => {
  refreshCaptcha();
});

const handleRegister = async () => {
  loading.value = true;
  message.value = '';
  successful.value = false;

  try {
    await authStore.register({
      username: username.value,
      displayName: displayName.value,
      email: email.value,
      password: password.value,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value
    });

    successful.value = true;
    message.value = '✅ 注册成功！即将跳转到登录页面...';

    setTimeout(() => {
      router.push('/login');
    }, 2000);
  } catch (error) {
    console.error('注册失败:', error);
    successful.value = false;

    // 详细解析错误信息，只设置一次message
    const status = error?.response?.status;
    const errorData = error?.response?.data;

    if (status === 400) {
      // 400错误 - 验证码或参数错误
      if (typeof errorData === 'string') {
        if (errorData.includes('验证码')) {
          message.value = '❌ 验证码错误或已过期，请重新输入';
        } else if (errorData.includes('已存在') || errorData.includes('已注册')) {
          message.value = '❌ 用户名或邮箱已被注册';
        } else if (errorData.includes('用户名') || errorData.includes('密码') || errorData.includes('邮箱')) {
          message.value = '❌ ' + errorData;
        } else {
          message.value = '❌ 注册信息有误，请检查后重试';
        }
      } else {
        message.value = '❌ 注册信息格式错误，请检查后重试';
      }
    } else if (status === 409) {
      message.value = '❌ 用户名或邮箱已被注册';
    } else if (status === 500) {
      message.value = '❌ 服务器错误，请稍后重试';
    } else if (typeof errorData === 'string' && errorData && !errorData.includes('status code')) {
      message.value = '❌ ' + errorData;
    } else {
      message.value = '❌ 注册失败，请稍后重试';
    }

    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 450px;
}

.register-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px;
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
  font-weight: 600;
}

.subtitle {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.register-form {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.form-group label .icon {
  margin-right: 5px;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.captcha-wrapper {
  display: flex;
  gap: 10px;
  align-items: stretch;
}

.captcha-wrapper input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 46px;
  border-radius: 10px;
  cursor: pointer;
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
  object-fit: cover;
}

.captcha-image:hover {
  border-color: #667eea;
  transform: scale(1.05);
}

.register-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.register-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message {
  margin-top: 15px;
  padding: 12px;
  border-radius: 10px;
  font-size: 14px;
  text-align: center;
}

.success-message {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
}

.error-message {
  background: #fee;
  border: 1px solid #fcc;
  color: #c33;
}

.register-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.register-footer p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.login-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.login-link:hover {
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-card {
    padding: 30px 20px;
  }

  .register-header h1 {
    font-size: 24px;
  }

  .captcha-wrapper {
    flex-direction: column;
  }

  .captcha-image {
    width: 100%;
  }
}
</style>
