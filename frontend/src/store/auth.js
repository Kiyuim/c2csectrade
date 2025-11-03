import { defineStore } from 'pinia';
import axios from 'axios';

const API_BASE = process.env.VUE_APP_API_BASE_URL || '/api';
const AUTH_API = `${API_BASE}/auth`;
const CHAT_API = `${API_BASE}/chat`;


export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('jwt_token') || null,
        user: JSON.parse(localStorage.getItem('user')) || null,
        unreadCount: 0,
        conversations: [],
    }),

    getters: {
        isLoggedIn: (state) => !!state.token,
        unreadCount: (state) => state.unreadCount,
        conversations: (state) => state.conversations,
    },

    actions: {
        /**
         * 登录
         * @param {Object} credentials - { username, password, captchaId, captchaCode, isAdmin }
         */
        async login(credentials) {
            try {
                const { data } = await axios.post(`${AUTH_API}/login`, {
                    username: credentials.username,
                    password: credentials.password,
                    isAdmin: credentials.isAdmin,
                    captchaId: credentials.captchaId,
                    captchaCode: credentials.captchaCode
                });
                const { token, username, role, userId, displayName, avatarUrl, email } = data;

                // 更新状态 - 保存完整的用户信息
                this.token = token;
                this.user = {
                    id: userId,
                    username,
                    role,
                    displayName,
                    avatarUrl,
                    email
                };

                // 持久化
                localStorage.setItem('jwt_token', token);
                localStorage.setItem('user', JSON.stringify(this.user));

                // 设置 Axios 默认 Authorization 头
                axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

                // 登录成功后，获取未读消息、会话列表和信誉等级
                await this.fetchUnreadCount();
                await this.fetchAllConversations();
                await this.fetchCreditScore();

            } catch (error) {
                // 登录失败时清除状态
                this.logout();
                throw error;
            }
        },

        /**
         * 注册
         * @param {Object} credentials - { username, password, email, captchaId, captchaCode }
         */
        async register(credentials) {
            return axios.post(`${AUTH_API}/register`, {
                username: credentials.username,
                password: credentials.password,
                email: credentials.email,
                captchaId: credentials.captchaId,
                captchaCode: credentials.captchaCode
            });
        },

        /**
         * 登出
         */
        logout() {
            this.token = null;
            this.user = null;
            this.unreadCount = 0;
            this.conversations = [];

            localStorage.removeItem('jwt_token');
            localStorage.removeItem('user');

            delete axios.defaults.headers.common['Authorization'];
        },

        /**
         * 恢复登录状态（页面刷新后调用）
         */
        init() {
            const savedToken = localStorage.getItem('jwt_token');
            const savedUser = localStorage.getItem('user');

            if (savedToken && savedUser) {
                this.token = savedToken;
                this.user = JSON.parse(savedUser);
                axios.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`;
                // 页面加载时也获取最新数据
                this.fetchUnreadCount();
                this.fetchCreditScore();
                this.fetchAllConversations();
            }
        },

        /**
         * 更新用户信息（用于头像、displayName 等更新）
         */
        updateUser(userData) {
            if (this.user) {
                // 合并更新用户信息
                this.user = { ...this.user, ...userData };
                localStorage.setItem('user', JSON.stringify(this.user));
            }
        },

        /**
         * 获取未读消息总数
         */
        async fetchUnreadCount() {
            if (!this.isLoggedIn) return;
            try {
                const { data } = await axios.get(`${CHAT_API}/unread-count`);
                this.unreadCount = data;
            } catch (error) {
                console.error('获取未读消息数失败:', error);
                this.unreadCount = 0;
            }
        },

        /**
         * 获取所有会话列表
         */
        async fetchAllConversations() {
            if (!this.isLoggedIn) return;
            try {
                const { data } = await axios.get(`${CHAT_API}/conversations`);
                this.conversations = data;
            } catch (error) {
                console.error('获取会话列表失败:', error);
                this.conversations = [];
            }
        },
        /**
         * 获取用户信誉等级
         */
        async fetchCreditScore() {
            if (!this.isLoggedIn || !this.user?.id) return;
            try {
                const { data } = await axios.get(`${API_BASE}/credit-score/${this.user.id}`);
                if (data.success && data.data) {
                    // 更新用户信息，添加信誉等级
                    this.user.creditLevel = data.data.level;
                    this.user.creditLevelName = data.data.levelName;
                    this.user.totalScore = data.data.totalScore;
                    localStorage.setItem('user', JSON.stringify(this.user));
                }
            } catch (error) {
                console.error('获取信誉等级失败:', error);
                // 失败时设置默认值
                if (this.user) {
                    this.user.creditLevel = 1;
                    this.user.creditLevelName = '新手';
                }
            }
        },

        /**
         * 更新或增加单个会话
         * @param {Object} conversation - 要更新或添加的会话
         */
        updateOrAddConversation(conversation) {
            const index = this.conversations.findIndex(c => c.userId === conversation.userId);
            if (index !== -1) {
                // 更新现有会话
                this.conversations[index] = { ...this.conversations[index], ...conversation };
            } else {
                // 添加新会话
                this.conversations.unshift(conversation);
            }
        },

        /**
         * 增加未读消息数
         * @param {number} [count=1] - 增加的数量
         */
        incrementUnreadCount(count = 1) {
            this.unreadCount += count;
        },
    },
});