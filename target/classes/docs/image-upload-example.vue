<!--
  图床上传示例代码 - Vue 3 + Axios版本（多吉云OSS）
-->

<template>
  <div class="uploader-container">
    <h2>图片上传</h2>
    <div class="file-input">
      <input type="file" accept="image/*" @change="handleFileChange" />
      <button @click="handleUpload" :disabled="!selectedFile || uploading">
        {{ uploading ? '上传中...' : '上传图片' }}
      </button>
    </div>
    
    <p v-if="error" class="error-message">{{ error }}</p>
    
    <div v-if="imageUrl" class="upload-result">
      <h3>上传成功！</h3>
      <img :src="imageUrl" alt="已上传图片" class="preview-image" />
      <p class="image-url">图片链接: {{ imageUrl }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';

// 定义组件将发射的事件
const emit = defineEmits(['upload-success', 'upload-error']);

// 响应式状态
const selectedFile = ref(null);
const uploading = ref(false);
const imageUrl = ref('');
const error = ref('');

// 处理文件选择
const handleFileChange = (event) => {
  selectedFile.value = event.target.files[0];
  error.value = '';
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

// 获取上传令牌并上传图片
const handleUpload = async () => {
  if (!selectedFile.value) {
    error.value = '请先选择文件';
    return;
  }
  
  try {
    uploading.value = true;
    
    // 先从我们自己的服务器获取上传令牌
    const tokenResponse = await axios.get('/api/image/token', {
      withCredentials: true // 确保发送认证Cookie
    });
    
    if (!tokenResponse.data.success) {
      throw new Error(tokenResponse.data.message || '获取上传令牌失败');
    }
    
    const { 
      token, 
      uploadUrl, 
      fileName, 
      bucket, 
      path, 
      allowedTypes, 
      maxFileSize,
      expiresAt 
    } = tokenResponse.data.data;
    
    // 验证文件
    const validationError = validateFile(
      selectedFile.value, 
      allowedTypes.split(','), 
      maxFileSize
    );
    
    if (validationError) {
      throw new Error(validationError);
    }
    
    // 检查令牌是否过期
    if (Date.now() / 1000 >= expiresAt) {
      throw new Error('上传令牌已过期');
    }
    
    // 准备上传数据
    const formData = new FormData();
    
    // 根据多吉云上传API要求设置参数
    formData.append('file', selectedFile.value);
    
    // 解析token参数（格式为accessKeyId:secretAccessKey:sessionToken）
    const [accessKeyId, secretAccessKey, sessionToken] = token.split(':');
    
    // 添加多吉云OSS上传所需参数
    formData.append('key', path); // 指定文件存储路径
    formData.append('bucket', bucket);
    formData.append('AccessKeyId', accessKeyId);
    formData.append('AccessKeySecret', secretAccessKey);
    formData.append('SecurityToken', sessionToken);
    
    console.log('正在上传文件到多吉云OSS：', {
      uploadUrl,
      fileName,
      path,
      expiresAt,
      accessKeyId
    });
    
    // 上传到多吉云图床服务
    const uploadResponse = await axios.post(uploadUrl, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    
    console.log('多吉云上传响应：', uploadResponse.data);
    
    // 处理上传响应 - 多吉云返回格式
    if (uploadResponse.data) {
      console.log('多吉云上传响应详细信息:', JSON.stringify(uploadResponse.data));
      
      let fileUrl = '';
      
      // 尝试提取URL - 根据实际返回格式进行调整
      if (uploadResponse.data.code === 200 && uploadResponse.data.data && uploadResponse.data.data.url) {
        // 标准多吉云API响应格式
        fileUrl = uploadResponse.data.data.url;
      } else if (uploadResponse.data.Location) {
        // S3兼容格式
        fileUrl = uploadResponse.data.Location;
      } else if (uploadResponse.data.url) {
        // 简单URL格式
        fileUrl = uploadResponse.data.url;
      } else if (typeof uploadResponse.data === 'string' && uploadResponse.data.includes('http')) {
        // 直接返回URL字符串
        fileUrl = uploadResponse.data;
      } else {
        // 构造可能的URL
        fileUrl = `https://${s3EndpointHost}/${path}`;
      }
      
      if (fileUrl) {
        imageUrl.value = fileUrl;
        
        // 发射上传成功事件
        emit('upload-success', fileUrl);
      } else {
        throw new Error('无法从响应中提取图片URL');
      }
    } else {
      throw new Error('上传失败，未获取到响应数据');
    }
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