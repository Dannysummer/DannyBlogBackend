# 图片管理API指南

本文档描述了博客系统图片管理模块的API接口，包括图片的上传、查询、删除等功能。图片管理功能基于AWS S3 SDK实现，支持图片直传和缩略图预览。

## 功能概述

- 图片上传：支持单张和多张图片上传，自动生成缩略图
- 图片查询：支持按类型、创建时间、关键字查询图片
- 图片删除：支持删除指定图片
- 缩略图预览：自动为上传的图片生成等比例缩略图

## API接口

### 1. 获取图片列表

```
GET /api/images
```

**权限要求**：无（公开接口）

**查询参数**：
- `type`：（可选）图片类型，可选值：`all`（默认）、`image`、`avatar`、`cover`、`article`
- `sort`：（可选）排序方式，可选值：`newest`（默认）、`oldest`
- `search`：（可选）搜索关键词，按图片名称模糊匹配

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "example.jpg",
      "url": "https://s3-endpoint.example.com/user/images/example.jpg",
      "thumbnailUrl": "https://s3-endpoint.example.com/user/images/example_thumb.jpg",
      "type": "image",
      "size": 1024000,
      "path": "user/images/example.jpg",
      "contentType": "image/jpeg",
      "createdAt": "2023-03-15T10:30:45",
      "updatedAt": "2023-03-15T10:30:45"
    }
  ],
  "message": "操作成功"
}
```

### 2. 获取特定图片详情

```
GET /api/images/{id}
```

**权限要求**：无（公开接口）

**路径参数**：
- `id`：图片ID

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "example.jpg",
    "url": "https://s3-endpoint.example.com/user/images/example.jpg",
    "thumbnailUrl": "https://s3-endpoint.example.com/user/images/example_thumb.jpg",
    "type": "image",
    "size": 1024000,
    "path": "user/images/example.jpg",
    "contentType": "image/jpeg",
    "createdAt": "2023-03-15T10:30:45",
    "updatedAt": "2023-03-15T10:30:45"
  },
  "message": "操作成功"
}
```

### 3. 上传图片

```
POST /api/images/upload
```

**权限要求**：用户必须登录（携带有效JWT令牌）

**请求类型**：`multipart/form-data`

**请求参数**：
- `files`：要上传的图片文件（必须，可多个）
- `type`：（可选）图片类型，默认为"image"，可选值：`image`、`avatar`、`cover`、`article`

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 2,
      "name": "new-image.png",
      "url": "https://s3-endpoint.example.com/user/images/new-image.png",
      "thumbnailUrl": "https://s3-endpoint.example.com/user/images/new-image_thumb.png",
      "type": "image",
      "size": 2048000,
      "path": "user/images/new-image.png",
      "contentType": "image/png",
      "createdAt": "2023-03-15T11:45:30",
      "updatedAt": "2023-03-15T11:45:30"
    }
  ],
  "message": "操作成功"
}
```

### 4. 删除图片

```
DELETE /api/images/{id}
```

**权限要求**：用户必须登录（携带有效JWT令牌）

**路径参数**：
- `id`：要删除的图片ID

**响应示例**：
```json
{
  "code": 200,
  "data": null,
  "message": "操作成功"
}
```

## 使用示例

### 前端Vue上传图片示例

```vue
<template>
  <div>
    <input type="file" @change="handleFileChange" accept="image/*" multiple />
    <button @click="uploadFiles" :disabled="files.length === 0">上传</button>
    
    <div v-if="uploadedImages.length > 0">
      <h3>已上传图片</h3>
      <div class="images-grid">
        <div v-for="image in uploadedImages" :key="image.id" class="image-card">
          <!-- 使用缩略图显示预览 -->
          <img :src="image.thumbnailUrl || image.url" alt="缩略图" />
          <div>{{ image.name }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';

const files = ref([]);
const uploadedImages = ref([]);

const handleFileChange = (event) => {
  files.value = Array.from(event.target.files);
};

const uploadFiles = async () => {
  if (files.value.length === 0) return;
  
  const formData = new FormData();
  files.value.forEach(file => {
    formData.append('files', file);
  });
  formData.append('type', 'image'); // 可以是image, avatar, cover, article
  
  try {
    const response = await axios.post('http://localhost:8088/api/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (response.data.code === 200) {
      uploadedImages.value = response.data.data;
      alert('上传成功！');
      files.value = [];
    }
  } catch (error) {
    console.error('上传失败', error);
    alert('上传失败：' + (error.response?.data?.message || error.message));
  }
};
</script>

<style scoped>
.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  margin-top: 20px;
}

.image-card {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  text-align: center;
}

.image-card img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  margin-bottom: 5px;
}
</style>
```

## 缩略图功能说明

系统会自动为上传的图片生成缩略图，默认尺寸为200x200像素，但会保持原图比例。缩略图URL可通过`thumbnailUrl`字段获取，建议在图片列表和预览时优先使用缩略图，以提高加载速度和减少带宽占用。

## 图片类型说明

- `image`：普通图片
- `avatar`：用户头像
- `cover`：文章封面
- `article`：文章内容图片

## 安全建议

1. 对于敏感操作（如删除），建议在前端实现二次确认
2. 上传图片前应进行类型和大小检查，默认限制为10MB
3. 敏感操作需要携带有效的JWT令牌
4. 图片上传成功后，应妥善保存返回的图片信息，特别是URL和ID 