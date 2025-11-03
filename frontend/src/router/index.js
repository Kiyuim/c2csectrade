import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/store/auth';

import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import ForgotPasswordView from '../views/ForgotPasswordView.vue';
import ProductCreate from '../views/ProductCreate.vue';
import ProductDetail from '../views/ProductDetail.vue';
import AdminUsers from '../views/AdminUsers.vue';
import ChatView from '../views/ChatView.vue';
import AdminChatView from '../views/AdminChatView.vue';
import FavoritesView from '../views/FavoritesView.vue';
import CartView from '../views/CartView.vue';
import UserProductsView from '../views/UserProductsView.vue';
import ProductManageView from '../views/ProductManageView.vue';
import AdminReports from '../views/AdminReports.vue';
import OrderHistory from '../views/OrderHistory.vue';
import PaymentView from '../views/PaymentView.vue';
import PaymentResult from '../views/PaymentResult.vue';
import PaymentPasswordSetup from '../views/PaymentPasswordSetup.vue';
import BargainView from '../views/BargainView.vue';
import ReviewView from '../views/ReviewView.vue';

// ===== 定义路由规则 =====
const routes = [
    {
        path: '/',
        name: 'home',
        component: HomeView,
        meta: { requiresAuth: true }, // 需要登录才能访问
    },
    {
        path: '/login',
        name: 'login',
        component: LoginView,
        meta: { requiresAuth: false }, // 登录页不需要权限
    },
    {
        path: '/register',
        name: 'register',
        component: RegisterView,
        meta: { requiresAuth: false },
    },
    {
        path: '/forgot-password',
        name: 'forgot-password',
        component: ForgotPasswordView,
        meta: { requiresAuth: false },
    },
    {
        path: '/products/create',
        name: 'product-create',
        component: ProductCreate,
        meta: { requiresAuth: true },
    },
    {
        path: '/products/:id',
        name: 'product-detail',
        component: ProductDetail,
    },
    {
        path: '/admin/users',
        name: 'admin-users',
        component: AdminUsers,
        meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
        path: '/chat',
        name: 'chat',
        component: ChatView,
        meta: { requiresAuth: true },
    },
    {
        path: '/favorites',
        name: 'favorites',
        component: FavoritesView,
        meta: { requiresAuth: true },
    },
    {
        path: '/cart',
        name: 'cart',
        component: CartView,
        meta: { requiresAuth: true },
    },
    {
        path: '/admin/chat',
        name: 'admin-chat',
        component: AdminChatView,
        meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
        path: '/admin/users/:userId/products',
        name: 'user-products',
        component: UserProductsView,
        meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
        path: '/products/manage',
        name: 'product-manage',
        component: ProductManageView,
        meta: { requiresAuth: true },
    },
    {
        path: '/admin/reports',
        name: 'admin-reports',
        component: AdminReports,
        meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
        path: '/order-history',
        name: 'order-history',
        component: OrderHistory,
        meta: { requiresAuth: true },
    },
    {
        path: '/payment/:orderId',
        name: 'payment',
        component: PaymentView,
        meta: { requiresAuth: true },
    },
    {
        path: '/payment-result',
        name: 'payment-result',
        component: PaymentResult,
        meta: { requiresAuth: true },
    },
    {
        path: '/payment-password/setup',
        name: 'payment-password-setup',
        component: PaymentPasswordSetup,
        meta: { requiresAuth: true },
    },
    {
        path: '/bargain/:id',
        name: 'bargain',
        component: BargainView,
        meta: { requiresAuth: false }, // 砍价页面可以分享，不需要登录也能查看
    },
    {
        path: '/review/:orderId',
        name: 'review',
        component: ReviewView,
        meta: { requiresAuth: true },
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/', // 未匹配的路径全部重定向到首页
    },
];

// ===== 创建路由实例 =====
const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes,
});

// ===== 全局路由守卫 =====
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore();

    // 需要登录
    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        return next('/login');
    }

    // 已登录时禁止访问登录/注册
    if ((to.path === '/login' || to.path === '/register') && authStore.isLoggedIn) {
        return next('/');
    }

    // 需要管理员权限
    if (to.meta.requiresAdmin) {
        const role = authStore.user?.role;
        if (role !== 'ROLE_ADMIN') {
            return next('/');
        }
    }

    return next();
});

export default router;