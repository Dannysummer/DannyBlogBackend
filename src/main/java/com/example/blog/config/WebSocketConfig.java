package com.example.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 启用基于STOMP协议的WebSocket消息代理
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 设置消息代理前缀，客户端订阅的路径将以/topic开头
        registry.enableSimpleBroker("/topic");
        // 设置应用程序目标前缀，客户端发送消息的路径将以/app开头
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册一个STOMP端点，客户端可以通过这个端点连接到WebSocket服务器
        // 启用SockJS支持，以便在不支持WebSocket的浏览器中使用SockJS
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173") // 允许的源，与CORS配置保持一致
                .withSockJS();
    }
} 