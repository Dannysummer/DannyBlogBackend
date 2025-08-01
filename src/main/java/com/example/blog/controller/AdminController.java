package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.UserDto;
import com.example.blog.dto.PagedResponse;
import com.example.blog.dto.ArticleDto;
import com.example.blog.enums.UserStatus;
import com.example.blog.service.UserService;
import com.example.blog.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ArticleService articleService;
    
    /**
     * 获取所有文章列表（管理员）
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<PagedResponse<ArticleDto>>> getAllArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String keyword) {
        
        logger.info("管理员获取文章列表请求: page={}, limit={}, status={}, author={}, keyword={}", 
                   page, limit, status, author, keyword);
        
        try {
            PagedResponse<ArticleDto> articles = articleService.getAllArticlesForAdmin(
                page, limit, status, author, keyword);
            
            return ResponseEntity.ok(ApiResponse.success(articles));
        } catch (Exception e) {
            logger.error("管理员获取文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取用户列表
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UserStatus status) {
        
        logger.info("获取用户列表请求: page={}, size={}, username={}, status={}", page, size, username, status);
        
        ApiResponse<Map<String, Object>> response = userService.getUserList(page, size, username, status);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户详情
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserDetail(@PathVariable Long id) {
        logger.info("获取用户详情请求: id={}", id);
        
        ApiResponse<UserDto> response = userService.getUserDetail(id);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建用户
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody UserDto userDto) {
        // 获取当前管理员用户名作为操作者
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        
        logger.info("创建用户请求: username={}, email={}, operator={}", 
                userDto.getUsername(), userDto.getEmail(), operator);
        
        ApiResponse<UserDto> response = userService.createUser(userDto, operator);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id, 
            @RequestBody UserDto userDto) {
        
        // 获取当前管理员用户名作为操作者
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        
        logger.info("更新用户请求: id={}, operator={}", id, operator);
        
        ApiResponse<UserDto> response = userService.updateUser(id, userDto, operator);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除用户
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<?>> deleteUser(@PathVariable Long id) {
        // 获取当前管理员用户名作为操作者
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        
        logger.info("删除用户请求: id={}, operator={}", id, operator);
        
        ApiResponse<?> response = userService.deleteUser(id, operator);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 修改用户状态
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<?>> changeUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        
        // 获取当前管理员用户名作为操作者
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        
        logger.info("修改用户状态请求: id={}, status={}, operator={}", id, status, operator);
        
        ApiResponse<?> response = userService.changeUserStatus(id, status, operator);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 重置用户密码
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetUserPassword(@PathVariable Long id) {
        // 获取当前管理员用户名作为操作者
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        
        logger.info("重置用户密码请求: id={}, operator={}", id, operator);
        
        ApiResponse<String> response = userService.resetUserPassword(id, operator);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 