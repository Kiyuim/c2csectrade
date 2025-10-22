import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    stompClient = null;
    connected = false;

    connect(onMessageReceived) {
        const token = localStorage.getItem('jwt_token');
        if (!token) {
            console.error("No token found for WebSocket connection.");
            return;
        }

        // Use relative endpoint so it works in dev proxy and Docker Nginx
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = () => {};

        const headers = { 'Authorization': `Bearer ${token}` };

        this.stompClient.connect(headers, (frame) => {
            console.log('WebSocket Connected: ' + frame);
            this.connected = true;
            this.stompClient.subscribe('/user/queue/private', onMessageReceived);
        }, (error) => {
            console.error('WebSocket connection error:', error);
            this.connected = false;
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
            console.log('WebSocket Disconnected');
        }
    }

    isConnected() {
        return this.connected;
    }
}

export default new WebSocketService();
