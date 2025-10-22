<template>
  <div class="chat-container">
    <div class="chat-header">
      <h3>与 {{ displayName }} 的聊天</h3>
    </div>

    <div class="messages-area" ref="messagesArea">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.isSystemMessage ? 'system' : (msg.sender === currentUsername ? 'sent' : 'received')]"
      >
        <div v-if="msg.isSystemMessage" class="system-badge">🔔 系统消息</div>
        <div class="message-header" v-if="!msg.isSystemMessage">
          <strong>{{ msg.senderDisplayName || msg.sender }}</strong>
          <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
        </div>
        <div class="message-content">{{ msg.content }}</div>
      </div>
      <div v-if="messages.length === 0" class="no-messages">
        暂无消息，开始聊天吧！
      </div>
    </div>

    <form @submit.prevent="sendMessage" class="input-area">
      <input
        v-model="newMessage"
        placeholder="输入消息..."
        class="message-input"
        :disabled="!isConnected"
      />
      <button
        type="submit"
        class="send-btn"
        :disabled="!newMessage.trim() || !isConnected"
      >
        发送
      </button>
    </form>

    <div v-if="!isConnected" class="connection-status">
      连接中...
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch, defineProps } from 'vue';
import WebSocketService from '@/services/WebSocketService';
import { chatService } from '@/services/chatService';

const props = defineProps({
  recipientUsername: {
    type: String,
    required: true
  },
  recipientDisplayName: {
    type: String,
    default: ''
  },
  currentUsername: {
    type: String,
    required: true
  }
});

const displayName = computed(() => props.recipientDisplayName || props.recipientUsername);

const messages = ref([]);
const newMessage = ref('');
const isConnected = ref(false);
const messagesArea = ref(null);

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diffInHours = (now - date) / (1000 * 60 * 60);

  if (diffInHours < 24) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' +
           date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesArea.value) {
      messagesArea.value.scrollTop = messagesArea.value.scrollHeight;
    }
  });
};

// 加载聊天历史
const loadHistory = async () => {
  try {
    const response = await chatService.getChatHistory(props.recipientUsername);
    messages.value = response.data.reverse(); // 反转以使最新消息在底部
    scrollToBottom();

    // 标记消息为已读
    await chatService.markAsRead(props.recipientUsername);
  } catch (error) {
    console.error('Failed to load chat history:', error);
  }
};

// 接收到新消息的回调
const onMessageReceived = (payload) => {
  const message = JSON.parse(payload.body);

  // 只添加与当前对话相关的消息
  if (message.sender === props.recipientUsername ||
      message.recipient === props.recipientUsername) {
    messages.value.push(message);
    scrollToBottom();

    // 如果是接收到的消息，标记为已读
    if (message.sender === props.recipientUsername) {
      chatService.markAsRead(props.recipientUsername);
    }
  }
};

// 发送消息
const sendMessage = () => {
  if (newMessage.value.trim() && isConnected.value) {
    const chatMessage = {
      sender: props.currentUsername,
      recipient: props.recipientUsername,
      content: newMessage.value,
      type: 'CHAT'
    };

    WebSocketService.sendMessage(chatMessage);
    newMessage.value = '';
  }
};

// 监听recipientUsername变化，重新加载历史
watch(() => props.recipientUsername, () => {
  messages.value = [];
  loadHistory();
});

onMounted(() => {
  loadHistory();

  // 连接WebSocket
  if (!WebSocketService.isConnected()) {
    WebSocketService.connect(onMessageReceived);
    setTimeout(() => {
      isConnected.value = WebSocketService.isConnected();
    }, 1000);
  } else {
    isConnected.value = true;
    WebSocketService.connect(onMessageReceived);
  }
});

onUnmounted(() => {
  // 不在这里断开连接，因为可能还有其他组件在使用
  // WebSocketService.disconnect();
});
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  border: none;
  background: white;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  width: 100%;
  box-sizing: border-box;
}

.chat-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  line-height: 1;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
}

.message {
  margin-bottom: 15px;
  max-width: 70%;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.sent {
  margin-left: auto;
}

.message.received {
  margin-right: auto;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
}

.message.sent .message-header {
  flex-direction: row-reverse;
}

.message-time {
  color: #666;
  font-size: 11px;
}

.message-content {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.5;
}

.message.sent .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message.received .message-content {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message.system {
  max-width: 90%;
  margin-left: auto;
  margin-right: auto;
}

.system-badge {
  font-size: 11px;
  font-weight: bold;
  color: #f57c00;
  margin-bottom: 5px;
  text-align: center;
}

.message.system .message-content {
  background: #fff3cd;
  color: #333;
  border-left: 4px solid #ffa000;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

.message.system .message-header {
  justify-content: center;
  color: #f57c00;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 50px 20px;
  font-size: 15px;
}

.input-area {
  display: flex;
  gap: 10px;
  padding: 20px;
  background: white;
  border-top: 1px solid #e0e0e0;
}

.message-input {
  flex: 1;
  padding: 12px 18px;
  border: 2px solid #e0e0e0;
  border-radius: 24px;
  outline: none;
  font-size: 14px;
  transition: all 0.3s;
}

.message-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.message-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.send-btn {
  padding: 12px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.connection-status {
  padding: 8px 20px;
  background: #fff3cd;
  color: #856404;
  text-align: center;
  font-size: 12px;
  border-top: 1px solid #ffeeba;
}
</style>
}
