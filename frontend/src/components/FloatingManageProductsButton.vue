<template>
  <div class="floating-manage-products" @click="goToManage" :title="'管理我的商品'">
    <span class="icon">📦</span>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goToManage = () => {
  router.push({ name: 'product-manage' });
};
</script>

<style scoped>
.floating-manage-products {
  position: fixed;
  bottom: 240px;
  left: 30px;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6a00 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.4);
  transition: all 0.3s ease;
  z-index: 1001;
}

.floating-manage-products:hover {
  transform: translateY(-4px) scale(1.1);
  box-shadow: 0 6px 20px rgba(255, 106, 0, 0.5);
}

.icon {
  font-size: 28px;
}

@media (max-width: 768px) {
  .floating-manage-products {
    width: 50px;
    height: 50px;
    bottom: 190px;
    left: 20px;
  }

  .icon {
    font-size: 24px;
  }
}
</style>

