# 多吉云对象存储Markdown文章上传指南

本文档详细说明如何使用多吉云对象存储上传和管理Markdown文章文件，包括上传接口、参数说明、请求示例和响应格式。

## 功能概述

系统支持将Markdown文章文件上传到多吉云对象存储，并提供以下功能：

- 获取上传策略和临时凭证
- 直接上传Markdown文章文件（文件将以`.md`格式保存）
- 带进度监控的文章上传
- 文章列表查询（包括按状态查询：已发布、草稿、已删除）
- 文章删除
- 保存文章草稿
- 管理文章状态（发布、设为草稿、移入回收站）
- 标记/取消标记热门文章
- 获取热门文章列表

所有接口都支持认证保护，确保只有已登录用户才能上传和管理自己的文章。上传的文章文件将保存在多吉云对象存储中的`articles/`目录下，并在数据库中保存相关元数据。

## API端点

### 1. 获取上传策略

获取直接上传到多吉云对象存储所需的临时凭证和策略。系统会自动生成一个带有`articles/`前缀的随机文件名。

- **URL**: `/api/article/policy`
- **方法**: `GET`
- **权限要求**: 需要身份认证
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": {
      "accessKeyId": "临时访问密钥ID",
      "secretAccessKey": "临时秘密访问密钥",
      "sessionToken": "临时会话令牌",
      "expires": 1742221496,
      "s3Bucket": "存储桶名称",
      "s3Endpoint": "S3终端节点URL",
      "path": "articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md" // 注意路径前缀为articles/
    }
  }
  ```

### 2. 直接上传文章文件

将Markdown文章文件上传到服务器，由服务器上传到多吉云对象存储。无论原始文件扩展名是什么，系统都会确保文件以`.md`格式保存。

- **URL**: `/api/article/upload`
- **方法**: `POST`
- **权限要求**: 需要身份认证
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `file`: (必填) Markdown文件
  - `title`: (可选) 文章标题，如不提供则使用文件名(不含扩展名)
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": {
      "id": 1,
      "title": "文章标题",
      "fileUrl": "https://example.com/articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md",
      "filePath": "articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md",
      "fileSize": 1024,
      "fileType": "text/markdown",
      "content": null,
      "createdAt": "2023-01-01T12:00:00",
      "updatedAt": "2023-01-01T12:00:00"
    }
  }
  ```

### 3. 带进度监控的文章上传

上传文章文件并通过WebSocket提供实时进度更新。同样，无论原始文件扩展名是什么，系统都会确保文件以`.md`格式保存在`articles/`目录下。

- **URL**: `/api/article/upload-with-progress`
- **方法**: `POST`
- **权限要求**: 需要身份认证
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `file`: (必填) Markdown文件
  - `title`: (可选) 文章标题，如不提供则使用文件名(不含扩展名)
- **WebSocket订阅主题**: `/topic/upload-progress/{username}`
- **WebSocket进度消息格式**:
  ```json
  {
    "uploadId": "上传任务唯一标识",
    "filename": "文件名",
    "bytesTransferred": 1024,
    "totalBytes": 2048,
    "percentage": 50.0,
    "status": "IN_PROGRESS", // STARTED, IN_PROGRESS, COMPLETED, FAILED
    "errorMessage": null
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": {
      "id": 1,
      "title": "文章标题",
      "fileUrl": "https://example.com/articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md",
      "filePath": "articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md",
      "fileSize": 1024,
      "fileType": "text/markdown",
      "content": null,
      "createdAt": "2023-01-01T12:00:00",
      "updatedAt": "2023-01-01T12:00:00"
    }
  }
  ```

### 4. 获取用户文章列表

获取当前登录用户的所有文章列表。每篇文章包含原始Markdown文件的URL，前端可以通过这个URL直接读取Markdown内容。

- **URL**: `/api/article/list`
- **方法**: `GET`
- **权限要求**: 需要身份认证
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": [
      {
        "id": 1,
        "title": "文章标题1",
        "fileUrl": "https://example.com/articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md",
        "filePath": "articles/5d7f8a6c32b94e7d9a1c8b4f0e3d2a1b.md",
        "fileSize": 1024,
        "fileType": "text/markdown",
        "content": null,
        "description": "文章描述",
        "category": "技术",
        "cover": "https://example.com/cover.jpg",
        "views": 10,
        "license": "CC BY-NC-SA 4.0",
        "licenseDescription": "知识共享 署名-非商业性使用-相同方式共享 4.0",
        "status": "PUBLISHED",
        "isFeatured": true,
        "tags": "标签1\\标签2\\标签3",
        "tagArray": ["标签1", "标签2", "标签3"],
        "createdAt": "2023-01-01T12:00:00",
        "updatedAt": "2023-01-01T12:00:00"
      },
      // ... 更多文章
    ]
  }
  ```

### 4.1 按状态获取用户文章列表

获取当前登录用户特定状态（已发布、草稿或已删除）的文章列表。

- **URL**: `/api/article/list/{status}`
- **方法**: `GET`
- **权限要求**: 需要身份认证
- **参数**:
  - `status`: (路径参数) 文章状态，可选值：`PUBLISHED`, `DRAFT`, `DELETED`
- **成功响应**: 与`/api/article/list`接口相同，但只返回指定状态的文章

### 5. 删除文章

删除指定ID的文章记录及其关联的存储文件。

- **URL**: `/api/article/{id}`
- **方法**: `DELETE`
- **权限要求**: 需要身份认证
- **参数**:
  - `id`: (路径参数) 文章ID
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "文章删除成功",
    "data": null
  }
  ```

### 6. 获取文章内容（解决CORS问题的代理接口）

通过后端服务器获取文章内容，解决直接请求多吉云存储时可能出现的CORS跨域问题。

- **URL**: `/api/article/content/{id}`
- **方法**: `GET`
- **权限要求**: 需要身份认证
- **参数**:
  - `id`: (路径参数) 文章ID
- **成功响应**:
  ```
  # 文章标题
  
  这里是Markdown格式的文章内容...
  ```
  **注意**: 该接口直接返回Markdown文本内容，不包装在JSON中。

- **错误响应**:
  ```json
  {
    "success": false,
    "message": "获取文章内容失败：原因...",
    "data": null
  }
  ```

### 7. 保存文章草稿

将文章内容作为草稿直接保存到数据库，不上传到多吉云存储。可用于文章编辑过程中的自动保存和临时存储。

- **URL**: `/api/article/articleDraftSave`
- **方法**: `POST`
- **权限要求**: 需要身份认证
- **Content-Type**: `application/x-www-form-urlencoded` 或 `multipart/form-data`
- **参数**:
  - `content`: (必填) 文章内容（Markdown格式）
  - `title`: (可选) 文章标题，如不提供则使用"未命名草稿"
  - `description`: (可选) 文章描述
  - `category`: (可选) 文章分类
  - `cover`: (可选) 封面图片URL
  - `articleId`: (可选) 文章ID，如果提供则更新现有文章，否则创建新草稿
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": {
      "id": 1,
      "title": "文章草稿标题",
      "content": "# Markdown格式的文章内容\n\n这是一篇草稿...",
      "description": "文章描述",
      "category": "技术",
      "cover": "https://example.com/images/cover.jpg",
      "views": 0,
      "author": "username",
      "license": "CC BY-NC-SA 4.0",
      "licenseDescription": "知识共享 署名-非商业性使用-相同方式共享 4.0",
      "fileUrl": "draft://local",
      "filePath": "draft://uuid",
      "fileSize": 0,
      "fileType": "text/markdown",
      "status": "DRAFT",
      "isFeatured": false,
      "tags": null,
      "tagArray": [],
      "createdAt": "2025-03-24 17:48:48",
      "updatedAt": "2025-03-24 17:48:48",
      "userId": 1
    }
  }
  ```

### 8. 获取热门文章列表

获取所有被标记为热门的文章列表。

- **URL**: `/api/article/featured`
- **方法**: `GET`
- **权限要求**: 无需身份认证
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": [
      {
        "id": 1,
        "title": "热门文章标题1",
        "description": "文章描述",
        "category": "技术",
        "cover": "https://example.com/cover.jpg",
        "views": 1500,
        "license": "CC BY-NC-SA 4.0",
        "licenseDescription": "知识共享 署名-非商业性使用-相同方式共享 4.0",
        "status": "PUBLISHED",
        "isFeatured": true,
        "tags": "标签1\\标签2",
        "tagArray": ["标签1", "标签2"],
        "createdAt": "2023-01-01T12:00:00",
        "updatedAt": "2023-01-01T12:00:00"
        // ... 其他字段
      },
      // ... 更多热门文章
    ]
  }
  ```

### 9. 获取热门文章（按访问量）

获取访问量最高的文章列表。

- **URL**: `/api/article/popular`
- **方法**: `GET`
- **权限要求**: 无需身份认证
- **参数**:
  - `limit`: (可选) 返回的文章数量，默认为10
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": [
      {
        "id": 1,
        "title": "热门文章标题1",
        "views": 2000,
        "status": "PUBLISHED",
        "isFeatured": false,
        // ... 其他字段
      },
      // ... 更多热门文章
    ]
  }
  ```

### 10. 更新文章状态

更新文章的状态（已发布、草稿或已删除）。

- **URL**: `/api/article/{id}/status`
- **方法**: `POST`
- **权限要求**: 需要身份认证
- **参数**:
  - `id`: (路径参数) 文章ID
  - `status`: (请求参数) 文章新状态，可选值：`PUBLISHED`, `DRAFT`, `DELETED`
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": {
      "id": 1,
      "title": "文章标题",
      "status": "PUBLISHED",
      // ... 其他字段
    }
  }
  ```

### 11. 标记或取消标记热门文章

将文章标记为热门文章或取消热门标记。

- **URL**: `/api/article/{id}/featured`
- **方法**: `POST`
- **权限要求**: 需要身份认证
- **参数**:
  - `id`: (路径参数) 文章ID
  - `featured`: (请求参数, 可选) 是否标记为热门，默认为true
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "成功",
    "data": {
      "id": 1,
      "title": "文章标题",
      "isFeatured": true,
      // ... 其他字段
    }
  }
  ```

## 前端集成示例

### 基本文件上传

```javascript
// 使用普通表单上传
async function uploadArticle(file, title) {
  const formData = new FormData();
  formData.append('file', file);
  if (title) {
    formData.append('title', title);
  }
  
  try {
    const response = await fetch('/api/article/upload', {
      method: 'POST',
      body: formData,
      headers: {
        'Authorization': 'Bearer ' + 您的认证令牌
      }
    });
    
    const result = await response.json();
    if (result.success) {
      console.log('文章上传成功:', result.data);
      return result.data;
    } else {
      throw new Error(result.message || '上传失败');
    }
  } catch (error) {
    console.error('文章上传错误:', error);
    throw error;
  }
}
```

### 带进度监控的上传

```javascript
// 使用进度监控上传
async function uploadArticleWithProgress(file, title, onProgress) {
  // 1. 连接WebSocket
  const socket = new SockJS('/ws');
  const stompClient = Stomp.over(socket);
  const username = '当前用户名';
  
  stompClient.connect({}, frame => {
    console.log('已连接到WebSocket');
    
    // 2. 订阅进度更新主题
    stompClient.subscribe('/topic/upload-progress/' + username, message => {
      const progress = JSON.parse(message.body);
      
      // 处理进度更新
      if (onProgress) {
        onProgress(progress);
      }
      
      // 如果上传完成或失败，关闭连接
      if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
        stompClient.disconnect();
      }
    });
    
    // 3. 开始上传
    uploadArticle();
  });
  
  // 4. 执行上传
  async function uploadArticle() {
    const formData = new FormData();
    formData.append('file', file);
    if (title) {
      formData.append('title', title);
    }
    
    try {
      const response = await fetch('/api/article/upload-with-progress', {
        method: 'POST',
        body: formData,
        headers: {
          'Authorization': 'Bearer ' + 您的认证令牌
        }
      });
      
      const result = await response.json();
      if (!result.success) {
        throw new Error(result.message || '上传失败');
      }
    } catch (error) {
      console.error('文章上传错误:', error);
      stompClient.disconnect();
      throw error;
    }
  }
}

// 使用带进度监控的上传
uploadArticleWithProgress(
  file, 
  '文章标题',
  progress => {
    console.log(`上传进度: ${progress.percentage.toFixed(2)}%`);
    // 更新UI进度条
    updateProgressBar(progress.percentage);
  }
);
```

### 获取用户文章列表

```javascript
async function getUserArticles() {
  try {
    const response = await fetch('/api/article/list', {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + 您的认证令牌
      }
    });
    
    const result = await response.json();
    if (result.success) {
      return result.data;
    } else {
      throw new Error(result.message || '获取文章列表失败');
    }
  } catch (error) {
    console.error('获取文章列表错误:', error);
    throw error;
  }
}
```

### 读取文章内容

```javascript
// 使用代理接口获取Markdown内容（推荐，解决CORS问题）
async function fetchArticleContent(articleId) {
  try {
    const response = await fetch(`/api/article/content/${articleId}`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + 您的认证令牌
      }
    });
    
    if (!response.ok) {
      throw new Error(`获取文章内容失败: ${response.status} ${response.statusText}`);
    }
    
    const markdown = await response.text();
    return markdown;
  } catch (error) {
    console.error('读取文章内容错误:', error);
    throw error;
  }
}

// 使用方法
const article = articles[0]; // 从文章列表中获取
const markdown = await fetchArticleContent(article.id);
console.log('文章内容:', markdown);

// 然后可以使用Markdown解析库（如marked.js）将内容渲染为HTML
// const html = marked(markdown);
// document.getElementById('article-content').innerHTML = html;
```

### 删除文章

```javascript
async function deleteArticle(articleId) {
  try {
    const response = await fetch(`/api/article/${articleId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': 'Bearer ' + 您的认证令牌
      }
    });
    
    const result = await response.json();
    if (result.success) {
      console.log('文章删除成功');
      return true;
    } else {
      throw new Error(result.message || '删除失败');
    }
  } catch (error) {
    console.error('文章删除错误:', error);
    throw error;
  }
}
```

### 前端草稿保存示例

```javascript
// 自动保存文章草稿
async function saveArticleDraft(content, title, articleId = null) {
  try {
    const formData = new FormData();
    formData.append('content', content);
    
    if (title) {
      formData.append('title', title);
    }
    
    if (articleId) {
      formData.append('articleId', articleId);
    }
    
    // 可选参数
    // formData.append('description', '文章描述');
    // formData.append('category', '技术');
    // formData.append('cover', '封面图片URL');
    
    const response = await fetch('/api/article/articleDraftSave', {
      method: 'POST',
      body: formData,
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      }
    });
    
    const result = await response.json();
    if (result.success) {
      console.log('草稿保存成功:', result.data);
      return result.data;
    } else {
      throw new Error(result.message || '保存失败');
    }
  } catch (error) {
    console.error('草稿保存错误:', error);
    throw error;
  }
}

// 使用示例 - 编辑器自动保存
const editor = document.getElementById('markdown-editor');
let articleId = null; // 首次保存为null，之后使用返回的ID

// 定时保存
setInterval(async () => {
  try {
    const content = editor.value;
    const title = document.getElementById('article-title').value;
    
    // 保存草稿，获取返回的文章对象
    const savedArticle = await saveArticleDraft(content, title, articleId);
    
    // 更新articleId，以便下次保存时更新而不是创建新草稿
    articleId = savedArticle.id;
    
    console.log('草稿已自动保存，ID:', articleId);
  } catch (error) {
    console.error('自动保存失败:', error);
  }
}, 30000); // 每30秒保存一次
```

### 文章状态管理示例

```javascript
// 更新文章状态
async function updateArticleStatus(articleId, status) {
  try {
    const formData = new FormData();
    formData.append('status', status); // 'PUBLISHED', 'DRAFT', 或 'DELETED'
    
    const response = await fetch(`/api/article/${articleId}/status`, {
      method: 'POST',
      body: formData,
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      }
    });
    
    const result = await response.json();
    if (result.success) {
      console.log(`文章状态已更新为 ${status}:`, result.data);
      return result.data;
    } else {
      throw new Error(result.message || '更新状态失败');
    }
  } catch (error) {
    console.error('更新文章状态错误:', error);
    throw error;
  }
}

// 示例：发布文章
function publishArticle(articleId) {
  updateArticleStatus(articleId, 'PUBLISHED')
    .then(article => {
      alert(`文章"${article.title}"已发布！`);
      // 更新UI
    })
    .catch(error => {
      alert(`发布失败: ${error.message}`);
    });
}

// 示例：移入回收站
function moveToTrash(articleId) {
  updateArticleStatus(articleId, 'DELETED')
    .then(article => {
      alert(`文章"${article.title}"已移入回收站`);
      // 更新UI
    })
    .catch(error => {
      alert(`操作失败: ${error.message}`);
    });
}
```

### 热门文章管理示例

```javascript
// 标记或取消标记热门文章
async function toggleFeaturedArticle(articleId, featured = true) {
  try {
    const formData = new FormData();
    formData.append('featured', featured);
    
    const response = await fetch(`/api/article/${articleId}/featured`, {
      method: 'POST',
      body: formData,
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      }
    });
    
    const result = await response.json();
    if (result.success) {
      console.log(`文章热门标记已${featured ? '添加' : '移除'}:`, result.data);
      return result.data;
    } else {
      throw new Error(result.message || '操作失败');
    }
  } catch (error) {
    console.error('更新热门标记错误:', error);
    throw error;
  }
}

// 获取热门文章列表
async function getFeaturedArticles() {
  try {
    const response = await fetch('/api/article/featured', {
      method: 'GET'
    });
    
    const result = await response.json();
    if (result.success) {
      return result.data;
    } else {
      throw new Error(result.message || '获取热门文章失败');
    }
  } catch (error) {
    console.error('获取热门文章错误:', error);
    throw error;
  }
}

// 获取访问量最高的文章
async function getPopularArticles(limit = 10) {
  try {
    const response = await fetch(`/api/article/popular?limit=${limit}`, {
      method: 'GET'
    });
    
    const result = await response.json();
    if (result.success) {
      return result.data;
    } else {
      throw new Error(result.message || '获取热门文章失败');
    }
  } catch (error) {
    console.error('获取热门文章错误:', error);
    throw error;
  }
}
```

## 前端内容查看示例

以下是一个简单的前端示例，展示如何查看文章内容：

```vue
<template>
  <div class="article-viewer">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="article-content" v-html="htmlContent"></div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { marked } from 'marked'; // 需要安装: npm install marked

export default {
  name: 'ArticleViewer',
  props: {
    articleId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const loading = ref(true);
    const error = ref(null);
    const markdown = ref('');
    const htmlContent = ref('');
    
    const fetchArticleContent = async () => {
      try {
        loading.value = true;
        error.value = null;
        
        // 使用代理接口获取文章内容，避免CORS问题
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/article/content/${props.articleId}`, {
          method: 'GET',
          headers: {
            'Authorization': 'Bearer ' + token
          }
        });
        
        if (!response.ok) {
          throw new Error(`获取文章内容失败: ${response.status} ${response.statusText}`);
        }
        
        markdown.value = await response.text();
        // 将Markdown转换为HTML
        htmlContent.value = marked(markdown.value);
      } catch (err) {
        console.error('读取文章内容错误:', err);
        error.value = err.message || '无法加载文章内容';
      } finally {
        loading.value = false;
      }
    };
    
    onMounted(() => {
      if (props.articleId) {
        fetchArticleContent();
      }
    });
    
    return {
      loading,
      error,
      markdown,
      htmlContent
    };
  }
};
</script>

<style scoped>
.article-viewer {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .error {
  text-align: center;
  padding: 20px;
}

.error {
  color: #f44336;
}

.article-content {
  line-height: 1.6;
}

.article-content h1, .article-content h2 {
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.article-content code {
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  padding: 0.2em 0.4em;
}

.article-content pre code {
  display: block;
  overflow-x: auto;
  padding: 1em;
  background-color: #f6f8fa;
}
</style>
```

使用组件：
```vue
<template>
  <div>
    <h1>文章阅读器</h1>
    <ArticleViewer :articleId="3" />
  </div>
</template>
```

## 注意事项

1. **文件格式**: 系统会自动确保所有上传的文件以`.md`格式保存，即使原始文件扩展名不是`.md`。这保证了前端可以一致地处理Markdown文件。

2. **文件存储路径**: 所有文章文件都存储在多吉云对象存储的`articles/`目录下，与图片文件（存储在`images/`目录）分开管理。

3. **文件类型验证**: 服务器端设置了允许的文件类型，默认为`text/markdown`、`text/plain`和`application/octet-stream`。如果需要支持其他类型，请修改服务器配置。

4. **文件内容读取**: 为避免CORS问题，请使用`/api/article/content/{id}`代理接口获取文章内容，而不是直接通过`fileUrl`读取。直接访问多吉云上的文件可能会遇到跨域限制。

5. **文件大小限制**: 默认最大文件大小为10MB。如果需要上传更大的文件，请修改服务器配置。

6. **安全性考虑**: 所有接口都需要身份认证，确保只有已登录用户才能上传和管理文章。

7. **WebSocket连接**: 使用进度监控功能时，请确保正确处理WebSocket连接的生命周期，避免资源泄漏。

8. **跨域设置**: 如果前端和后端部署在不同的域名下，请确保正确配置CORS设置，允许跨域请求。如果需要直接访问多吉云上的文件，需要在多吉云的管理控制台中配置CORS设置，允许您的前端域名访问。

9. **文章状态管理**: 文章支持三种状态：`PUBLISHED`（已发布）、`DRAFT`（草稿）和`DELETED`（已删除）。默认情况下，通过文件上传创建的文章状态为`PUBLISHED`，通过草稿保存创建的文章状态为`DRAFT`。

10. **热门文章标记**: 文章可以被标记为热门（`isFeatured=true`），这些文章可以通过`/api/article/featured`接口获取。系统还提供了根据访问量获取热门文章的接口`/api/article/popular`。

## 故障排除

1. **文件上传失败**: 检查文件类型和大小是否符合服务器限制，确保用户已正确登录。

2. **CORS错误**: 如果遇到"Access to fetch at ... has been blocked by CORS policy"错误，请使用`/api/article/content/{id}`代理接口获取文章内容，而不是直接通过`fileUrl`访问多吉云上的文件。如果必须直接访问，需要在多吉云控制台配置适当的CORS设置。

3. **无法读取文章内容**: 检查用户认证状态，确保认证令牌有效且未过期。如果使用代理接口仍然失败，检查服务器日志以获取更详细的错误信息。

4. **WebSocket连接失败**: 检查WebSocket配置和防火墙设置，确保WebSocket端点可访问。

5. **无法获取文章列表**: 检查用户认证状态，确保认证令牌有效且未过期。

6. **删除文件失败**: 检查多吉云对象存储的访问权限，确保服务器有删除文件的权限。 