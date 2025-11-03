<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <h2>举报商品</h2>
      <form @submit.prevent="submitReport">
        <div class="form-group">
          <label for="reason">举报原因</label>
          <select id="reason" v-model="reason" required>
            <option value="">请选择原因</option>
            <option value="INAPPROPRIATE_CONTENT">不当内容</option>
            <option value="SCAM">诈骗</option>
            <option value="SPAM">垃圾信息</option>
            <option value="OTHER">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label for="description">详细描述</label>
          <textarea id="description" v-model="description" rows="4"></textarea>
        </div>
        <div class="form-actions">
          <button type="button" @click="$emit('close')">取消</button>
          <button type="submit">提交</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { toast } from '@/services/toast';

const props = defineProps({
  productId: {
    type: Number,
    required: true,
  },
});

const emit = defineEmits(['close']);

const reason = ref('');
const description = ref('');

const submitReport = async () => {
  if (!reason.value) {
    toast('请选择举报原因', 'warning');
    return;
  }

  try {
    const response = await axios.post('/api/reports', {
      productId: props.productId,
      reason: reason.value,
      description: description.value,
    });
    toast(response.data || '举报成功，管理员将尽快处理', 'success');
    emit('close');
  } catch (error) {
    console.error('举报失败:', error);
    // 显示后端返回的错误消息
    const errorMessage = error.response?.data || '举报失败，请重试';
    toast(errorMessage, 'error');
  }
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
}

.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
