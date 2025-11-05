<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1>🐟 闲鱼</h1>
          <p class="subtitle">欢迎回来，请登录您的账户</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="username">
              <span class="icon">👤</span> 用户名
            </label>
            <input
              type="text"
              id="username"
              v-model="username"
              placeholder="请输入用户名"
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
              placeholder="请输入密码"
              required
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

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="!loading">🚀 登录</span>
            <span v-else>⏳ 登录中...</span>
          </button>
        </form>

        <div class="login-footer">
          <p>还没有账户？ <router-link to="/register" class="register-link">立即注册</router-link></p>
          <p>忘记密码？ <router-link to="/forgot-password" class="forgot-link">找回密码</router-link></p>
          <p class="hint">💡 提示：管理员账户会自动识别权限</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/store/auth';
import { useRouter } from 'vue-router';
import { toast } from '@/services/toast';

const username = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
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
    errorMessage.value = ''; // 清除错误消息
  } catch (error) {
    console.error('Failed to load captcha:', error);
    // 不显示错误消息，让验证码区域保持干净
    // 用户可以点击验证码图片重新加载
  }
};

onMounted(() => {
  refreshCaptcha();
});

const handleLogin = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    await authStore.login({
      username: username.value,
      password: password.value,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value,
      isAdmin: false
    });
    router.push('/');
  } catch (error) {
    console.error('登录失败:', error);

    // 详细解析错误信息，只显示一次提示
    const status = error?.response?.status;
    const errorData = error?.response?.data;

    let errorMsg = '';

    if (status === 400) {
      // 400错误 - 验证码或请求参数错误
      if (typeof errorData === 'string') {
        if (errorData.includes('验证码')) {
          errorMsg = '❌ 验证码错误或已过期，请重新输入';
        } else if (errorData.includes('用户名') || errorData.includes('密码')) {
          errorMsg = '❌ 请输入用户名和密码';
        } else {
          errorMsg = '❌ ' + errorData;
        }
      } else {
        errorMsg = '❌ 请求参数错误，请检查输入信息';
      }
    } else if (status === 401) {
      errorMsg = '❌ 用户名或密码错误，请重新输入';
    } else if (status === 403) {
      errorMsg = '❌ 账户已被禁用，请联系管理员';
    } else if (status === 500) {
      errorMsg = '❌ 服务器错误，请稍后重试';
    } else if (typeof errorData === 'string' && errorData && !errorData.includes('status code')) {
      errorMsg = '❌ ' + errorData;
    } else {
      errorMsg = '❌ 登录失败，请检查用户名和密码';
    }

    // 只调用一次toast
    toast(errorMsg, 'error');
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url('@/assets/1.jpg') center center / cover no-repeat;
  padding: 20px;
  position: relative;
}

.login-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 0;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 450px;
}

.login-card {
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

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
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

.login-form {
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

.login-btn {
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

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  margin-top: 15px;
  padding: 12px;
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 10px;
  color: #c33;
  font-size: 14px;
  text-align: center;
}

.login-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.login-footer p {
  margin: 10px 0;
  color: #666;
  font-size: 14px;
}

.register-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.register-link:hover {
  text-decoration: underline;
}

.forgot-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.forgot-link:hover {
  text-decoration: underline;
}

.hint {
  font-size: 12px;
  color: #999;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }

  .login-header h1 {
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
