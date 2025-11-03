import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnecting = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
    }

    connect(onMessageReceived) {
        // 如果已经连接或正在重连，不要重复连接
        if (this.connected || this.reconnecting) {
            console.log('[WS] 已经连接或正在连接中...');
            return;
        }

        const token = localStorage.getItem('jwt_token');
        if (!token) {
            console.error("[WS] No token found for WebSocket connection.");
            return;
        }

        const user = JSON.parse(localStorage.getItem('user') || '{}');
        const username = user.username || 'unknown';
        console.log('[WS] 开始连接 WebSocket... 用户:', username);

        this.reconnecting = true;

        // Use relative endpoint so it works in dev proxy and Docker Nginx
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        // Disable verbose debug logs but keep errors
        this.stompClient.debug = (msg) => {
            if (msg.includes('ERROR') || msg.includes('DISCONNECT')) {
                console.error('[WS-DEBUG]', msg);
            }
        };

        const headers = { 'Authorization': `Bearer ${token}` };

        this.stompClient.connect(headers, () => {
            console.log('[WS] ✅ WebSocket 连接成功!');
            console.log('[WS] 已认证用户:', username);
            this.connected = true;
            this.reconnecting = false;
            this.reconnectAttempts = 0;

            // 订阅私有消息队列
            console.log('[WS] 订阅 /user/queue/private');
            const subscription = this.stompClient.subscribe('/user/queue/private', (message) => {
                console.log('[WS] 📨 收到消息!');
                try {
                    onMessageReceived(message);
                } catch (error) {
                    console.error('[WS] 处理消息时出错:', error);
                }
            });

            console.log('[WS] ✅ 订阅成功，订阅ID:', subscription.id);
            console.log('[WS] 等待消息中...');
        }, (error) => {
            console.error('[WS] ❌ WebSocket 连接失败:', error);
            this.connected = false;
            this.reconnecting = false;

            // 尝试重连
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                const delay = Math.min(5000 * this.reconnectAttempts, 30000);
                console.log(`[WS] ${delay/1000}秒后尝试第${this.reconnectAttempts}次重连...`);
                setTimeout(() => {
                    this.connect(onMessageReceived);
                }, delay);
            } else {
                console.error('[WS] 已达到最大重连次数，停止重连');
            }
        });
    }

    sendMessage(chatMessage) {
        if (this.stompClient && this.connected) {
            this.stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        } else {
            console.error('WebSocket is not connected');
        }
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
            this.reconnecting = false;
            this.reconnectAttempts = 0;
            console.log('[WS] WebSocket 已断开连接');
        }
    }

    isConnected() {
        return this.connected;
    }

    reconnect(onMessageReceived) {
        console.log('[WS] 手动触发重连...');
        this.disconnect();
        this.reconnectAttempts = 0;
        setTimeout(() => {
            this.connect(onMessageReceived);
        }, 1000);
    }
}

export default new WebSocketService();
