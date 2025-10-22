import { defineStore } from 'pinia';
import axios from 'axios';

const API_BASE = process.env.VUE_APP_API_BASE_URL || '/api';
const AUTH_API = `${API_BASE}/auth`;

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('jwt_token') || null,
        user: JSON.parse(localStorage.getItem('user')) || null,
    }),

    getters: {
        isLoggedIn: (state) => !!state.token,
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
    },
});