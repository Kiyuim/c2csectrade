<template>
  <div class="admin-reports-page">
    <header class="page-header">
      <h1><i class="fas fa-flag"></i> 举报管理</h1>
      <div class="header-actions">
        <button @click="fetchReports" :disabled="loading">
          <i :class="['fas fa-sync-alt', loading ? 'fa-spin' : '']"></i>
          刷新
        </button>
        <button @click="goHome">
          <i class="fas fa-home"></i>
          返回主页
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>加载举报信息中...</p>
    </div>

    <div v-else-if="reports.length === 0" class="no-reports-container">
      <i class="fas fa-check-circle"></i>
      <p>没有待处理的举报</p>
    </div>

    <div v-else class="reports-grid">
      <div v-for="report in reports" :key="report.id" class="report-card">
        <div class="card-header">
          <span class="report-id">举报 #{{ report.id }}</span>
          <span :class="['status-badge', getStatusClass(report.status)]">{{ getStatusText(report.status) }}</span>
        </div>
        <div class="card-body">
          <div class="info-item">
            <strong class="info-label"><i class="fas fa-book"></i> 商品ID:</strong>
            <a :href="`/products/${report.productId}`" target="_blank" class="product-link">{{ report.productId }}</a>
          </div>
          <div class="info-item">
            <strong class="info-label"><i class="fas fa-user-secret"></i> 举报人ID:</strong>
            <span>{{ report.reporterId }}</span>
          </div>
          <div class="info-item">
            <strong class="info-label"><i class="fas fa-exclamation-triangle"></i> 举报原因:</strong>
            <span>{{ getReasonText(report.reason) }}</span>
          </div>
          <div class="info-item description">
            <strong class="info-label"><i class="fas fa-comment-dots"></i> 详细描述:</strong>
            <p>{{ report.description || '无' }}</p>
          </div>
          <div class="info-item">
            <strong class="info-label"><i class="fas fa-clock"></i> 举报时间:</strong>
            <span>{{ formatTimestamp(report.createdAt) }}</span>
          </div>
        </div>
        <div class="card-footer">
          <button class="btn btn-approve" @click="handleReport(report, 'APPROVED')" :disabled="report.status !== 'PENDING'">
            <i class="fas fa-check"></i> 批准并下架
          </button>
          <button class="btn btn-reject" @click="handleReport(report, 'REJECTED')" :disabled="report.status !== 'PENDING'">
            <i class="fas fa-times"></i> 拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { toast } from '@/services/toast';
import { format } from 'date-fns';
import { useRouter } from 'vue-router';

const router = useRouter();
const reports = ref([]);
const loading = ref(false);

const reasonMapping = {
  INAPPROPRIATE_CONTENT: '不当内容',
  SCAM: '诈骗',
  SPAM: '垃圾信息',
  OTHER: '其他',
};

const statusMapping = {
  PENDING: '待处理',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
};

const getReasonText = (reason) => reasonMapping[reason] || reason;
const getStatusText = (status) => statusMapping[status] || status;

const getStatusClass = (status) => {
  switch (status) {
    case 'PENDING': return 'status-pending';
    case 'APPROVED': return 'status-approved';
    case 'REJECTED': return 'status-rejected';
    default: return '';
  }
};

const formatTimestamp = (timestamp) => {
  if (!timestamp) return 'N/A';
  return format(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss');
};

const fetchReports = async () => {
  loading.value = true;
  try {
    const response = await axios.get('/api/admin/reports');
    reports.value = response.data;
  } catch (error) {
    console.error('获取举报列表失败:', error);
    toast('获取举报列表失败', 'error');
  } finally {
    loading.value = false;
  }
};

const handleReport = async (report, status) => {
  if (status === 'APPROVED') {
    if (!confirm('确定要批准该举报并下架商品吗？此操作将通知用户。')) {
      return;
    }
    try {
      await axios.put(`/api/admin/products/${report.productId}/delist?reportId=${report.id}`);
      toast(`商品 #${report.productId} 已下架`, 'success');
      await axios.put(`/api/admin/reports/${report.id}`, { status });
      toast(`举报 #${report.id} 状态已更新为 ${status}`, 'success');
      fetchReports();
    } catch (error) {
      console.error('处理举报失败:', error);
      toast('处理举报失败', 'error');
    }
  } else {
    try {
      await axios.put(`/api/admin/reports/${report.id}`, { status });
      toast(`举报 #${report.id} 状态已更新为 ${status}`, 'success');
      fetchReports();
    } catch (error) {
      console.error('更新举报状态失败:', error);
      toast('更新举报状态失败', 'error');
    }
  }
};

const goHome = () => {
  router.push('/');
};

onMounted(() => {
  fetchReports();
});
</script>

<style scoped>
@import url("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css");

.admin-reports-page {
  padding: 2rem;
  background-color: #f4f7f6;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 2rem;
  color: #333;
}

.header-actions button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-left: 1rem;
}

.header-actions button:hover {
  background-color: #0056b3;
}

.loading-container, .no-reports-container {
  text-align: center;
  padding: 4rem;
  color: #666;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
}

.report-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}

.report-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
}

.report-id {
  font-weight: bold;
  color: #495057;
}

.status-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 500;
  text-transform: uppercase;
}

.status-pending { background-color: #ffc107; color: #333; }
.status-approved { background-color: #28a745; color: white; }
.status-rejected { background-color: #dc3545; color: white; }

.card-body {
  padding: 1rem;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 0.75rem;
  color: #666;
}

.info-label {
  font-weight: 500;
  color: #333;
  margin-right: 0.5rem;
  min-width: 100px;
}

.info-label i {
  margin-right: 0.5rem;
  color: #007bff;
}

.product-link {
  color: #007bff;
  text-decoration: none;
  transition: color 0.2s;
}

.product-link:hover {
  color: #0056b3;
  text-decoration: underline;
}

.description p {
  margin: 0;
  padding: 0.5rem;
  background-color: #f8f9fa;
  border-radius: 5px;
  border: 1px solid #e9ecef;
  max-height: 100px;
  overflow-y: auto;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem;
  background-color: #f8f9fa;
  border-top: 1px solid #e9ecef;
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
}

.btn {
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
  font-size: 0.9rem;
}

.btn-approve { background-color: #28a745; color: white; }
.btn-approve:hover { background-color: #218838; }

.btn-reject { background-color: #dc3545; color: white; }
.btn-reject:hover { background-color: #c82333; }

.btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>
