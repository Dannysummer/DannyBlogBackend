package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.UpdateAvatarRequest;
import com.example.blog.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private AuthService authService;
    
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/update-avatar")
    public ResponseEntity<ApiResponse<?>> updateAvatar(@RequestBody UpdateAvatarRequest request) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("收到更新头像请求: username={}, avatarUrl={}", username, request.getAvatarUrl());
            
            // 调用服务更新头像
            ApiResponse<?> response = authService.updateAvatar(username, request.getAvatarUrl());
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("更新头像失败: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新头像失败：" + e.getMessage()));
        }
    }
} 