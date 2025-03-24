<!--
  多吉云图床上传示例代码 - Vue 3 + AWS S3 SDK版本
-->

<template>
  <div class="uploader-container">
    <h2>图片上传 (S3 SDK)</h2>
    <div class="file-input">
      <input type="file" accept="image/*" @change="handleFileChange" />
      <button @click="handleUpload" :disabled="!selectedFile || uploading">
        {{ uploading ? '上传中...' : '上传图片' }}
      </button>
    </div>
    
    <p v-if="error" class="error-message">{{ error }}</p>
    
    <div v-if="uploadProgress > 0 && uploadProgress < 100" class="progress-container">
      <div class="progress-bar" :style="{ width: uploadProgress + '%' }"></div>
      <div class="progress-text">{{ uploadProgress.toFixed(1) }}%</div>
    </div>
    
    <div v-if="imageUrl" class="upload-result">
      <h3>上传成功！</h3>
      <img :src="imageUrl" alt="已上传图片" class="preview-image" />
      <p class="image-url">图片链接: {{ imageUrl }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

// 尝试导入 AWS SDK
let S3;
let Credentials;
try {
  const AWS = await import('aws-sdk');
  S3 = AWS.S3;
  Credentials = AWS.Credentials;
} catch (e) {
  console.warn('AWS SDK 加载失败，将使用服务端上传：', e);
}

// 定义组件将发射的事件
const emit = defineEmits(['upload-success', 'upload-error']);

// 响应式状态
const selectedFile = ref(null);
const uploading = ref(false);
const imageUrl = ref('');
const error = ref('');
const uploadProgress = ref(0);
const s3Client = ref(null);

// 处理文件选择
const handleFileChange = (event) => {
  selectedFile.value = event.target.files[0];
  error.value = '';
  uploadProgress.value = 0;
};

// 检查文件类型和大小是否符合要求
const validateFile = (file, allowedTypes, maxFileSize) => {
  if (!allowedTypes.includes(file.type)) {
    return `不支持的文件类型：${file.type}，支持的类型：${allowedTypes}`;
  }
  
  if (file.size > maxFileSize) {
    const maxSizeMB = maxFileSize / (1024 * 1024);
    return `文件太大，最大允许 ${maxSizeMB} MB`;
  }
  
  return null;
};

// 使用 S3 SDK 上传
const uploadWithS3SDK = async (policy) => {
  try {
    const { credentials, path, s3Bucket, s3Endpoint } = policy;
    
    // 创建 S3 客户端
    const s3 = new S3({
      accessKeyId: credentials.accessKeyId,
      secretAccessKey: credentials.secretAccessKey,
      sessionToken: credentials.sessionToken,
      endpoint: `https://${s3Endpoint}`,
      region: 'automatic',
      s3ForcePathStyle: true
    });
    
    // 创建上传参数
    const params = {
      Bucket: s3Bucket,
      Key: path,
      Body: selectedFile.value,
      ContentType: selectedFile.value.type
    };
    
    // 执行上传请求
    const upload = s3.upload(params);
    
    // 监听上传进度
    upload.on('httpUploadProgress', (progress) => {
      const percentage = (progress.loaded / progress.total) * 100;
      uploadProgress.value = percentage;
    });
    
    // 等待上传完成
    const data = await upload.promise();
    
    // 构造返回的文件URL
    return `https://${s3Endpoint}/${path}`;
  } catch (err) {
    console.error('S3 SDK 上传失败:', err);
    throw new Error('S3 SDK 上传失败: ' + err.message);
  }
};

// 通过服务端上传文件
const uploadViaServer = async () => {
  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    
    const response = await axios.post('/api/image/upload', formData, {
      withCredentials: true,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        const percentage = (progressEvent.loaded / progressEvent.total) * 100;
        uploadProgress.value = percentage;
      }
    });
    
    if (response.data.success) {
      return response.data.data.url;
    } else {
      throw new Error(response.data.message || '上传失败');
    }
  } catch (err) {
    console.error('服务端上传失败:', err);
    throw new Error('服务端上传失败: ' + (err.response?.data?.message || err.message));
  }
};

// 获取上传令牌并上传图片
const handleUpload = async () => {
  if (!selectedFile.value) {
    error.value = '请先选择文件';
    return;
  }
  
  try {
    uploading.value = true;
    uploadProgress.value = 0;
    
    // 从服务器获取上传策略
    const policyResponse = await axios.get('/api/image/policy', {
      withCredentials: true
    });
    
    if (!policyResponse.data.success) {
      throw new Error(policyResponse.data.message || '获取上传策略失败');
    }
    
    const policy = policyResponse.data.data;
    
    // 验证文件
    const validationError = validateFile(
      selectedFile.value, 
      policy.allowedTypes.split(','), 
      policy.maxFileSize
    );
    
    if (validationError) {
      throw new Error(validationError);
    }
    
    // 上传文件 - 根据是否有S3 SDK选择上传方式
    let uploadedFileUrl;
    
    if (S3 && Credentials) {
      console.log('使用 S3 SDK 上传...');
      uploadedFileUrl = await uploadWithS3SDK(policy);
    } else {
      console.log('使用服务端上传...');
      uploadedFileUrl = await uploadViaServer();
    }
    
    // 设置上传结果
    imageUrl.value = uploadedFileUrl;
    
    // 发射上传成功事件
    emit('upload-success', uploadedFileUrl);
    
    console.log('上传成功:', uploadedFileUrl);
  } catch (err) {
    console.error('上传过程发生错误:', err);
    error.value = err.message || '上传图片失败';
    
    // 发射上传错误事件
    emit('upload-error', err.message || '上传图片失败');
  } finally {
    uploading.value = false;
  }
};
</script>

<style scoped>
.uploader-container {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.file-input {
  display: flex;
  margin-bottom: 16px;
  gap: 10px;
}

.error-message {
  color: #ff4d4f;
  font-size: 14px;
  margin: 8px 0;
}

.progress-container {
  height: 20px;
  width: 100%;
  background-color: #f0f0f0;
  border-radius: 4px;
  margin: 10px 0;
  position: relative;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background-color: #1890ff;
  transition: width 0.3s ease;
}

.progress-text {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5);
}

.upload-result {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: #f8f8f8;
}

.preview-image {
  max-width: 100%;
  max-height: 300px;
  margin: 10px 0;
  border-radius: 4px;
}

.image-url {
  word-break: break-all;
  font-family: monospace;
  padding: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
}

button {
  background-color: #1890ff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #40a9ff;
}

button:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}
</style> 