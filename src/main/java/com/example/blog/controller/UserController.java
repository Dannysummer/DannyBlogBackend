package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.UpdateAvatarRequest;
import com.example.blog.dto.UserDto;
import com.example.blog.service.AuthService;
import com.example.blog.service.UserService;
import com.example.blog.dto.UpdateProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取当前登录用户的信息
     * @return 用户信息
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUserProfile() {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("获取用户个人信息: username={}", username);
            
            // 调用服务获取用户信息
            ApiResponse<UserDto> response = userService.getUserByUsername(username);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("获取用户信息失败: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取用户信息失败：" + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取用户信息（仅允许查询自己的信息）
     * @param id 用户ID
     * @return 用户信息
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("根据ID获取用户信息: username={}, id={}", username, id);
            
            // 验证当前用户是否有权限查询此ID的用户信息
            ApiResponse<Map<String, Object>> validationResponse = userService.validateUserAccess(username, id);
            if (!validationResponse.isSuccess()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(validationResponse.getMessage()));
            }
            
            // 调用服务获取用户信息
            ApiResponse<UserDto> response = userService.getUserDetail(id);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("获取用户信息失败: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取用户信息失败：" + e.getMessage()));
        }
    }
    
    /**
     * 更新用户个人信息（仅允许更新自己的信息）
     * @param id 用户ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUserProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("更新用户个人信息: username={}, id={}", username, id);
            
            // 验证当前用户是否有权限更新此ID的用户信息
            ApiResponse<Map<String, Object>> validationResponse = userService.validateUserAccess(username, id);
            if (!validationResponse.isSuccess()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(validationResponse.getMessage()));
            }
            
            // 转换请求为UserDto
            UserDto userDto = new UserDto();
            userDto.setEmail(request.getEmail());
            userDto.setPhoneNumber(request.getPhoneNumber());
            userDto.setRealName(request.getRealName());
            
            // 调用服务更新用户信息
            ApiResponse<UserDto> response = userService.updateUser(id, userDto, username);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("更新用户信息失败: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新用户信息失败：" + e.getMessage()));
        }
    }
    
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/rank")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRank() {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("获取用户排名: username={}", username);
            
            // 调用服务获取用户信息
            ApiResponse<UserDto> userResponse = userService.getUserByUsername(username);
            
            if (!userResponse.isSuccess()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(userResponse.getMessage()));
            }
            
            UserDto user = userResponse.getData();
            
            Map<String, Object> result = new HashMap<>();
            result.put("rank", user.getRank());
            result.put("username", user.getUsername());
            result.put("createTime", user.getCreateTime());
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("获取用户排名失败: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取用户排名失败：" + e.getMessage()));
        }
    }
    
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