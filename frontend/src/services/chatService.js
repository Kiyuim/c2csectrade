import axios from 'axios';

// Use relative base so it works in dev proxy and Docker Nginx
const API_URL = '/api/chat';

const getAuthHeader = () => {
    const token = localStorage.getItem('jwt_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
};

export const chatService = {
    // 获取与某个用户的聊天历史
    getChatHistory(username, limit = 50) {
        return axios.get(`${API_URL}/history/${username}`, {
            params: { limit },
            headers: getAuthHeader()
        });
    },

    // 标记与某个用户的消息为已读
    markAsRead(username) {
        return axios.post(`${API_URL}/read/${username}`, {}, {
            headers: getAuthHeader()
        });
    },

    // 获取未读消息总数
    getUnreadCount() {
        return axios.get(`${API_URL}/unread-count`, {
            headers: getAuthHeader()
        });
    }
};
