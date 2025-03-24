# S3文件上传进度跟踪功能实现文档

## 功能概述

本文档描述了基于WebSocket的实时文件上传进度跟踪功能，它允许客户端在上传文件到服务器时实时获取上传进度信息。此功能特别适用于大文件上传场景，提供了更好的用户体验。

## 技术实现原理

### 后端实现

1. **WebSocket通信**：使用Spring WebSocket支持，通过STOMP协议发送实时进度消息
2. **进度跟踪**：自定义`UploadProgressListener`监听上传进度并发送WebSocket消息
3. **流包装**：使用`ProgressTrackingInputStream`包装输入流以跟踪读取字节数

### 关键组件

1. **WebSocketConfig**：配置WebSocket端点和消息代理
2. **UploadProgressListener**：上传进度监听器，发送进度消息到客户端
3. **ProgressTrackingRequestBody**：请求体包装器，用于跟踪上传进度
4. **ImageUploadController**：提供带进度监控的上传端点

## API端点

```
POST /api/image/upload-with-progress
```

### 请求参数

| 参数名    | 类型   | 必填 | 描述                |
|-----------|--------|------|---------------------|
| file      | File   | 是   | 要上传的文件        |
| username  | String | 否   | 用户标识，用于WebSocket通信 |

### 响应格式

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "url": "https://example.com/path/to/file.jpg",
    "filename": "example.jpg",
    "size": 1024000,
    "path": "images/uuid-filename.jpg"
  }
}
```

## WebSocket进度消息

### 订阅通道

```
/topic/upload-progress/{username}
```

### 消息格式

```json
{
  "uploadId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "example.jpg",
  "bytesTransferred": 512000,
  "totalBytes": 1024000,
  "percentage": 50.0,
  "status": "IN_PROGRESS",
  "errorMessage": null
}
```

状态字段(`status`)可能的值：
- `STARTED`：开始上传
- `INITIATED`：初始化
- `IN_PROGRESS`：上传中
- `COMPLETED`：已完成
- `FAILED`：失败

## 前端实现

### 1. 建立WebSocket连接

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

// 连接WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 连接成功后订阅进度通道
stompClient.connect({}, frame => {
  console.log('WebSocket连接成功');
  
  // 获取当前用户标识
  const username = localStorage.getItem('username') || 'default_user';
  
  // 订阅进度通道
  stompClient.subscribe(`/topic/upload-progress/${username}`, message => {
    const progress = JSON.parse(message.body);
    console.log('上传进度更新:', progress);
    updateProgressUI(progress);
  });
});
```

### 2. 发送上传请求

```javascript
async function uploadFileWithProgress(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  // 获取用户标识（应从用户认证上下文获取）
  const username = localStorage.getItem('username') || 'default_user';
  formData.append('username', username);
  
  try {
    const response = await fetch('/api/image/upload-with-progress', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    });
    
    const result = await response.json();
    if (result.success) {
      console.log('上传成功:', result.data);
      return result.data;
    } else {
      console.error('上传失败:', result.message);
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('上传请求错误:', error);
    throw error;
  }
}
```

### 3. 进度UI更新

```javascript
function updateProgressUI(progress) {
  const progressBar = document.getElementById('upload-progress-bar');
  const progressText = document.getElementById('upload-progress-text');
  
  // 更新进度条
  progressBar.style.width = `${progress.percentage}%`;
  
  // 更新进度文本
  progressText.textContent = `${Math.round(progress.percentage)}% - ${formatFileSize(progress.bytesTransferred)} / ${formatFileSize(progress.totalBytes)}`;
  
  // 处理不同状态
  if (progress.status === 'COMPLETED') {
    progressBar.classList.add('completed');
  } else if (progress.status === 'FAILED') {
    progressBar.classList.add('failed');
    progressText.textContent = `上传失败: ${progress.errorMessage || '未知错误'}`;
  }
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
}
```

## Vue组件示例

项目中提供了一个完整的Vue 3组件示例：`src/main/resources/docs/upload-progress-example.vue`。
该组件提供了完整的文件选择、上传、进度显示和结果展示功能。

### 使用方法

```javascript
import ImageUploadWithProgress from '@/components/ImageUploadWithProgress.vue';

export default {
  components: {
    ImageUploadWithProgress
  }
}
```

## 日志记录

系统会在关键节点记录详细的日志信息，包括：

1. 上传开始：记录文件名、类型和大小
2. 上传进度：定期记录已上传百分比（约每5%记录一次）
3. 上传完成：记录完整文件信息和最终URL
4. 上传失败：记录错误原因和已上传大小

日志格式示例：
```
[上传进度] 开始上传文件: example.jpg，上传ID: 550e8400-e29b-41d4-a716-446655440000，总大小: 5.2 MB
[上传进度] 文件: example.jpg，已上传: 1.0 MB (20.0%)，总大小: 5.2 MB
[上传进度] 文件上传完成: example.jpg，大小: 5.2 MB，上传ID: 550e8400-e29b-41d4-a716-446655440000
```

## 最佳实践

1. **错误处理**：前端应妥善处理WebSocket连接失败、断线重连和上传错误
2. **用户标识**：确保提供正确的用户标识以保证WebSocket消息能正确路由
3. **资源清理**：在组件卸载时断开WebSocket连接以避免内存泄漏
4. **服务器负载**：对于大量并发上传，应考虑调整WebSocket服务器配置

## 注意事项

1. 上传大文件时，服务器需要足够的内存来处理文件读取和计算
2. 进度消息发送频率已优化，约每5%进度发送一次，避免消息过多
3. 确保WebSocket连接正确建立后再开始上传，以免丢失初始进度消息
4. 在生产环境中，应使用与前端应用一致的CORS配置

## 故障排除

### 常见问题

1. **WebSocket连接失败**
   - 检查WebSocket端点配置是否正确
   - 确认CORS设置是否允许WebSocket连接
   - 验证前端SockJS和STOMP库版本兼容性

2. **进度消息未收到**
   - 确认WebSocket连接是否成功建立
   - 检查订阅的通道路径是否正确
   - 验证用户标识是否匹配

3. **上传失败**
   - 检查文件大小是否超出限制
   - 确认文件类型是否在允许列表中
   - 查看服务器日志获取详细错误信息

### 调试技巧

1. 启用WebSocket客户端调试
   ```javascript
   // 开启STOMP客户端调试
   const stompClient = Stomp.over(socket);
   stompClient.debug = function(str) {
     console.log(str);
   };
   ```

2. 监控WebSocket事件
   ```javascript
   socket.onopen = () => console.log('WebSocket连接已打开');
   socket.onclose = () => console.log('WebSocket连接已关闭');
   socket.onerror = (error) => console.error('WebSocket错误:', error);
   ```

3. 服务器日志级别调整
   在`application.properties`中设置：
   ```properties
   logging.level.org.springframework.web.socket=DEBUG
   logging.level.com.example.blog.service=DEBUG
   ``` 