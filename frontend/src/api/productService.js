import axios from 'axios';

const apiBase = process.env.VUE_APP_API_BASE_URL || '/api';

const apiClient = axios.create({
  baseURL: apiBase,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动添加 JWT Token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default {
  createProduct(formData) {
    // 对于 FormData，需要移除默认的 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary
    return apiClient.post('/products', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  getProductById(id) {
    return apiClient.get(`/products/${id}`);
  },
  getAllProducts(params) {
    return apiClient.get('/products', { params });
  },
  searchProducts(params) {
    return apiClient.get('/products/search', { params });
  },
};
