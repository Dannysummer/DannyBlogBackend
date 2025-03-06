package com.example.blog.controller;

import com.example.blog.dto.*;
import com.example.blog.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    // Form Data 登录
    @PostMapping(value = "/login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> loginForm(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletResponse response) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return handleLogin(request, response);
    }
    
    // JSON 登录
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> loginJson(@RequestBody LoginRequest request, HttpServletResponse response) {
        return handleLogin(request, response);
    }
    
    // Form Data 注册
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> registerForm(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        
        try {
            ApiResponse<?> response = authService.register(request);
            if (!response.isSuccess()) {
                if (response.getMessage().contains("用户名已经存在")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                } else if (response.getMessage().contains("邮箱已经注册")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                } else {
                    return ResponseEntity.badRequest().body(response);
                }
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("服务器内部错误：" + e.getMessage()));
        }
    }
    
    // JSON 注册
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> registerJson(@RequestBody RegisterRequest request) {
        try {
            ApiResponse<?> response = authService.register(request);
            if (!response.isSuccess()) {
                if (response.getMessage().contains("用户名已经存在")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                } else if (response.getMessage().contains("邮箱已经注册")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                } else {
                    return ResponseEntity.badRequest().body(response);
                }
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("服务器内部错误：" + e.getMessage()));
        }
    }
    
    // Form Data 发送邮箱验证码
    @PostMapping(value = "/send-email-code", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> sendEmailCodeForm(
            @RequestParam String email,
            @RequestParam(required = false) String type) {
        try {
            ApiResponse<?> response = authService.sendEmailCode(email, type);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getMessage().contains("已被注册")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("发送验证码失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("发送验证码失败：" + e.getMessage()));
        }
    }
    
    // JSON 发送邮箱验证码
    @PostMapping(value = "/send-email-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> sendEmailCode(@RequestBody EmailCodeRequest request) {
        try {
            ApiResponse<?> response = authService.sendEmailCode(request.getEmail(), request.getType());
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if (response.getMessage().contains("已被注册")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("发送验证码失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("发送验证码失败：" + e.getMessage()));
        }
    }
    
    // 私有方法处理登录逻辑
    private ResponseEntity<?> handleLogin(LoginRequest request, HttpServletResponse response) {
        ApiResponse<?> apiResponse = authService.login(request);
        
        if (apiResponse.isSuccess() && apiResponse.getToken() != null) {
            ResponseCookie cookie = ResponseCookie.from("token", apiResponse.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .maxAge(5400)
                    .path("/")
                    .build();
            
            response.addHeader("Set-Cookie", cookie.toString());
            apiResponse.setToken(null);
        }
        
        return ResponseEntity.ok(apiResponse);
    }
    
    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            ApiResponse<?> response = authService.resetPassword(request.getEmail(), request.getCode());
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("重置密码失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("重置密码失败：" + e.getMessage()));
        }
    }

    @PostMapping(value = "/verify-email-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> verifyEmailCode(@RequestBody EmailCodeRequest request) {
        try {
            ApiResponse<?> response = authService.verifyEmailCode(request.getEmail(), request.getCode());
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("验证邮箱验证码失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("验证失败：" + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletResponse response) {
        try {
            // 创建一个立即过期的 cookie 来清除 token
            ResponseCookie cookie = ResponseCookie.from("token", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0) // 立即过期
                    .build();
            
            response.addHeader("Set-Cookie", cookie.toString());
            
            return ResponseEntity.ok(ApiResponse.success("退出登录成功"));
        } catch (Exception e) {
            logger.error("退出登录失败: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("退出登录失败：" + e.getMessage()));
        }
    }
} 