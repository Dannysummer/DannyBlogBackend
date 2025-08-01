package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.ArticleDto;
import com.example.blog.entity.Article;
import com.example.blog.entity.User;
import com.example.blog.enums.ArticleLicense;
import com.example.blog.enums.ArticleStatus;
import com.example.blog.repository.ArticleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.S3UploadService;
import com.example.blog.service.UploadProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ArticleUploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleUploadController.class);
    
    @Autowired
    private S3UploadService s3UploadService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Value("${upload.article.allowed-types:text/markdown,text/plain,application/octet-stream}")
    private String allowedTypesString;
    
    @Value("${upload.article.max-file-size:10485760}") // 默认10MB
    private long maxFileSize;
    
    /**
     * 获取上传策略和临时凭证
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<?>> getUploadPolicy() {
        try {
            // 生成特定的文章文件路径前缀
            String prefix = "articles/";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".md";
            String filePath = prefix + fileName;
            
            Map<String, Object> policy = s3UploadService.getUploadPolicy(filePath);
            logger.info("成功生成文章上传策略，路径: {}", filePath);
            return ResponseEntity.ok(ApiResponse.success(policy));
        } catch (Exception e) {
            logger.error("生成文章上传策略失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("生成文章上传策略失败：" + e.getMessage()));
        }
    }
    
    /**
     * 直接上传Markdown文件到服务器，再由服务器上传到S3
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cover", required = false) String cover) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            List<String> allowedTypes = Arrays.asList(allowedTypesString.split(","));
            if (contentType == null || !allowedTypes.contains(contentType)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("不支持的文件类型：" + contentType + "，仅支持：" + allowedTypesString));
            }
            
            // 验证文件大小
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("文件太大，最大允许 " + (maxFileSize/(1024*1024)) + " MB"));
            }
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在：" + username));
            }
            
            // 设置文章标题
            String articleTitle = title;
            if (articleTitle == null || articleTitle.trim().isEmpty()) {
                // 如果未提供标题，使用文件名作为标题（去除扩展名）
                String originalFilename = file.getOriginalFilename();
                if (originalFilename != null && originalFilename.contains(".")) {
                    articleTitle = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                } else {
                    articleTitle = "未命名文章";
                }
            }
            
            // 处理文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".md";  // 默认使用.md扩展名
                
            // 确保是MD文件格式
            if (!fileExtension.equalsIgnoreCase(".md")) {
                fileExtension = ".md";
            }
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;
            String filePath = "articles/" + fileName;
            
            // 创建新的MultipartFile用于上传
            MultipartFile fileForUpload = file;
            if (!originalFilename.endsWith(fileExtension)) {
                // 如果原始文件名没有正确的扩展名，则创建新的MultipartFile
                fileForUpload = new MultipartFileImpl(
                    fileName,
                    "text/markdown", // 强制设置为markdown类型
                    file.getBytes()
                );
            }
            
            // 上传文件
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("path", filePath);
            Map<String, Object> result = s3UploadService.uploadFile(fileForUpload, uploadParams);
            
            // 创建文章记录
            Article article = new Article();
            article.setTitle(articleTitle);
            article.setViews(0); // 初始化浏览量为0
            article.setCover(cover); // 设置封面图片URL，如果提供的话
            article.setDescription(description); // 设置描述，如果提供的话
            article.setCategory(category); // 设置分类，如果提供的话
            article.setAuthor(username); // 默认使用用户名作为作者
            article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0); // 默认使用CC协议
            article.setStatus(ArticleStatus.PUBLISHED); // 默认已发布状态
            article.setIsFeatured(false); // 默认非热门文章
            article.setFileUrl((String) result.get("url"));
            article.setFilePath((String) result.get("path"));
            article.setFileSize((Long) result.get("size"));
            article.setFileType("text/markdown"); // 强制设置为markdown类型
            article.setUser(user);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存文章记录
            Article savedArticle = articleRepository.save(article);
            logger.info("文章上传成功: id={}, title={}, user={}, url={}", 
                      savedArticle.getId(), savedArticle.getTitle(), username, savedArticle.getFileUrl());
            
            // 使用DTO返回数据，避免重复字段
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
        } catch (Exception e) {
            logger.error("文章上传失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("文章上传失败：" + e.getMessage()));
        }
    }
    
    /**
     * 带进度监控的文章文件上传
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload-with-progress")
    public ResponseEntity<ApiResponse<?>> uploadFileWithProgress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cover", required = false) String cover) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            List<String> allowedTypes = Arrays.asList(allowedTypesString.split(","));
            if (contentType == null || !allowedTypes.contains(contentType)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("不支持的文件类型：" + contentType + "，仅支持：" + allowedTypesString));
            }
            
            // 验证文件大小
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("文件太大，最大允许 " + (maxFileSize/(1024*1024)) + " MB"));
            }
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("开始带进度监控的文章上传: 文件名={}, 类型={}, 大小={}, 标题={}", 
                file.getOriginalFilename(), file.getContentType(), formatFileSize(file.getSize()), title);
            
            // 先保存文件内容到字节数组
            byte[] fileContent = file.getBytes();
            
            // 创建进度监听器
            UploadProgressListener progressListener = 
                new UploadProgressListener(
                    messagingTemplate, 
                    file.getOriginalFilename(), 
                    username
                );
            
            // 处理文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".md";  // 默认使用.md扩展名
                
            // 确保是MD文件格式
            if (!fileExtension.equalsIgnoreCase(".md")) {
                fileExtension = ".md";
            }
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;
            String filePath = "articles/" + fileName;
            
            // 创建新的MultipartFile用于上传
            MultipartFile fileForUpload = new MultipartFileImpl(
                fileName,
                "text/markdown", // 强制设置为markdown类型
                fileContent
            );
            
            // 上传文件（带进度监控）
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("path", filePath);
            Map<String, Object> result = s3UploadService.uploadFileWithProgress(fileForUpload, progressListener, uploadParams);
            
            // 设置文章标题
            String articleTitle = title;
            if (articleTitle == null || articleTitle.trim().isEmpty()) {
                // 如果未提供标题，使用文件名作为标题（去除扩展名）
                if (originalFilename != null && originalFilename.contains(".")) {
                    articleTitle = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                } else {
                    articleTitle = "未命名文章";
                }
            }
            
            // 获取用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在：" + username);
            }
            
            // 创建文章记录
            Article article = new Article();
            article.setTitle(articleTitle);
            article.setViews(0); // 初始化浏览量为0
            article.setCover(cover); // 设置封面图片URL，如果提供的话
            article.setDescription(description); // 设置描述，如果提供的话
            article.setCategory(category); // 设置分类，如果提供的话
            article.setAuthor(username); // 默认使用用户名作为作者
            article.setLicense(ArticleLicense.CC_BY_NC_SA_4_0); // 默认使用CC协议
            article.setStatus(ArticleStatus.PUBLISHED); // 默认已发布状态
            article.setIsFeatured(false); // 默认非热门文章
            article.setFileUrl((String) result.get("url"));
            article.setFilePath((String) result.get("path"));
            article.setFileSize((Long) result.get("size"));
            article.setFileType("text/markdown"); // 强制设置为markdown类型
            article.setUser(user);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存文章记录
            Article savedArticle = articleRepository.save(article);
            logger.info("文章上传成功（带进度监控）: id={}, title={}, user={}, url={}", 
                      savedArticle.getId(), savedArticle.getTitle(), username, savedArticle.getFileUrl());
            
            // 使用DTO返回数据，避免重复字段
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
        } catch (Exception e) {
            logger.error("带进度监控的文章上传失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("文章上传失败：" + e.getMessage()));
        }
    }
    
    /**
     * 删除文章记录及其关联的文件
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteArticle(@PathVariable("id") Long id) {
        try {
            // 获取文章记录
            Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            // 确保只能删除自己的文章
            if (user == null || !article.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权删除此文章"));
            }
            
            // 删除S3中的文件
            s3UploadService.deleteFile(article.getFilePath());
            
            // 删除数据库记录
            articleRepository.delete(article);
            
            logger.info("文章删除成功: id={}, title={}", id, article.getTitle());
            return ResponseEntity.ok(ApiResponse.success("文章删除成功"));
        } catch (Exception e) {
            logger.error("文章删除失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("文章删除失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取用户的所有文章
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> getUserArticles() {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在：" + username));
            }
            
            // 获取用户的文章列表
            List<Article> articles = articleRepository.findByUserOrderByCreatedAtDesc(user);
            
            // 将实体转换为DTO
            List<ArticleDto> articleDtos = articles.stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(articleDtos));
        } catch (Exception e) {
            logger.error("获取用户文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取单篇文章信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getArticle(@PathVariable("id") Long id) {
        try {
            // 获取文章记录
            Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
            
            // 检查访问权限
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null && 
                !authentication.getName().equals("anonymousUser");
            
            if (article.getStatus() == ArticleStatus.PUBLISHED) {
                // 已发布的文章允许公开访问
                // 增加访问量
                article.setViews(article.getViews() + 1);
                articleRepository.save(article);
                
                // 为了兼容前端期望的字段名，返回特定格式
                Map<String, Object> articleData = new HashMap<>();
                articleData.put("id", article.getId());
                articleData.put("title", article.getTitle());
                articleData.put("content", article.getContent() != null ? article.getContent() : "");
                articleData.put("status", article.getStatus().name());
                articleData.put("createTime", article.getCreatedAt());
                articleData.put("updateTime", article.getUpdatedAt());
                articleData.put("views", article.getViews());
                articleData.put("coverImage", article.getCover() != null ? article.getCover() : "");
                articleData.put("category", article.getCategory() != null ? article.getCategory() : "");
                articleData.put("tags", article.getTagArray() != null ? article.getTagArray() : new String[0]);
                articleData.put("author", article.getAuthor() != null ? article.getAuthor() : "");
                articleData.put("license", article.getLicense() != null ? article.getLicense().getCode() : "");
                articleData.put("allowComments", true);
                articleData.put("sticky", article.getIsFeatured() != null ? article.getIsFeatured() : false);
                articleData.put("comments", 0); // 暂时返回0，后续可以从评论服务获取
                
                return ResponseEntity.ok(ApiResponse.success(articleData));
            } else {
                // 未发布的文章需要认证，且只能查看自己的文章
                if (!isAuthenticated) {
                    return ResponseEntity.notFound().build();
                }
                
                String username = authentication.getName();
                User user = userRepository.findByUsername(username);
                
                if (user == null || !article.getUser().getId().equals(user.getId())) {
                    return ResponseEntity.notFound().build();
                }
                
                // 使用DTO返回数据，避免重复字段
                ArticleDto articleDto = ArticleDto.fromEntity(article);
                return ResponseEntity.ok(ApiResponse.success(articleDto));
            }
        } catch (Exception e) {
            logger.error("获取文章失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取文章内容（代理请求以避免CORS问题）
     */
    @GetMapping("/content/{id}")
    public ResponseEntity<?> getArticleContent(@PathVariable("id") Long id) {
        try {
            // 获取文章记录
            Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
            
            // 检查文章是否已发布（公开访问只能查看已发布的文章）
            if (article.getStatus() != ArticleStatus.PUBLISHED) {
                return ResponseEntity.notFound().build();
            }
            
            // 增加访问量
            article.setViews(article.getViews() + 1);
            articleRepository.save(article);
            
            // 读取文件内容
            String fileUrl = article.getFileUrl();
            logger.info("准备获取文章内容: fileUrl={}", fileUrl);
            
            String markdownContent;
            
            // 如果是草稿文章或文件URL无效，直接使用数据库中的内容
            if (fileUrl == null || fileUrl.startsWith("draft://")) {
                markdownContent = article.getContent();
                if (markdownContent == null || markdownContent.isEmpty()) {
                    logger.error("数据库中没有文章内容: id={}", id);
                    return ResponseEntity.badRequest().body(ApiResponse.error("文章内容不可用"));
                }
            } else {
                // 尝试从S3获取内容
                // 使用Spring的RestTemplate代理请求
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                
                // 配置RestTemplate使用UTF-8编码
                org.springframework.http.converter.StringHttpMessageConverter converter = 
                    new org.springframework.http.converter.StringHttpMessageConverter(
                        java.nio.charset.StandardCharsets.UTF_8);
                restTemplate.getMessageConverters().set(1, converter);
                
                try {
                    markdownContent = restTemplate.getForObject(fileUrl, String.class);
                    // 更新内容字段（可选）
                    if (markdownContent != null && (article.getContent() == null || article.getContent().isEmpty())) {
                        article.setContent(markdownContent);
                        articleRepository.save(article);
                        logger.info("已更新文章[{}]的内容字段", id);
                    }
                } catch (Exception e) {
                    logger.warn("从S3获取文章内容失败，尝试使用数据库内容: {}", e.getMessage());
                    // 如果从S3获取失败，使用数据库中的内容作为备用
                    markdownContent = article.getContent();
                    if (markdownContent == null || markdownContent.isEmpty()) {
                        logger.error("数据库中也没有文章内容: id={}", id);
                        return ResponseEntity.badRequest().body(ApiResponse.error("文章内容不可用"));
                    }
                }
            }
            
            // 不使用ApiResponse包装，直接返回Markdown内容
            return ResponseEntity
                .ok()
                .contentType(new org.springframework.http.MediaType("text", "markdown", java.nio.charset.StandardCharsets.UTF_8))
                .body(markdownContent);
        } catch (Exception e) {
            logger.error("获取文章内容失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章内容失败：" + e.getMessage()));
        }
    }
    
    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    /**
     * 自定义MultipartFile实现，用于包装文件内容
     */
    private static class MultipartFileImpl implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;
        
        public MultipartFileImpl(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getOriginalFilename() {
            return name;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content.length;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
    
    /**
     * 保存文章草稿，直接保存内容到数据库，不上传到多吉云
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/articleDraftSave")
    public ResponseEntity<ApiResponse<?>> saveArticleDraft(
            @RequestParam(value = "content", required = true) String content,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cover", required = false) String cover,
            @RequestParam(value = "license", required = false) String license,
            @RequestParam(value = "articleId", required = false) Long articleId) {
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
            if (articleId != null) {
                article = articleRepository.findById(articleId)
                    .orElse(null);
                
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
            if (title != null && !title.trim().isEmpty()) {
                article.setTitle(title);
            } else if (isNewDraft) {
                article.setTitle("未命名草稿");
            }
            
            // 更新文章内容和其他信息
            article.setContent(content);
            article.setDescription(description);
            article.setCategory(category);
            article.setCover(cover);
            
            // 如果提供了许可证，则设置
            if (license != null && !license.trim().isEmpty()) {
                try {
                    // 尝试通过code查找许可证
                    article.setLicenseCode(license);
                } catch (Exception e) {
                    logger.warn("无效的许可证代码: {}, 使用默认许可证", license);
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
                article.setIsFeatured(false); // 非热门文章
                article.setFileType("text/markdown");
                article.setUser(user);
                article.setCreatedAt(LocalDateTime.now());
                
                // 设置标记为草稿的文件路径和URL
                article.setFilePath("draft://" + UUID.randomUUID().toString());
                article.setFileUrl("draft://local");
                article.setFileSize(0L);
            }
            
            // 更新修改时间
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存文章记录
            Article savedArticle = articleRepository.save(article);
            logger.info("文章草稿保存成功: id={}, title={}, user={}", 
                      savedArticle.getId(), savedArticle.getTitle(), username);
            
            // 使用DTO返回数据
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
        } catch (Exception e) {
            logger.error("保存文章草稿失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("保存文章草稿失败：" + e.getMessage()));
        }
    }

    /**
     * 获取热门文章列表（已标记为热门的文章）
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<?>> getFeaturedArticles() {
        try {
            // 获取所有被标记为热门的文章
            List<Article> articles = articleRepository.findFeaturedArticles();
            
            // 将实体转换为DTO
            List<ArticleDto> articleDtos = articles.stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(articleDtos));
        } catch (Exception e) {
            logger.error("获取热门文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取热门文章列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取热门文章（根据访问量）
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<?>> getPopularArticles(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 创建分页请求，按照访问量降序排序
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, limit);
            
            // 获取访问量最高的文章
            List<Article> articles = articleRepository.findMostViewedArticles(pageable);
            
            // 将实体转换为DTO
            List<ArticleDto> articleDtos = articles.stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(articleDtos));
        } catch (Exception e) {
            logger.error("获取热门文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取热门文章列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 标记或取消标记文章为热门文章
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/featured")
    public ResponseEntity<ApiResponse<?>> toggleFeaturedArticle(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "true") boolean featured) {
        try {
            // 获取文章记录
            Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            // 确保只能修改自己的文章
            if (user == null || !article.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权修改此文章"));
            }
            
            // 设置热门标记
            article.setIsFeatured(featured);
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存更改
            Article savedArticle = articleRepository.save(article);
            
            logger.info("文章热门标记已{}：id={}, title={}", 
                featured ? "添加" : "移除", savedArticle.getId(), savedArticle.getTitle());
            
            // 使用DTO返回数据
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
        } catch (Exception e) {
            logger.error("修改文章热门标记失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("修改文章热门标记失败：" + e.getMessage()));
        }
    }
    
    /**
     * 更新文章状态（发布、设为草稿或移到回收站）
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateArticleStatus(
            @PathVariable("id") Long id,
            @RequestParam String status) {
        try {
            // 获取文章记录
            Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            // 确保只能修改自己的文章
            if (user == null || !article.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权修改此文章"));
            }
            
            // 设置文章状态
            try {
                ArticleStatus articleStatus = ArticleStatus.valueOf(status.toUpperCase());
                article.setStatus(articleStatus);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("无效的状态值。有效值：PUBLISHED, DRAFT, DELETED")
                );
            }
            
            article.setUpdatedAt(LocalDateTime.now());
            
            // 保存更改
            Article savedArticle = articleRepository.save(article);
            
            logger.info("文章状态已更新：id={}, title={}, status={}", 
                savedArticle.getId(), savedArticle.getTitle(), savedArticle.getStatus());
            
            // 使用DTO返回数据
            ArticleDto articleDto = ArticleDto.fromEntity(savedArticle);
            return ResponseEntity.ok(ApiResponse.success(articleDto));
        } catch (Exception e) {
            logger.error("更新文章状态失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新文章状态失败：" + e.getMessage()));
        }
    }

    /**
     * 获取特定状态的用户文章列表
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list/{status}")
    public ResponseEntity<ApiResponse<?>> getUserArticlesByStatus(
            @PathVariable("status") String status) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在：" + username));
            }
            
            // 验证状态值
            ArticleStatus articleStatus;
            try {
                articleStatus = ArticleStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("无效的状态值。有效值：PUBLISHED, DRAFT, DELETED")
                );
            }
            
            // 获取用户的文章列表
            List<Article> articles = articleRepository.findByUserAndStatus(user, articleStatus);
            
            // 将实体转换为DTO
            List<ArticleDto> articleDtos = articles.stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(articleDtos));
        } catch (Exception e) {
            logger.error("获取用户文章列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文章列表失败：" + e.getMessage()));
        }
    }
} 