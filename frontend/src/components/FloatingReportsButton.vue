<template>
  <div class="floating-reports-button">
    <button @click="toggleReportsPanel" class="reports-bubble" :class="{ 'has-pending': pendingReportsCount > 0 }">
      <svg class="reports-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M12 7V12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <div v-if="pendingReportsCount > 0" class="reports-badge">
        {{ pendingReportsCount > 99 ? '99+' : pendingReportsCount }}
      </div>
    </button>

    <!-- 举报管理面板 -->
    <div v-if="showPanel" class="reports-panel">
      <div class="panel-header">
        <h3>🚩 举报管理</h3>
        <button @click="closePanel" class="close-btn">×</button>
      </div>

      <div class="panel-content">
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="reports.length === 0" class="no-reports">
          <div class="empty-icon">✅</div>
          <p>暂无待处理的举报</p>
        </div>

        <div v-else class="reports-list">
          <div v-for="report in reports" :key="report.id" class="report-item">
            <div class="report-header">
              <span class="report-status" :class="`status-${report.status.toLowerCase()}`">
                {{ getStatusText(report.status) }}
              </span>
              <span class="report-time">{{ formatTime(report.createdAt) }}</span>
            </div>

            <div class="report-body">
              <p class="report-reason"><strong>举报原因：</strong>{{ report.reason }}</p>
              <p v-if="report.description" class="report-description">{{ report.description }}</p>

              <div class="report-product">
                <strong>商品：</strong>{{ report.productName || `ID: ${report.productId}` }}
              </div>

              <div class="report-reporter">
                <strong>举报人：</strong>{{ report.reporterDisplayName || report.reporterUsername || `ID: ${report.reporterId}` }}
              </div>
            </div>

            <div class="report-actions" v-if="report.status === 'PENDING'">
              <button @click="handleReport(report.id, report.productId, 'APPROVED')" class="btn-approve">
                ✅ 下架商品
              </button>
              <button @click="handleReport(report.id, report.productId, 'REJECTED')" class="btn-reject">
                ❌ 驳回举报
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="panel-footer">
        <button @click="goToFullReports" class="btn-full-view">查看全部举报</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { toast } from '@/services/toast';

const router = useRouter();
const showPanel = ref(false);
const reports = ref([]);
const pendingReportsCount = ref(0);
const loading = ref(false);
let refreshInterval = null;

const toggleReportsPanel = () => {
  showPanel.value = !showPanel.value;
  if (showPanel.value) {
    loadReports();
  }
};

const closePanel = () => {
  showPanel.value = false;
};

const loadReports = async () => {
  loading.value = true;
  try {
    const token = localStorage.getItem('jwt_token');
    const response = await axios.get('/api/admin/reports', {
      headers: { Authorization: `Bearer ${token}` }
    });

    reports.value = response.data || [];

    // 计算待处理的举报数量
    pendingReportsCount.value = reports.value.filter(r => r.status === 'PENDING').length;
  } catch (error) {
    console.error('加载举报列表失败:', error);
    if (error.response?.status === 403) {
      toast('无权限访问举报管理', 'error');
    }
  } finally {
    loading.value = false;
  }
};

const handleReport = async (reportId, productId, status) => {
  if (!confirm(status === 'APPROVED' ? '确定要下架该商品吗？' : '确定要驳回该举报吗？')) {
    return;
  }

  try {
    const token = localStorage.getItem('jwt_token');

    if (status === 'APPROVED') {
      // 下架商品
      await axios.put(`/api/admin/products/${productId}/delist?reportId=${reportId}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      toast('商品已下架，已通知相关用户', 'success');
    } else {
      // 驳回举报
      await axios.put(`/api/admin/reports/${reportId}`, { status: 'REJECTED' }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      toast('举报已驳回', 'success');
    }

    // 刷新列表
    loadReports();
  } catch (error) {
    console.error('处理举报失败:', error);
    toast('操作失败，请重试', 'error');
  }
};

const goToFullReports = () => {
  router.push('/admin/reports');
  closePanel();
};

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待处理',
    'APPROVED': '已处理',
    'REJECTED': '已驳回'
  };
  return statusMap[status] || status;
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;

  // 1小时内
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  }
  // 今天
  if (now.toDateString() === date.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  // 更早
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
};

const startRefreshInterval = () => {
  // 每30秒刷新一次未读数
  refreshInterval = setInterval(() => {
    loadReports();
  }, 30000);
};

const stopRefreshInterval = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
};

onMounted(() => {
  loadReports();
  startRefreshInterval();
});

onUnmounted(() => {
  stopRefreshInterval();
});
</script>

<style scoped>
.floating-reports-button {
  position: fixed;
  bottom: 280px;
  right: 30px;
  z-index: 999;
}

.reports-bubble {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  position: relative;
}

.reports-bubble:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.5);
}

.reports-bubble.has-pending {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
  }
  50% {
    box-shadow: 0 4px 20px rgba(255, 107, 107, 0.8);
  }
}

.reports-icon {
  width: 28px;
  height: 28px;
  color: white;
}

.reports-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff3838;
  color: white;
  border-radius: 12px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.reports-panel {
  position: fixed;
  right: 30px;
  bottom: 350px;
  width: 450px;
  max-height: 600px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: transparent;
  border: none;
  color: white;
  font-size: 28px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  max-height: 450px;
}

.loading-state, .no-reports {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #ff6b6b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  border-left: 4px solid #ff6b6b;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.report-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-approved {
  background: #d4edda;
  color: #155724;
}

.status-rejected {
  background: #f8d7da;
  color: #721c24;
}

.report-time {
  font-size: 12px;
  color: #666;
}

.report-body {
  margin-bottom: 12px;
}

.report-body p {
  margin: 8px 0;
  font-size: 14px;
  color: #333;
}

.report-reason {
  font-weight: 500;
}

.report-description {
  color: #666;
  font-size: 13px;
}

.report-product, .report-reporter {
  font-size: 13px;
  color: #666;
  margin-top: 5px;
}

.report-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn-approve, .btn-reject {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-approve {
  background: #28a745;
  color: white;
}

.btn-approve:hover {
  background: #218838;
}

.btn-reject {
  background: #dc3545;
  color: white;
}

.btn-reject:hover {
  background: #c82333;
}

.panel-footer {
  padding: 15px;
  border-top: 1px solid #e0e0e0;
  background: #f8f9fa;
}

.btn-full-view {
  width: 100%;
  padding: 10px;
  background: white;
  border: 2px solid #ff6b6b;
  color: #ff6b6b;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-full-view:hover {
  background: #ff6b6b;
  color: white;
}
</style>

