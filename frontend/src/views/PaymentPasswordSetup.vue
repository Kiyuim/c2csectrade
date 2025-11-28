<template>
  <div class="payment-password-setup">
    <div class="setup-container">
      <div class="setup-header">
        <h2>{{ isUpdate ? '修改支付密码' : '设置支付密码' }}</h2>
        <p class="subtitle">{{ isUpdate ? '为了您的账户安全，请验证原密码' : '设置6位数字支付密码，用于支付验证' }}</p>
      </div>

      <div class="setup-form">
        <div class="form-group" v-if="isUpdate">
          <label>原支付密码</label>
          <input
            type="password"
            v-model="oldPassword"
            maxlength="6"
            placeholder="请输入原支付密码"
            class="password-input"
          />
        </div>

        <div class="form-group">
          <label>{{ isUpdate ? '新' : '' }}支付密码</label>
          <input
            type="password"
            v-model="password"
            maxlength="6"
            placeholder="请输入6位数字密码"
            class="password-input"
          />
        </div>

        <div class="form-group">
          <label>确认密码</label>
          <input
            type="password"
            v-model="confirmPassword"
            maxlength="6"
            placeholder="请再次输入密码"
            class="password-input"
          />
        </div>

        <div class="password-tips">
          <p>💡 温馨提示：</p>
          <ul>
            <li>支付密码必须是6位数字</li>
            <li>请不要使用过于简单的密码（如123456）</li>
            <li>请妥善保管您的支付密码</li>
          </ul>
        </div>

        <div class="button-group">
          <button class="btn btn-cancel" @click="goBack">取消</button>
          <button class="btn btn-primary" @click="submitPassword" :disabled="loading">
            {{ loading ? '处理中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import toast from '@/utils/toast';
import Swal from 'sweetalert2';

const router = useRouter();
const isUpdate = ref(false);
const oldPassword = ref('');
const password = ref('');
const confirmPassword = ref('');
const loading = ref(false);

const checkPaymentPasswordStatus = async () => {
  try {
    const response = await axios.get('/api/users/payment-password/check');
    isUpdate.value = response.data.hasPaymentPassword;
  } catch (error) {
    console.error('检查支付密码状态失败:', error);
  }
};

const submitPassword = async () => {
  // 验证输入
  if (isUpdate.value && !oldPassword.value) {
    toast.warning('请输入原支付密码');
    return;
  }

  if (!password.value) {
    toast.warning('请输入支付密码');
    return;
  }

  if (!/^\d{6}$/.test(password.value)) {
    toast.warning('支付密码必须是6位数字');
    return;
  }

  if (password.value !== confirmPassword.value) {
    toast.warning('两次输入的密码不一致');
    return;
  }

  // 简单密码检查
  const weakPasswords = ['123456', '000000', '111111', '222222', '333333', '444444', '555555', '666666', '777777', '888888', '999999'];
  if (weakPasswords.includes(password.value)) {
    const result = await Swal.fire({
      title: '您输入的密码过于简单',
      text: "容易被破解。确定要使用吗？",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });

    if (!result.isConfirmed) {
      return;
    }
  }

  loading.value = true;

  try {
    if (isUpdate.value) {
      // 修改支付密码
      await axios.put('/api/users/payment-password/update', {
        oldPassword: oldPassword.value,
        newPassword: password.value,
        confirmPassword: confirmPassword.value
      });
      toast.success('支付密码修改成功');
    } else {
      // 设置支付密码
      await axios.post('/api/users/payment-password/set', {
        password: password.value,
        confirmPassword: confirmPassword.value
      });
      toast.success('支付密码设置成功');
    }

    // 清空输入
    oldPassword.value = '';
    password.value = '';
    confirmPassword.value = '';

    // 重新检查状态，确保更新成功
    await checkPaymentPasswordStatus();

    // 检查是否需要返回支付页面
    const returnToPaymentOrderId = sessionStorage.getItem('returnToPayment');

    // 延迟跳转，确保后端状态已完全同步（增加延迟时间到1秒）
    setTimeout(() => {
      if (returnToPaymentOrderId) {
        // 清除标记
        sessionStorage.removeItem('returnToPayment');
        // 返回支付页面
        router.push(`/payment/${returnToPaymentOrderId}`);
      } else {
        // 返回上一页
        router.back();
      }
    }, 1000);
  } catch (error) {
    console.error('操作失败:', error);
    if (error.response?.data?.message) {
      toast.error(error.response.data.message);
    } else {
      toast.error('操作失败，请重试');
    }
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  checkPaymentPasswordStatus();
});
</script>

<style scoped>
.payment-password-setup {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.setup-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
  padding: 40px;
}

.setup-header {
  text-align: center;
  margin-bottom: 30px;
}

.setup-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.setup-form {
  margin-top: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.password-input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s;
  letter-spacing: 4px;
}

.password-input:focus {
  outline: none;
  border-color: #667eea;
}

.password-tips {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  margin: 20px 0;
}

.password-tips p {
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
}

.password-tips ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.password-tips li {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
  padding-left: 20px;
  position: relative;
}

.password-tips li:before {
  content: '•';
  position: absolute;
  left: 0;
  color: #667eea;
}

.button-group {
  display: flex;
  gap: 15px;
  margin-top: 30px;
}

.btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel {
  background: #f0f0f0;
  color: #666;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 576px) {
  .setup-container {
    padding: 30px 20px;
  }

  .button-group {
    flex-direction: column;
  }
}
</style>

