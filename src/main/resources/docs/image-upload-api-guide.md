# 图片上传API指南

## 接口概述

本文档描述了博客系统图片上传相关的API接口。系统提供了两种图片上传方式：

1. **前端直传方式**：获取临时令牌后，由前端直接上传到云存储
2. **服务器中转方式**：先上传到服务器，再由服务器转发到云存储

## 接口详情

### 1. 获取上传令牌（前端直传）

```
GET /api/image/token
```

**权限要求**：用户必须登录（携带有效JWT令牌）

**请求参数**：无

**响应结构**：
```json
{
  "code": 200,
  "data": {
    "uploadUrl": "https://s3-endpoint.example.com",
    "fileName": "随机生成的文件名.jpg",
    "bucket": "你的存储桶名称",
    "path": "user/avatars/随机生成的文件名.jpg",
    "allowedTypes": "image/jpeg,image/png,image/gif",
    "maxFileSize": 5242880,
    "token": "临时上传凭证",
    "expiresAt": 1613456789
  },
  "message": "操作成功"
}
```

### 2. 获取上传策略（前端直传的另一种方式）

```
GET /api/image/policy
```

**权限要求**：用户必须登录（携带有效JWT令牌）

**请求参数**：无

**响应结构**：
```json
{
  "code": 200,
  "data": {
    "fileName": "随机生成的文件名.jpg",
    "path": "user/avatars/随机生成的文件名.jpg",
    "bucket": "你的存储桶名称",
    "allowedTypes": "image/jpeg,image/png,image/gif",
    "maxFileSize": 5242880,
    "credentials": {
      "accessKeyId": "临时访问密钥ID",
      "secretAccessKey": "临时密钥",
      "sessionToken": "临时会话令牌",
      "expires": 1613456789,
      "s3Bucket": "S3存储桶名称",
      "s3Endpoint": "s3存储服务端点"
    }
  },
  "message": "操作成功"
}
```

### 3. 服务器中转上传（适合小文件）

```
POST /api/image/upload
```

**权限要求**：用户必须登录（携带有效JWT令牌）

**请求类型**：`multipart/form-data`

**请求参数**：
- `file`: 要上传的文件（必须）

**响应结构**：
```json
{
  "code": 200,
  "data": {
    "url": "https://s3-endpoint.example.com/user/avatars/文件名.jpg",
    "filename": "文件名.jpg",
    "size": 文件大小（字节）,
    "path": "user/avatars/文件名.jpg"
  },
  "message": "操作成功"
}
```

## 使用示例

### 前端直传示例（Vue 3）

```vue
<template>
  <div>
    <input type="file" @change="handleFileChange" accept="image/*" />
    <button @click="uploadFile" :disabled="!selectedFile || uploading">上传</button>
    <div v-if="uploading">上传中...</div>
    <div v-if="uploadResult">
      <p>上传成功！</p>
      <img :src="uploadResult.url" style="max-width: 300px;" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';

const selectedFile = ref(null);
const uploading = ref(false);
const uploadResult = ref(null);

const handleFileChange = (event) => {
  selectedFile.value = event.target.files[0];
};

const uploadFile = async () => {
  if (!selectedFile.value) return;
  
  try {
    uploading.value = true;
    
    // 方式1：使用服务器中转上传
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    
    const response = await axios.post('/api/image/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (response.data.code === 200) {
      uploadResult.value = response.data.data;
    }
    
    // 方式2：前端直传（使用 token 接口）
    // const tokenResponse = await axios.get('/api/image/token', {
    //   headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    // });
    // 
    // if (tokenResponse.data.code === 200) {
    //   const tokenData = tokenResponse.data.data;
    //   
    //   const formData = new FormData();
    //   formData.append('key', tokenData.path);
    //   formData.append('file', selectedFile.value);
    //   formData.append('token', tokenData.token);
    //   
    //   const uploadResponse = await axios.post(tokenData.uploadUrl, formData);
    //   
    //   uploadResult.value = {
    //     url: `${tokenData.uploadUrl}/${tokenData.path}`,
    //     filename: tokenData.fileName
    //   };
    // }
    
  } catch (error) {
    console.error('上传失败', error);
    alert('上传失败：' + (error.response?.data?.message || error.message));
  } finally {
    uploading.value = false;
  }
};
</script>
```

## 注意事项

1. 文件类型限制：仅支持配置的文件类型（通常是image/jpeg, image/png, image/gif）
2. 文件大小限制：不超过配置的最大文件大小（默认5MB）
3. 上传令牌有效期：通常为1小时，过期需要重新获取
4. 权限要求：所有上传接口都需要用户登录

## 最佳实践

- 小文件（<2MB）建议使用服务器中转上传（/api/image/upload）方式
- 大文件（>2MB）建议使用前端直传方式（/api/image/token）
- 始终进行前端文件类型和大小验证，避免无效请求
- 妥善处理上传错误，提供用户友好的错误提示 