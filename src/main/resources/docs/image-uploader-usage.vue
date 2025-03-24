<!--
  图床上传组件使用示例 - Vue 3
-->

<template>
  <div class="app-container">
    <h1>用户头像上传</h1>
    
    <!-- 导入并使用图片上传组件 -->
    <ImageUploader @upload-success="handleUploadSuccess" />
    
    <!-- 显示上传后的用户头像 -->
    <div v-if="userAvatar" class="avatar-preview">
      <h2>您的头像</h2>
      <img :src="userAvatar" alt="用户头像" class="avatar-image" />
      <button @click="saveUserProfile" :disabled="saving">
        {{ saving ? '保存中...' : '保存用户资料' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import ImageUploader from './image-upload-example.vue';
import axios from 'axios';

// 响应式状态
const userAvatar = ref('');
const saving = ref(false);

// 处理上传成功事件
const handleUploadSuccess = (url) => {
  userAvatar.value = url;
};

// 保存用户资料
const saveUserProfile = async () => {
  if (!userAvatar.value) return;
  
  try {
    saving.value = true;
    
    // 发送请求更新用户头像
    const response = await axios.post('/api/user/update-avatar', {
      avatarUrl: userAvatar.value
    }, {
      withCredentials: true
    });
    
    if (response.data.success) {
      alert('头像更新成功！');
    } else {
      throw new Error(response.data.message || '更新失败');
    }
  } catch (err) {
    console.error('更新头像失败:', err);
    alert('更新头像失败: ' + (err.message || '未知错误'));
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.app-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.avatar-preview {
  margin-top: 30px;
  text-align: center;
  padding: 20px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  background-color: #fafafa;
}

.avatar-image {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  margin: 20px 0;
  border: 2px solid #1890ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

button {
  background-color: #52c41a;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

button:hover {
  background-color: #73d13d;
}

button:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

h1, h2 {
  color: #333;
}
</style> 