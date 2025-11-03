<template>
  <div class="debug-page">
    <h1>🔧 WebSocket 调试工具</h1>

    <div class="debug-section">
      <h2>连接状态</h2>
      <div class="status-box">
        <p><strong>连接状态:</strong>
          <span :class="connectionStatus.class">{{ connectionStatus.text }}</span>
        </p>
        <p><strong>当前用户:</strong> {{ currentUser.username || '未登录' }}</p>
        <p><strong>用户ID:</strong> {{ currentUser.id || 'N/A' }}</p>
        <p><strong>JWT Token:</strong> {{ tokenPreview }}</p>
      </div>

      <div class="button-group">
        <button @click="testConnect" class="btn-primary">连接WebSocket</button>
        <button @click="testDisconnect" class="btn-danger">断开连接</button>
        <button @click="testReconnect" class="btn-warning">重新连接</button>
      </div>
    </div>

    <div class="debug-section">
      <h2>发送测试消息</h2>
      <div class="form-group">
        <label>接收者用户名:</label>
        <input v-model="testRecipient" placeholder="输入用户名" />
      </div>
      <div class="form-group">
        <label>消息内容:</label>
        <textarea v-model="testMessage" rows="3" placeholder="输入测试消息"></textarea>
      </div>
      <button @click="sendTestMessage" class="btn-primary">发送普通消息</button>
    </div>

    <div class="debug-section">
      <h2>接收到的消息 ({{ receivedMessages.length }})</h2>
      <button @click="clearMessages" class="btn-secondary">清空消息</button>
      <div class="messages-list">
        <div v-for="(msg, index) in receivedMessages" :key="index"
             :class="['message-item', msg.isSystemMessage ? 'system' : 'normal']">
          <div class="message-header">
            <span class="message-type">
              {{ msg.isSystemMessage ? '🔔 系统消息' : '💬 普通消息' }}
            </span>
            <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
          </div>
          <div class="message-body">
            <p><strong>发送者:</strong> {{ msg.sender }}</p>
            <p><strong>接收者:</strong> {{ msg.recipient }}</p>
            <p><strong>内容:</strong> {{ msg.content }}</p>
          </div>
        </div>
        <div v-if="receivedMessages.length === 0" class="no-messages">
          暂无消息
        </div>
      </div>
    </div>

    <div class="debug-section">
      <h2>控制台日志</h2>
      <button @click="clearLogs" class="btn-secondary">清空日志</button>
      <div class="logs-container">
        <div v-for="(log, index) in logs" :key="index" :class="['log-item', log.level]">
          <span class="log-time">{{ log.time }}</span>
          <span class="log-level">{{ log.level.toUpperCase() }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import WebSocketService from '@/services/WebSocketService';
import emitter from '@/eventBus';

const router = useRouter();
const currentUser = ref({});
const testRecipient = ref('');
const testMessage = ref('');
const receivedMessages = ref([]);
const logs = ref([]);

const connectionStatus = computed(() => {
  if (WebSocketService.isConnected()) {
    return { text: '✅ 已连接', class: 'connected' };
  }
  return { text: '❌ 未连接', class: 'disconnected' };
});

const tokenPreview = computed(() => {
  const token = localStorage.getItem('jwt_token');
  if (!token) return '无';
  return token.substring(0, 20) + '...' + token.substring(token.length - 10);
});

const addLog = (level, message) => {
  const now = new Date();
  const time = now.toLocaleTimeString('zh-CN', { hour12: false });
  logs.value.unshift({ level, message, time });
  if (logs.value.length > 50) logs.value.pop();
};

const testConnect = () => {
  addLog('info', '尝试连接WebSocket...');
  WebSocketService.connect(onMessageReceived);
};

const testDisconnect = () => {
  addLog('info', '断开WebSocket连接');
  WebSocketService.disconnect();
};

const testReconnect = () => {
  addLog('info', '重新连接WebSocket');
  WebSocketService.reconnect(onMessageReceived);
};

const sendTestMessage = () => {
  if (!testRecipient.value || !testMessage.value) {
    addLog('error', '请填写接收者和消息内容');
    return;
  }

  const message = {
    sender: currentUser.value.username,
    recipient: testRecipient.value,
    content: testMessage.value,
    type: 'CHAT'
  };

  addLog('info', `发送消息给 ${testRecipient.value}: ${testMessage.value}`);
  WebSocketService.sendMessage(message);
};

const onMessageReceived = (payload) => {
  try {
    const message = JSON.parse(payload.body);
    addLog('success', `收到消息: ${message.content}`);
    receivedMessages.value.unshift(message);
  } catch (error) {
    addLog('error', `解析消息失败: ${error.message}`);
  }
};

const clearMessages = () => {
  receivedMessages.value = [];
  addLog('info', '已清空消息列表');
};

const clearLogs = () => {
  logs.value = [];
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  return new Date(timestamp).toLocaleString('zh-CN');
};

onMounted(() => {
  const user = localStorage.getItem('user');
  if (user) {
    currentUser.value = JSON.parse(user);
  }

  addLog('info', 'WebSocket调试工具已加载');

  // 监听消息事件
  emitter.on('chat-message', onMessageReceived);

  // 自动连接
  if (!WebSocketService.isConnected()) {
    testConnect();
  }
});

onUnmounted(() => {
  emitter.off('chat-message', onMessageReceived);
});
</script>

<style scoped>
.debug-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: monospace;
}

h1 {
  color: #333;
  border-bottom: 2px solid #667eea;
  padding-bottom: 10px;
}

.debug-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.debug-section h2 {
  margin-top: 0;
  color: #667eea;
  font-size: 18px;
}

.status-box {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.status-box p {
  margin: 8px 0;
}

.connected {
  color: #28a745;
  font-weight: bold;
}

.disconnected {
  color: #dc3545;
  font-weight: bold;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.2s;
}

button:hover {
  opacity: 0.8;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-warning {
  background: #ffc107;
  color: #333;
}

.btn-secondary {
  background: #6c757d;
  color: white;
  margin-bottom: 10px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: inherit;
}

.messages-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
}

.message-item {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 10px;
}

.message-item.system {
  background: #fff3cd;
  border-color: #ffc107;
}

.message-item.normal {
  background: #e7f3ff;
  border-color: #667eea;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: bold;
}

.message-time {
  font-size: 12px;
  color: #666;
}

.message-body p {
  margin: 4px 0;
  font-size: 13px;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 20px;
}

.logs-container {
  max-height: 300px;
  overflow-y: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
}

.log-item {
  padding: 4px 0;
  border-bottom: 1px solid #333;
}

.log-time {
  color: #858585;
  margin-right: 10px;
}

.log-level {
  margin-right: 10px;
  font-weight: bold;
}

.log-item.info .log-level {
  color: #4fc3f7;
}

.log-item.success .log-level {
  color: #66bb6a;
}

.log-item.error .log-level {
  color: #ef5350;
}

.log-item.warning .log-level {
  color: #ffca28;
}
</style>

