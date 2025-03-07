package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.entity.BulletChat;
import com.example.blog.service.BulletChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bullet-chats")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class BulletChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(BulletChatController.class);
    
    @Autowired
    private BulletChatService bulletChatService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllBulletChats() {
        try {
            List<BulletChat> bulletChats = bulletChatService.getAllBulletChats();
            logger.info("成功获取所有弹幕，共{}条", bulletChats.size());
            return ResponseEntity.ok(ApiResponse.success(bulletChats));
        } catch (Exception e) {
            logger.error("获取弹幕失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取弹幕失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getBulletChatById(@PathVariable Integer id) {
        try {
            BulletChat bulletChat = bulletChatService.getBulletChatById(id);
            logger.info("成功获取弹幕，ID：{}", id);
            return ResponseEntity.ok(ApiResponse.success(bulletChat));
        } catch (Exception e) {
            logger.error("获取弹幕失败，ID：{}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取弹幕失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<?>> getBulletChatsByStatus(@PathVariable String status) {
        try {
            List<BulletChat> bulletChats = bulletChatService.getBulletChatsByStatus(status);
            logger.info("成功获取状态为{}的弹幕，共{}条", status, bulletChats.size());
            return ResponseEntity.ok(ApiResponse.success(bulletChats));
        } catch (Exception e) {
            logger.error("获取状态为{}的弹幕失败", status, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取弹幕失败：" + e.getMessage()));
        }
    }
    
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBulletChat(@RequestBody BulletChat bulletChat) {
        try {
            BulletChat savedBulletChat = bulletChatService.createBulletChat(bulletChat);
            logger.info("成功创建弹幕，ID：{}", savedBulletChat.getBulletId());
            return ResponseEntity.ok(ApiResponse.success(savedBulletChat));
        } catch (Exception e) {
            logger.error("创建弹幕失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("创建弹幕失败：" + e.getMessage()));
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateBulletChat(
            @PathVariable Integer id,
            @RequestBody BulletChat bulletChat) {
        try {
            BulletChat updatedBulletChat = bulletChatService.updateBulletChat(id, bulletChat);
            logger.info("成功更新弹幕，ID：{}", id);
            return ResponseEntity.ok(ApiResponse.success(updatedBulletChat));
        } catch (Exception e) {
            logger.error("更新弹幕失败，ID：{}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新弹幕失败：" + e.getMessage()));
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteBulletChat(@PathVariable Integer id) {
        try {
            bulletChatService.deleteBulletChat(id);
            logger.info("成功删除弹幕，ID：{}", id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            logger.error("删除弹幕失败，ID：{}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("删除弹幕失败：" + e.getMessage()));
        }
    }
} 