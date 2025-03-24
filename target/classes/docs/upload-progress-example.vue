<template>
  <div class="upload-container">
    <h2>图片上传（带实时进度）</h2>
    
    <!-- 文件选择区域 -->
    <div class="file-upload-area">
      <input 
        type="file" 
        ref="fileInput" 
        @change="handleFileSelected" 
        accept="image/*" 
        multiple 
        class="file-input"
      />
      <button @click="triggerFileInput" class="select-btn">选择图片</button>
      <button @click="uploadFiles" :disabled="files.length === 0 || uploading" class="upload-btn">
        {{ uploading ? '上传中...' : '开始上传' }}
      </button>
    </div>
    
    <!-- 已选文件列表 -->
    <div v-if="files.length > 0" class="selected-files">
      <h3>已选择 {{ files.length }} 个文件</h3>
      <ul class="file-list">
        <li v-for="(file, index) in files" :key="index" class="file-item">
          <div class="file-info">
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatSize(file.size) }}</span>
          </div>
          <button @click="removeFile(index)" class="remove-btn">
            <span>×</span>
          </button>
        </li>
      </ul>
    </div>
    
    <!-- 上传进度展示 -->
    <div v-if="uploadProgresses.length > 0" class="upload-progress-container">
      <h3>上传进度</h3>
      <div 
        v-for="(progress, index) in uploadProgresses" 
        :key="progress.uploadId" 
        class="progress-item"
      >
        <div class="progress-info">
          <span class="progress-filename">{{ progress.filename }}</span>
          <span class="progress-status">{{ getStatusText(progress.status) }}</span>
        </div>
        <div class="progress-bar-container">
          <div 
            class="progress-bar" 
            :class="{'success': progress.status === 'COMPLETED', 'error': progress.status === 'FAILED'}"
            :style="{width: `${progress.percentage}%`}"
          ></div>
        </div>
        <div class="progress-details">
          <span>{{ Math.round(progress.percentage) }}%</span>
          <span>{{ formatSize(progress.bytesTransferred) }} / {{ formatSize(progress.totalBytes) }}</span>
          <span v-if="progress.status === 'FAILED'" class="error-message">
            {{ progress.errorMessage || '上传失败' }}
          </span>
        </div>
      </div>
    </div>
    
    <!-- 上传结果展示 -->
    <div v-if="uploadedImages.length > 0" class="uploaded-images">
      <h3>上传成功的图片</h3>
      <div class="image-grid">
        <div 
          v-for="image in uploadedImages" 
          :key="image.id" 
          class="image-card"
        >
          <img :src="image.thumbnailUrl || image.url" :alt="image.name" class="thumbnail" />
          <div class="image-info">
            <span class="image-name">{{ image.name }}</span>
            <span class="image-size">{{ formatSize(image.size) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export default {
  name: 'ImageUploadWithProgress',
  
  setup() {
    // 状态变量
    const files = ref([]);
    const uploading = ref(false);
    const uploadProgresses = ref([]);
    const uploadedImages = ref([]);
    const fileInput = ref(null);
    
    // WebSocket相关
    let stompClient = null;
    let subscription = null;
    
    // 生命周期钩子
    onMounted(() => {
      connectWebSocket();
    });
    
    onUnmounted(() => {
      disconnectWebSocket();
    });
    
    // WebSocket连接
    const connectWebSocket = () => {
      const socket = new SockJS('/ws');
      stompClient = Stomp.over(socket);
      
      stompClient.connect({}, frame => {
        console.log('WebSocket连接成功:', frame);
        
        // 获取当前用户名（假设从某处获取，实际项目中应从认证上下文获取）
        const username = getCurrentUsername();
        
        // 订阅个人上传进度通道
        subscription = stompClient.subscribe(`/topic/upload-progress/${username}`, message => {
          const progress = JSON.parse(message.body);
          console.log('收到进度更新:', progress);
          
          // 更新进度列表
          const index = uploadProgresses.value.findIndex(p => p.uploadId === progress.uploadId);
          if (index >= 0) {
            uploadProgresses.value[index] = progress;
          } else {
            uploadProgresses.value.push(progress);
          }
        });
      }, error => {
        console.error('WebSocket连接失败:', error);
      });
    };
    
    // 断开WebSocket连接
    const disconnectWebSocket = () => {
      if (subscription) {
        subscription.unsubscribe();
      }
      if (stompClient) {
        stompClient.disconnect();
      }
    };
    
    // 获取当前用户名（实际项目中应从认证上下文获取）
    const getCurrentUsername = () => {
      // 这里应该从认证上下文或localStorage等地方获取用户名
      // 示例中使用硬编码，实际使用请替换
      return localStorage.getItem('username') || 'anonymous';
    };
    
    // 触发文件选择器
    const triggerFileInput = () => {
      fileInput.value.click();
    };
    
    // 处理文件选择
    const handleFileSelected = (event) => {
      const selectedFiles = Array.from(event.target.files);
      files.value = [...files.value, ...selectedFiles];
    };
    
    // 移除已选文件
    const removeFile = (index) => {
      files.value.splice(index, 1);
    };
    
    // 上传文件
    const uploadFiles = async () => {
      if (files.value.length === 0 || uploading.value) return;
      
      uploading.value = true;
      uploadProgresses.value = []; // 清空之前的进度
      
      try {
        const formData = new FormData();
        files.value.forEach(file => {
          formData.append('files', file);
        });
        formData.append('type', 'image'); // 可以根据需要修改类型
        
        const response = await fetch('/api/images/upload-with-progress', {
          method: 'POST',
          body: formData,
          credentials: 'include'
        });
        
        const result = await response.json();
        if (result.success) {
          console.log('上传成功:', result.data);
          uploadedImages.value = [...uploadedImages.value, ...result.data];
          files.value = []; // 清空已选文件
        } else {
          console.error('上传失败:', result.message);
          alert(`上传失败: ${result.message}`);
        }
      } catch (error) {
        console.error('上传发生错误:', error);
        alert(`上传发生错误: ${error.message}`);
      } finally {
        uploading.value = false;
      }
    };
    
    // 格式化文件大小
    const formatSize = (bytes) => {
      if (bytes === 0) return '0 B';
      
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      
      return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
    };
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'STARTED': '准备上传',
        'INITIATED': '初始化',
        'IN_PROGRESS': '上传中',
        'COMPLETED': '已完成',
        'FAILED': '失败'
      };
      return statusMap[status] || status;
    };
    
    return {
      files,
      fileInput,
      uploading,
      uploadProgresses,
      uploadedImages,
      triggerFileInput,
      handleFileSelected,
      removeFile,
      uploadFiles,
      formatSize,
      getStatusText
    };
  }
};
</script>

<style scoped>
.upload-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

.file-upload-area {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.file-input {
  display: none;
}

button {
  padding: 10px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background-color 0.3s;
}

.select-btn {
  background-color: #4a90e2;
  color: white;
}

.upload-btn {
  background-color: #2ecc71;
  color: white;
}

.upload-btn:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.selected-files {
  margin-bottom: 20px;
}

.file-list {
  list-style: none;
  padding: 0;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.file-info {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-weight: bold;
}

.file-size {
  color: #7f8c8d;
  font-size: 0.8em;
}

.remove-btn {
  background-color: #e74c3c;
  color: white;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.upload-progress-container {
  margin-bottom: 20px;
}

.progress-item {
  margin-bottom: 15px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.progress-filename {
  font-weight: bold;
}

.progress-status {
  color: #7f8c8d;
}

.progress-bar-container {
  width: 100%;
  height: 10px;
  background-color: #ecf0f1;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 5px;
}

.progress-bar {
  height: 100%;
  background-color: #3498db;
  transition: width 0.3s;
}

.progress-bar.success {
  background-color: #2ecc71;
}

.progress-bar.error {
  background-color: #e74c3c;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  font-size: 0.8em;
  color: #7f8c8d;
}

.error-message {
  color: #e74c3c;
  font-weight: bold;
}

.uploaded-images {
  margin-top: 30px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.image-card {
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.thumbnail {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

.image-info {
  padding: 10px;
  font-size: 0.8em;
}

.image-name {
  display: block;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.image-size {
  color: #7f8c8d;
}
</style> 