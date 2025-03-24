package com.example.blog.controller;

import com.example.blog.entity.Image;
import com.example.blog.entity.User;
import com.example.blog.repository.ImageRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.dto.ApiResponse;
import com.example.blog.service.S3UploadService;
import com.example.blog.service.ImageTokenService;
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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ImageUploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageUploadController.class);
    
    @Autowired
    private S3UploadService s3UploadService;
    
    @Autowired
    private ImageTokenService imageTokenService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Value("${upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypesString;
    
    @Value("${upload.max-file-size:10485760}") // 默认10MB
    private long maxFileSize;
    
    /**
     * 获取上传策略和临时凭证
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<?>> getUploadPolicy() {
        try {
            Map<String, Object> policy = s3UploadService.getUploadPolicy(null);
            logger.info("成功生成上传策略");
            return ResponseEntity.ok(ApiResponse.success(policy));
        } catch (Exception e) {
            logger.error("生成上传策略失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("生成上传策略失败：" + e.getMessage()));
        }
    }
    
    /**
     * 直接上传文件到服务器，再由服务器上传到S3
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadFile(@RequestParam("file") MultipartFile file) {
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
            
            // 上传文件
            Map<String, Object> result = s3UploadService.uploadFile(file);
            logger.info("文件上传成功: {}", result.get("url"));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("文件上传失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取上传令牌（兼容多吉云原有API）
     * 这里同时支持S3和原始多吉云上传方式
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/token")
    public ResponseEntity<ApiResponse<?>> getUploadToken() {
        try {
            Map<String, Object> tokenData = imageTokenService.generateUploadToken();
            logger.info("成功生成上传令牌");
            return ResponseEntity.ok(ApiResponse.success(tokenData));
        } catch (Exception e) {
            logger.error("生成上传令牌失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("生成上传令牌失败：" + e.getMessage()));
        }
    }
    
    /**
     * 带进度监控的文件上传
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload-with-progress")
    public ResponseEntity<ApiResponse<?>> uploadFileWithProgress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type,
            @RequestParam(value = "customName", required = false) String customName) {
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
            
            logger.info("开始带进度监控的文件上传: 文件名={}, 类型={}, 大小={}, 自定义名称={}", 
                file.getOriginalFilename(), file.getContentType(), formatFileSize(file.getSize()), customName);
            
            // 先保存文件内容到字节数组
            byte[] fileContent = file.getBytes();
            
            // 创建进度监听器
            UploadProgressListener progressListener = 
                new UploadProgressListener(
                    messagingTemplate, 
                    file.getOriginalFilename(), 
                    username
                );
            
            // 处理自定义文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
                
            // 如果提供了自定义名称，使用自定义名称；否则使用UUID
            String fileName;
            if (customName != null && !customName.trim().isEmpty()) {
                // 仅过滤不安全的文件名字符，允许中文
                customName = customName.replaceAll("[\\\\/:*?\"<>|]", "_");
                // 确保添加原始文件扩展名
                if (!customName.endsWith(fileExtension) && !fileExtension.isEmpty()) {
                    fileName = customName + fileExtension;
                } else {
                    fileName = customName;
                }
                logger.info("使用自定义文件名: {}", fileName);
            } else {
                // 使用默认的UUID生成方式
                fileName = java.util.UUID.randomUUID().toString().replace("-", "") + fileExtension;
                logger.info("使用生成的UUID文件名: {}", fileName);
            }
            
            // 创建新的MultipartFile用于上传，但使用自定义或生成的文件名
            MultipartFile fileForUpload = new MultipartFileImpl(
                fileName, // 使用处理后的文件名
                file.getContentType(),
                fileContent
            );
            
            // 上传文件（带进度监控）
            Map<String, Object> result = s3UploadService.uploadFileWithProgress(fileForUpload, progressListener);
            
            // 生成缩略图并上传
            String thumbnailUrl = generateAndUploadThumbnail(fileContent, fileName, file.getContentType(), type);
            
            // 保存图片记录到数据库
            Image image = new Image();
            image.setName(fileName); // 使用自定义或生成的文件名
            image.setUrl((String) result.get("url"));
            image.setThumbnailUrl(thumbnailUrl); // 设置缩略图URL
            image.setType(type);
            image.setSize((Long) result.get("size"));
            image.setPath((String) result.get("path"));
            image.setContentType(file.getContentType());
            
            // 获取用户并设置关联
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在：" + username);
            }
            image.setUser(user);
            
            // 保存图片记录
            Image savedImage = imageRepository.save(image);
            logger.info("图片记录已保存到数据库：id={}, name={}, type={}, user={}", 
                savedImage.getId(), savedImage.getName(), savedImage.getType(), username);
            
            // 返回保存的图片记录
            return ResponseEntity.ok(ApiResponse.success(savedImage));
        } catch (Exception e) {
            logger.error("带进度监控的文件上传失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("文件上传失败：" + e.getMessage()));
        }
    }
    
    /**
     * 生成缩略图并上传
     */
    private String generateAndUploadThumbnail(byte[] fileContent, String originalFilename, String contentType, String type) throws Exception {
        // 从字节数组读取图片
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileContent));
        if (originalImage == null) {
            logger.warn("无法读取图片或不支持的图片格式：{}", originalFilename);
            return null;
        }
        
        // 计算缩略图尺寸，保持比例
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double ratio = (double) originalWidth / originalHeight;
        
        // 设置缩略图尺寸
        int thumbnailWidth = 200;
        int thumbnailHeight = 200;
        
        int thumbWidth, thumbHeight;
        if (ratio > 1) { // 宽图
            thumbWidth = thumbnailWidth;
            thumbHeight = (int) (thumbnailWidth / ratio);
        } else { // 高图
            thumbHeight = thumbnailHeight;
            thumbWidth = (int) (thumbnailHeight * ratio);
        }
        
        // 生成缩略图
        BufferedImage thumbnailImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnailImage.createGraphics();
        graphics.drawImage(originalImage.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        
        // 将缩略图转为MultipartFile格式
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String imageFormat = contentType != null && contentType.contains("png") ? "png" : "jpeg";
        ImageIO.write(thumbnailImage, imageFormat, baos);
        
        // 构建缩略图文件名
        String thumbFilename = originalFilename != null 
                ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "_thumb." + imageFormat
                : "thumbnail_" + System.currentTimeMillis() + "." + imageFormat;
        
        // 准备上传缩略图
        MultipartFile thumbnailFile = new MultipartFileImpl(
                thumbFilename,
                contentType,
                baos.toByteArray()
        );
        
        // 上传缩略图
        Map<String, Object> result = s3UploadService.uploadFile(thumbnailFile);
        logger.info("缩略图上传成功：{}", result.get("url"));
        
        // 直接使用返回的URL
        return (String) result.get("url");
    }
    
    /**
     * 自定义MultipartFile实现，用于缩略图上传
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
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
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
     * 重命名已上传的图片
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/rename")
    public ResponseEntity<ApiResponse<?>> renameImage(
            @PathVariable("id") Long id,
            @RequestParam("newName") String newName) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("开始重命名图片: id={}, 新名称={}, 用户={}", id, newName, username);
            
            // 查找图片
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("图片不存在: ID=" + id));
            
            // 验证所有权（只有图片所有者或管理员可以重命名）
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                throw new RuntimeException("用户不存在: " + username);
            }
            
            if (!image.getUser().getId().equals(currentUser.getId()) && 
                    !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权重命名此图片"));
            }
            
            // 处理文件名，保留原始扩展名
            String originalFilename = image.getName();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            
            // 清理新文件名，允许中文，仅过滤不安全字符
            newName = newName.replaceAll("[\\\\/:*?\"<>|]", "_");
            
            // 如果新名称不包含扩展名，添加原始扩展名
            if (!newName.endsWith(fileExtension) && !fileExtension.isEmpty()) {
                newName = newName + fileExtension;
            }
            
            // 更新图片名称（仅修改数据库中的名称，S3存储中的文件路径不变）
            String oldName = image.getName();
            image.setName(newName);
            
            // 保存修改
            Image savedImage = imageRepository.save(image);
            
            logger.info("图片重命名成功: id={}, 旧名称={}, 新名称={}, 用户={}", 
                    savedImage.getId(), oldName, savedImage.getName(), username);
            
            return ResponseEntity.ok(ApiResponse.success(savedImage));
        } catch (Exception e) {
            logger.error("图片重命名失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("图片重命名失败: " + e.getMessage()));
        }
    }
} 