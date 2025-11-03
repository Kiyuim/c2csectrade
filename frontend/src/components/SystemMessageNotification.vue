<template>
  <transition name="notification">
    <div v-if="visible" class="system-message-notification" @click="handleClick">
      <div class="notification-header">
        <span class="notification-icon">🔔</span>
        <span class="notification-title">系统消息</span>
        <button @click.stop="close" class="close-btn">×</button>
      </div>
      <div class="notification-content">{{ message }}</div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const props = defineProps({
  message: {
    type: String,
    required: true
  },
  duration: {
    type: Number,
    default: 5000
  }
});

const emit = defineEmits(['close', 'click']);

const visible = ref(false);

const close = () => {
  visible.value = false;
  setTimeout(() => {
    emit('close');
  }, 300);
};

const handleClick = () => {
  emit('click');
  close();
};

onMounted(() => {
  visible.value = true;

  if (props.duration > 0) {
    setTimeout(() => {
      close();
    }, props.duration);
  }
});
</script>

<style scoped>
.system-message-notification {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 350px;
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
  border: 2px solid #ffc107;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  z-index: 10000;
  cursor: pointer;
  transition: all 0.3s ease;
}

.system-message-notification:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.notification-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 193, 7, 0.2);
  border-bottom: 1px solid #ffc107;
  border-radius: 10px 10px 0 0;
}

.notification-icon {
  font-size: 24px;
  margin-right: 10px;
}

.notification-title {
  font-weight: bold;
  color: #856404;
  flex: 1;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 24px;
  color: #856404;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  line-height: 1;
  transition: transform 0.2s;
}

.close-btn:hover {
  transform: scale(1.2);
}

.notification-content {
  padding: 16px;
  color: #333;
  line-height: 1.6;
  font-size: 14px;
}

.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  transform: translateX(400px);
  opacity: 0;
}

.notification-leave-to {
  transform: translateX(400px);
  opacity: 0;
}
</style>

