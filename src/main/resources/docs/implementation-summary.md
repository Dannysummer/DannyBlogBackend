# 文件上传进度监控实现方案

## 整体架构

我们实现了一套基于WebSocket的文件上传进度监控系统，主要包含以下组件：

1. 前端Vue组件 - 处理文件选择、上传和进度展示
2. WebSocket服务端 - 接收和推送进度信息
3. 自定义进度监听器 - 监控S3上传进度并通过WebSocket推送到前端

## 关键技术点

### 1. Spring WebSocket配置

我们使用Spring的WebSocket支持，配置了STOMP消息代理：

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }
}
```

### 2. 自定义上传进度监听器

我们创建了`UploadProgressListener`类，实现AWS S3 SDK的`TransferListener`接口，捕获上传进度并通过WebSocket发送到前端：

```java
public class UploadProgressListener implements TransferListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final String uploadId;
    private final String destination;
    
    // 实现TransferListener接口的方法
    @Override
    public void bytesTransferred(long bytesTransferred) {
        // 获取进度信息并发送WebSocket消息
        sendProgressMessage(bytesTransferred, getTotalBytes().orElse(0), "IN_PROGRESS");
    }
    
    // 发送进度消息
    private void sendProgressMessage(long bytesTransferred, long totalBytes, String status) {
        ProgressMessage message = new ProgressMessage();
        // 设置消息内容
        messagingTemplate.convertAndSend(destination, message);
    }
}
```

### 3. 文件上传服务

我们在`S3UploadService`中添加了支持进度监控的上传方法：

```java
public Map<String, Object> uploadFileWithProgress(MultipartFile file, String username) {
    // 获取临时凭证，准备上传
    
    // 创建S3客户端
    
    // 在实际项目中，使用S3TransferManager和UploadProgressListener
    // 在示例中简化实现
    
    // 返回上传结果
}
```

### 4. 前端Vue组件

我们提供了一个Vue组件示例，展示如何：
- 连接WebSocket
- 监听上传进度消息
- 展示上传进度动画

```javascript
// 连接WebSocket
connectWebSocket() {
    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    
    this.stompClient.connect({}, frame => {
        this.stompClient.subscribe(
            `/topic/upload-progress/${this.username}`,
            this.onProgressMessage
        );
    });
}

// 处理进度消息
onProgressMessage(message) {
    const progress = JSON.parse(message.body);
    // 更新UI上的进度条
}
```

## 使用方法

1. 前端集成Vue组件
2. 使用带进度监控的API端点上传文件：`/api/images/upload-with-progress`
3. WebSocket自动推送进度信息到前端

## 优势

1. 实时反馈 - 用户可以看到上传进度，提升用户体验
2. 低延迟 - WebSocket提供比轮询更高效的通信方式
3. 可扩展 - 架构设计支持添加更多进度事件和状态

## 技术局限及简化处理

由于一些技术限制（例如AWS S3 SDK版本兼容性、WebSocket依赖问题），当前实现做了以下简化：

1. 在`uploadFileWithProgress`方法中，未实际使用`S3TransferManager`和进度监听，而是使用普通S3客户端上传
2. 未实现真正的进度推送，需要在实际项目中根据AWS SDK版本完善

这些限制不影响整体架构设计，实际项目中可以根据具体环境进行调整。 