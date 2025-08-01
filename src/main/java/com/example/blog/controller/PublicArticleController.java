package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleNavigationDto;
import com.example.blog.dto.ArticleArchiveDto;
import com.example.blog.dto.PagedResponse;
import com.example.blog.service.ArticleService;
import com.example.blog.service.S3UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开文章API控制器
 * 处理不需要认证的文章相关接口
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PublicArticleController {
    
    private static final Logger logger = LoggerFactory.getLogger(PublicArticleController.class);
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private S3UploadService s3UploadService;
    

    /**
     * 兼容性接口：智能获取文章列表
     * - 未登录用户：返回公开的已发布文章
     * - 已登录用户 + includeAll=true：返回用户的所有文章
     * - 管理员用户 + includeAll=true：返回所有文章
     */
    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<?>> getArticlesCompatible(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean includeAll) {
        try {
            // 获取当前用户信息（可能为null）
            String currentUsername = null;
            boolean isAdmin = false;
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                currentUsername = authentication.getName();
                // 检查是否为管理员
                isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            }
            
            logger.info("兼容性接口调用: page={}, limit={}, includeAll={}, currentUsername={}, isAdmin={}", 
                       page, limit, includeAll, currentUsername, isAdmin);
            
            // 使用智能获取方法
            PagedResponse<ArticleDto> articles = articleService.getArticlesSmartly(
                page, limit, includeAll, currentUsername, isAdmin);
            
            return ResponseEntity.ok(ApiResponse.success(articles));
        } catch (Exception e) {
            logger.error("获取文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章列表失败：" + e.getMessage()));
        }
    }

    
    /**
     * 获取最新发布的文章列表
     */
    @GetMapping("/articles/recent")
    public ResponseEntity<ApiResponse<?>> getRecentArticles(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<ArticleDto> articles = articleService.getRecentPublishedArticles(limit);
            
            Map<String, Object> response = Map.of("articles", articles);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("获取最新文章失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取最新文章失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取相关文章列表
     */
    @GetMapping("/articles/{articleId}/related")
    public ResponseEntity<ApiResponse<?>> getRelatedArticles(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            List<ArticleDto> articles = articleService.getRelatedArticles(articleId, limit);
            
            Map<String, Object> response = Map.of("articles", articles);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("获取相关文章失败: articleId={}", articleId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取相关文章失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取文章导航信息（上一篇/下一篇）
     */
    @GetMapping("/articles/{articleId}/navigation")
    public ResponseEntity<ApiResponse<?>> getArticleNavigation(@PathVariable Long articleId) {
        try {
            Map<String, ArticleNavigationDto> navigation = articleService.getArticleNavigation(articleId);
            return ResponseEntity.ok(ApiResponse.success(navigation));
        } catch (Exception e) {
            logger.error("获取文章导航失败: articleId={}", articleId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章导航失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取文章归档数据
     */
    @GetMapping("/articles/archive")
    public ResponseEntity<ApiResponse<?>> getArticleArchive() {
        try {
            List<ArticleArchiveDto.YearArchive> archives = articleService.getArticleArchive();
            
            Map<String, Object> response = Map.of("archive", archives);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("获取文章归档失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章归档失败：" + e.getMessage()));
        }
    }
    
    /**
     * 分页获取已发布文章列表
     */
    @GetMapping("/articles/published")
    public ResponseEntity<ApiResponse<?>> getPublishedArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> result = articleService.getPublishedArticles(page, limit);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("获取已发布文章列表失败: page={}, limit={}", page, limit, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取已发布文章列表失败：" + e.getMessage()));
        }
    }
} 