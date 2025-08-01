package com.example.blog.controller;

import com.example.blog.entity.Article;
import com.example.blog.service.ArticleService;
import com.example.blog.repository.ArticleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.entity.User;
import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.PagedResponse;
import com.example.blog.enums.ArticleLicense;
import com.example.blog.enums.ArticleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/article")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ArticleController {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleController.class);
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PutMapping("/{id}/feature")
    public ResponseEntity<Map<String, Object>> toggleFeature(@PathVariable Long id) {
        Article article = articleService.toggleFeature(id);
        
        // 只返回必要的字段
        Map<String, Object> response = new HashMap<>();
        response.put("id", article.getId());
        response.put("isFeatured", article.getIsFeatured());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 保存文章草稿
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<?>> saveDraft(@RequestBody Map<String, Object> articleData) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在：" + username));
            }
            
            Article article;
            boolean isNewDraft = false;
            
            // 如果提供了文章ID，查找现有文章进行更新
            if (articleData.containsKey("id") && articleData.get("id") != null) {
                Long articleId = Long.valueOf(articleData.get("id").toString());
                article = articleRepository.findById(articleId).orElse(null);
                
                // 如果找不到文章或文章不属于当前用户，创建新文章
                if (article == null || !article.getUser().getId().equals(user.getId())) {
                    article = new Article();
                    isNewDraft = true;
                }
            } else {
                // 创建新的草稿文章
                article = new Article();
                isNewDraft = true;
            }
            
            // 设置文章标题
            if (articleData.containsKey("title")) {
                String title = (String) articleData.get("title");
                article.setTitle(title != null && !title.trim().isEmpty() ? title.trim() : "未命名草稿");
            } else if (isNewDraft) {
                article.setTitle("未命名草稿");
            }
            
            // 更新文章内容和其他信息
            if (articleData.containsKey("content")) {
                article.setContent((String) articleData.get("content"));
            }
            if (articleData.containsKey("description")) {
                article.setDescription((String) articleData.get("description"));
            }
            if (articleData.containsKey("category")) {
                article.setCategory((String) articleData.get("category"));
            }
            if (articleData.containsKey("cover")) {
                article.setCover((String) articleData.get("cover"));
            }
            if (articleData.containsKey("tags")) {
                if (articleData.get("tags") instanceof String) {
                    article.setTags((String) articleData.get("tags"));
                } else if (articleData.get("tags") instanceof String[]) {
                    String[] tagArray = (String[]) articleData.get("tags");
                    article.setTagArray(tagArray);
                }
            }
            if (articleData.containsKey("license")) {
                String licenseCode = (String) articleData.get("license");
                try {
                    article.setLicenseCode(licenseCode);
                } catch (Exception e) {
                    logger.warn("无效的许可证代码: {}, 使用默认许可证", licenseCode);
                    article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0);
                }
            }
            
            // 如果是新草稿，需要设置其他字段
            if (isNewDraft) {
                article.setViews(0);
                article.setAuthor(username);
                if (article.getLicense() == null) {
                    article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0);
                }
                article.setStatus(ArticleStatus.DRAFT); // 设置为草稿状态
                article.setIsFeatured(false);
                article.setFileType("text/markdown");
                article.setUser(user);
                article.setCreatedAt(LocalDateTime.now());
                article.setFileUrl("draft://" + System.currentTimeMillis());
            }
            
            // 更新时间
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存文章
            Article savedArticle = articleRepository.save(article);
            logger.info("草稿保存成功: id={}, title={}, user={}", 
                      savedArticle.getId(), savedArticle.getTitle(), username);
            
            // 返回保存后的文章数据
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
            
        } catch (Exception e) {
            logger.error("保存草稿失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("保存草稿失败：" + e.getMessage()));
        }
    }
    
    /**
     * 发布文章 - 强制更新文章的所有数据并设置为已发布状态
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<?>> publishArticle(
            @PathVariable Long id,
            @RequestBody Map<String, Object> articleData) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在：" + username));
            }
            
            // 查找文章
            Article article = articleRepository.findById(id).orElse(null);
            if (article == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限 - 只能发布自己的文章
            if (!article.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权发布此文章"));
            }
            
            // ===== 强制验证必填字段 =====
            String title = (String) articleData.get("title");
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("发布文章时标题不能为空"));
            }
            
            String content = (String) articleData.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("发布文章时内容不能为空"));
            }
            
            // ===== 强制更新所有字段 =====
            // 必填字段
            article.setTitle(title.trim());
            article.setContent(content);
            
            // 可选字段 - 即使前端没传也要更新（设为null或默认值）
            article.setDescription((String) articleData.get("description"));
            article.setCategory((String) articleData.get("category"));
            article.setCover((String) articleData.get("cover"));
            
            // 处理标签
            Object tagsObj = articleData.get("tags");
            if (tagsObj instanceof String) {
                article.setTags((String) tagsObj);
            } else if (tagsObj instanceof String[]) {
                article.setTagArray((String[]) tagsObj);
            } else if (tagsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> tagsList = (java.util.List<String>) tagsObj;
                article.setTagArray(tagsList.toArray(new String[0]));
            } else {
                // 如果没有传tags或格式不对，清空标签
                article.setTags(null);
            }
            
            // 处理许可证
            String licenseCode = (String) articleData.get("license");
            if (licenseCode != null && !licenseCode.trim().isEmpty()) {
                try {
                    article.setLicenseCode(licenseCode);
                } catch (Exception e) {
                    logger.warn("无效的许可证代码: {}, 使用默认许可证", licenseCode);
                    article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0);
                }
            } else {
                // 没有传许可证，使用默认
                article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0);
            }
            
            // 处理是否为特色文章
            Object isFeaturedObj = articleData.get("isFeatured");
            if (isFeaturedObj instanceof Boolean) {
                article.setIsFeatured((Boolean) isFeaturedObj);
            } else {
                // 默认不是特色文章
                article.setIsFeatured(false);
            }
            
            // ===== 强制设置发布相关字段 =====
            article.setStatus(ArticleStatus.PUBLISHED);
            article.setAuthor(username); // 确保作者字段正确
            
            // 如果是从草稿发布，更新文件URL
            if (article.getFileUrl() == null || article.getFileUrl().startsWith("draft://")) {
                article.setFileUrl("published://" + article.getId() + "_" + System.currentTimeMillis());
            }
            
            // 更新时间
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存文章
            Article savedArticle = articleRepository.save(article);
            logger.info("文章发布成功: id={}, title={}, user={}, 所有字段已更新", 
                      savedArticle.getId(), savedArticle.getTitle(), username);
            
            // 返回发布后的文章数据
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
            
        } catch (Exception e) {
            logger.error("发布文章失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("发布文章失败：" + e.getMessage()));
        }
    }
    
    /**
     * 更新文章
     * @param fullUpdate 是否完整更新所有字段（默认false，只更新提供的字段）
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateArticle(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean fullUpdate,
            @RequestBody Map<String, Object> articleData) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在：" + username));
            }
            
            // 查找文章
            Article article = articleRepository.findById(id).orElse(null);
            if (article == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查权限 - 只能编辑自己的文章
            if (!article.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权编辑此文章"));
            }
            
            // 根据fullUpdate参数决定更新模式
            if (fullUpdate) {
                // ===== 完整更新模式 - 更新所有字段 =====
                logger.info("使用完整更新模式更新文章: id={}", id);
                
                // 标题（必填）
                String title = (String) articleData.get("title");
                if (title == null || title.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("完整更新时标题不能为空"));
                }
                article.setTitle(title.trim());
                
                // 内容（必填）
                String content = (String) articleData.get("content");
                if (content == null || content.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("完整更新时内容不能为空"));
                }
                article.setContent(content);
                
                // 可选字段 - 强制更新所有字段
                article.setDescription((String) articleData.get("description"));
                article.setCategory((String) articleData.get("category"));
                article.setCover((String) articleData.get("cover"));
                
                // 处理标签
                Object tagsObj = articleData.get("tags");
                if (tagsObj instanceof String) {
                    article.setTags((String) tagsObj);
                } else if (tagsObj instanceof String[]) {
                    article.setTagArray((String[]) tagsObj);
                } else if (tagsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> tagsList = (java.util.List<String>) tagsObj;
                    article.setTagArray(tagsList.toArray(new String[0]));
                } else {
                    article.setTags(null);
                }
                
                // 处理许可证
                String licenseCode = (String) articleData.get("license");
                if (licenseCode != null && !licenseCode.trim().isEmpty()) {
                    try {
                        article.setLicenseCode(licenseCode);
                    } catch (Exception e) {
                        logger.warn("无效的许可证代码: {}, 使用默认许可证", licenseCode);
                        article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0);
                    }
                } else {
                    article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0);
                }
                
                // 处理状态
                String statusStr = (String) articleData.get("status");
                if (statusStr != null && !statusStr.trim().isEmpty()) {
                    try {
                        ArticleStatus status = ArticleStatus.valueOf(statusStr.toUpperCase());
                        article.setStatus(status);
                    } catch (Exception e) {
                        logger.warn("无效的文章状态: {}, 保持原有状态", statusStr);
                    }
                }
                
                // 处理是否为特色文章
                Object isFeaturedObj = articleData.get("isFeatured");
                if (isFeaturedObj instanceof Boolean) {
                    article.setIsFeatured((Boolean) isFeaturedObj);
                } else {
                    article.setIsFeatured(false);
                }
                
            } else {
                // ===== 部分更新模式 - 只更新提供的字段 =====
                logger.info("使用部分更新模式更新文章: id={}", id);
                
                if (articleData.containsKey("title")) {
                    String title = (String) articleData.get("title");
                    if (title == null || title.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("文章标题不能为空"));
                    }
                    article.setTitle(title.trim());
                }
                if (articleData.containsKey("content")) {
                    String content = (String) articleData.get("content");
                    if (content != null) {
                        article.setContent(content);
                    }
                }
                if (articleData.containsKey("description")) {
                    article.setDescription((String) articleData.get("description"));
                }
                if (articleData.containsKey("category")) {
                    article.setCategory((String) articleData.get("category"));
                }
                if (articleData.containsKey("cover")) {
                    article.setCover((String) articleData.get("cover"));
                }
                if (articleData.containsKey("tags")) {
                    Object tagsObj = articleData.get("tags");
                    if (tagsObj instanceof String) {
                        article.setTags((String) tagsObj);
                    } else if (tagsObj instanceof String[]) {
                        article.setTagArray((String[]) tagsObj);
                    } else if (tagsObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> tagsList = (java.util.List<String>) tagsObj;
                        article.setTagArray(tagsList.toArray(new String[0]));
                    }
                }
                if (articleData.containsKey("license")) {
                    String licenseCode = (String) articleData.get("license");
                    try {
                        article.setLicenseCode(licenseCode);
                    } catch (Exception e) {
                        logger.warn("无效的许可证代码: {}, 保持原有许可证", licenseCode);
                    }
                }
                if (articleData.containsKey("status")) {
                    String statusStr = (String) articleData.get("status");
                    try {
                        ArticleStatus status = ArticleStatus.valueOf(statusStr.toUpperCase());
                        article.setStatus(status);
                    } catch (Exception e) {
                        logger.warn("无效的文章状态: {}, 保持原有状态", statusStr);
                    }
                }
                if (articleData.containsKey("isFeatured")) {
                    article.setIsFeatured((Boolean) articleData.get("isFeatured"));
                }
            }
            
            // 更新修改时间
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存文章
            Article savedArticle = articleRepository.save(article);
            if (fullUpdate) {
                logger.info("文章完整更新成功: id={}, title={}, user={}, 所有字段已更新", 
                          savedArticle.getId(), savedArticle.getTitle(), username);
            } else {
                logger.info("文章部分更新成功: id={}, title={}, user={}", 
                          savedArticle.getId(), savedArticle.getTitle(), username);
            }
            
            // 返回更新后的文章数据
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
            
        } catch (Exception e) {
            logger.error("更新文章失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新文章失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取文章详情（支持公开访问已发布文章，需要认证访问草稿）
     */
    @GetMapping("/details/{id}")
    public ResponseEntity<ApiResponse<?>> getArticleById(@PathVariable Long id) {
        try {
            // 获取当前用户（可能为null）
            String currentUsername = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                currentUsername = authentication.getName();
            }
            
            ArticleDto article = articleService.getArticleById(id, currentUsername);
            if (article == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(ApiResponse.success(article));
        } catch (Exception e) {
            logger.error("获取文章详情失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章详情失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取文章内容（支持公开访问已发布文章，需要认证访问草稿）
     */
    @GetMapping("/content/{id}")
    public ResponseEntity<ApiResponse<?>> getArticleContent(@PathVariable Long id) {
        try {
            // 获取当前用户（可能为null）
            String currentUsername = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                currentUsername = authentication.getName();
            }
            
            String content = articleService.getArticleContent(id, currentUsername);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", content);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("获取文章内容失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章内容失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户的文章列表
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            PagedResponse<ArticleDto> articles = articleService.getArticlesByUser(username, page, limit, status);
            return ResponseEntity.ok(ApiResponse.success(articles));
        } catch (Exception e) {
            logger.error("获取用户文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取用户文章列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户的文章列表（简单格式，兼容前端期望）
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ArticleDto>>> getUserArticleList(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        
        if (authentication == null) {
            logger.warn("未认证用户尝试访问文章列表");
            return ResponseEntity.status(401).body(ApiResponse.error("需要认证"));
        }
        
        try {
            String username = authentication.getName();
            logger.info("获取用户文章列表: username={}, status={}", username, status);
            
            // 获取用户的所有文章（不分页）
            List<Article> articles;
            if (status == null || status.isEmpty() || status.equalsIgnoreCase("ALL")) {
                articles = articleRepository.findByAuthorOrderByCreatedAtDesc(username);
            } else {
                try {
                    ArticleStatus articleStatus = ArticleStatus.valueOf(status.toUpperCase());
                    articles = articleRepository.findByAuthorAndStatusOrderByCreatedAtDesc(username, articleStatus);
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的文章状态: {}, 返回所有文章", status);
                    articles = articleRepository.findByAuthorOrderByCreatedAtDesc(username);
                }
            }
            
            List<ArticleDto> articleDtos = articles.stream()
                    .map(ArticleDto::fromEntity)
                    .collect(Collectors.toList());
            
            logger.info("成功获取用户文章列表: username={}, 文章数量={}", username, articleDtos.size());
            return ResponseEntity.ok(ApiResponse.success(articleDtos));
            
        } catch (Exception e) {
            logger.error("获取用户文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 按状态获取当前用户的文章列表（兼容前端期望）
     */
    @GetMapping("/list/{status}")
    public ResponseEntity<ApiResponse<List<ArticleDto>>> getUserArticleListByStatus(
            @PathVariable String status,
            Authentication authentication) {
        
        if (authentication == null) {
            logger.warn("未认证用户尝试访问文章列表");
            return ResponseEntity.status(401).body(ApiResponse.error("需要认证"));
        }
        
        try {
            String username = authentication.getName();
            logger.info("按状态获取用户文章列表: username={}, status={}", username, status);
            
            List<Article> articles;
            try {
                ArticleStatus articleStatus = ArticleStatus.valueOf(status.toUpperCase());
                articles = articleRepository.findByAuthorAndStatusOrderByCreatedAtDesc(username, articleStatus);
            } catch (IllegalArgumentException e) {
                logger.warn("无效的文章状态: {}, 返回空列表", status);
                articles = new ArrayList<>();
            }
            
            List<ArticleDto> articleDtos = articles.stream()
                    .map(ArticleDto::fromEntity)
                    .collect(Collectors.toList());
            
            logger.info("成功按状态获取用户文章列表: username={}, status={}, 文章数量={}", username, status, articleDtos.size());
            return ResponseEntity.ok(ApiResponse.success(articleDtos));
            
        } catch (Exception e) {
            logger.error("按状态获取用户文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章列表失败：" + e.getMessage()));
        }
    }
}