import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { createPinia } from 'pinia';
import axios from 'axios';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/services/toast';

// ========== 创建 Vue 应用 ==========
const app = createApp(App);

// ========== 注册 Pinia 与 Router ==========
const pinia = createPinia();
app.use(pinia);
app.use(router);

// ========== 初始化 Auth Store ==========
const authStore = useAuthStore();
authStore.init(); // 🔹 恢复登录状态（从 localStorage 加载用户与 token）

// ========== 设置 Axios 拦截器 ==========

// 请求拦截器：在每个请求前加上 Authorization 头
axios.interceptors.request.use(
    (config) => {
        const token = authStore.token || localStorage.getItem('jwt_token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 响应拦截器：仅处理401登出逻辑，不显示错误消息（由各组件自行处理）
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error?.response?.status;
        const url = error?.config?.url || '';
        const currentPath = router.currentRoute.value.path;

        // 如果是401错误且是API请求，但不在登录/注册/忘记密码页面
        if (status === 401 && url.startsWith('/api')) {
            // 排除登录、注册、忘记密码等公开接口
            const publicPaths = ['/login', '/register', '/forgot-password'];
            const isPublicPage = publicPaths.some(path => currentPath.includes(path));
            const isPublicApi = url.includes('/auth/login') ||
                               url.includes('/auth/register') ||
                               url.includes('/auth/forgot-password') ||
                               url.includes('/auth/reset-password');

            // 只有在非公开页面且非公开API时才提示登录过期
            if (!isPublicPage && !isPublicApi) {
                toast('❌ 登录已过期，请重新登录', 'error');
                authStore.logout();
                router.push('/login');
            }
        }
        // ⚠️ 不再在这里自动显示错误消息，让各组件自己处理，避免重复提示和错误码暴露
        return Promise.reject(error);
    }
);

// ========== 挂载应用 ==========
app.mount('#app');