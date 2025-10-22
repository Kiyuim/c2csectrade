<template>
  <div class="floating-publish-container">
    <router-link to="/products/create" class="publish-button">
      <!-- 发布图标 -->
      <svg class="publish-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <span class="publish-text">发布</span>
    </router-link>
  </div>
</template>

<script setup>
// 发布商品浮动按钮组件
</script>

<style scoped>
.floating-publish-container {
  position: fixed;
  bottom: 310px;
  left: 30px;
  z-index: 1001;
}

.publish-button {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #28a745, #218838);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(40, 167, 69, 0.3);
  transition: all 0.3s ease;
  text-decoration: none;
  color: white;
}

.publish-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(40, 167, 69, 0.4);
}

.publish-icon {
  width: 24px;
  height: 24px;
  color: white;
  margin-bottom: 2px;
}

.publish-text {
  font-size: 10px;
  font-weight: 500;
  color: white;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .floating-publish-container {
    bottom: 260px;
    left: 20px;
  }

  .publish-button {
    width: 50px;
    height: 50px;
  }

  .publish-icon {
    width: 20px;
    height: 20px;
  }

  .publish-text {
    font-size: 9px;
  }
}
</style>
