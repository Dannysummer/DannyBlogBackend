package com.example.blog.service;

// import com.example.blog.dto.ApiResponse;
import com.example.blog.entity.Image;
import com.example.blog.entity.User;
import com.example.blog.repository.ImageRepository;
import com.example.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
// import java.time.LocalDateTime;
import java.util.ArrayList;
// import java.util.Date;
// import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    
    @Autowired
    private S3UploadService s3UploadService;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Value("${app.image.thumbnail.width:200}")
    private int thumbnailWidth;
    
    @Value("${app.image.thumbnail.height:200}")
    private int thumbnailHeight;
    
    @Value("${app.image.custom-domain:}")
    private String customDomain;
    
    /**
     * 获取图片列表
     */
    public List<com.example.blog.entity.Image> getAllImages(String type, String sort, String search) {
        logger.info("获取图片列表：type={}, sort={}, search={}", type, sort, search);
        
        // 如果有搜索关键词
        if (search != null && !search.isEmpty()) {
            if (type != null && !type.equals("all")) {
                return imageRepository.searchByNameAndType(search, type);
            } else {
                return imageRepository.searchByName(search);
            }
        }
        
        // 根据排序方式
        if ("oldest".equals(sort)) {
            if (type != null && !type.equals("all")) {
                return imageRepository.findByTypeOrderByCreatedAtAsc(type);
            } else {
                return imageRepository.findAllOrderByCreatedAtAsc();
            }
        } else {
            // 默认按最新排序
            if (type != null && !type.equals("all")) {
                return imageRepository.findByTypeOrderByCreatedAtDesc(type);
            } else {
                return imageRepository.findAllOrderByCreatedAtDesc();
            }
        }
    }
    
    /**
     * 获取指定ID的图片
     */
    public com.example.blog.entity.Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("图片不存在"));
    }
    
    /**
     * 删除图片
     */
    public void deleteImage(Long id) {
        com.example.blog.entity.Image image = getImageById(id);
        try {
            // 删除多吉云存储桶中的原图
            s3UploadService.deleteFile(image.getPath());
            logger.info("成功删除多吉云存储桶中的原图：{}", image.getPath());
            
            // 删除多吉云存储桶中的缩略图
            if (image.getThumbnailUrl() != null) {
                // 从thumbnailUrl中提取路径而不是自己构建
                String thumbnailUrl = image.getThumbnailUrl();
                String thumbnailPath = null;
                
                // 如果使用了自定义域名，需要提取路径部分
                if (customDomain != null && !customDomain.isEmpty() && thumbnailUrl.startsWith(customDomain)) {
                    // 从自定义域名URL中提取路径部分
                    thumbnailPath = thumbnailUrl.substring(customDomain.length());
                    // 确保路径不以/开头
                    if (thumbnailPath.startsWith("/")) {
                        thumbnailPath = thumbnailPath.substring(1);
                    }
                } else {
                    // 对于没有使用自定义域名的情况，尝试提取路径
                    // 例如 https://s3endpoint.example.com/user/images/abc_thumb.jpg -> user/images/abc_thumb.jpg
                    int pathStartPos = thumbnailUrl.indexOf("/", 8); // 跳过 https://
                    if (pathStartPos > 0) {
                        thumbnailPath = thumbnailUrl.substring(pathStartPos + 1);
                    }
                }
                
                if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                    logger.info("正在删除缩略图，路径：{}", thumbnailPath);
                    s3UploadService.deleteFile(thumbnailPath);
                    logger.info("成功删除多吉云存储桶中的缩略图：{}", thumbnailPath);
                } else {
                    logger.warn("无法从URL提取缩略图路径：{}", thumbnailUrl);
                }
            }
            
            // 删除数据库记录
            imageRepository.delete(image);
            logger.info("删除图片记录成功：id={}, path={}", id, image.getPath());
        } catch (Exception e) {
            logger.error("删除图片失败：id={}, path={}", id, image.getPath(), e);
            throw new RuntimeException("删除图片失败：" + e.getMessage());
        }
    }
    
    /**
     * 上传图片，生成缩略图
     */
    public List<com.example.blog.entity.Image> uploadImages(List<MultipartFile> files, String type, String username) {
        com.example.blog.entity.User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        List<com.example.blog.entity.Image> savedImages = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // 上传原图
                Map<String, Object> result = s3UploadService.uploadFile(file);
                
                // 生成缩略图并上传
                String thumbnailUrl = generateAndUploadThumbnail(file, type);
                
                // 创建图片记录
                com.example.blog.entity.Image image = new com.example.blog.entity.Image();
                image.setName((String) result.get("filename"));
                
                // 使用返回的URL，这里已经考虑了自定义域名
                image.setUrl((String) result.get("url"));
                image.setThumbnailUrl(thumbnailUrl);
                
                image.setType(type);
                image.setSize((Long) result.get("size"));
                image.setPath((String) result.get("path"));
                image.setContentType(file.getContentType());
                image.setUser(user);
                
                // 保存图片记录
                com.example.blog.entity.Image savedImage = imageRepository.save(image);
                savedImages.add(savedImage);
                
                logger.info("图片上传成功：name={}, type={}, user={}", 
                           savedImage.getName(), savedImage.getType(), username);
            } catch (Exception e) {
                logger.error("图片上传失败：", e);
                throw new RuntimeException("图片上传失败：" + e.getMessage());
            }
        }
        
        return savedImages;
    }
    
    /**
     * 上传图片，生成缩略图（带进度监控）
     */
    public List<com.example.blog.entity.Image> uploadImagesWithProgress(List<MultipartFile> files, String type, String username) {
        com.example.blog.entity.User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        List<com.example.blog.entity.Image> savedImages = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // 创建进度监听器
                UploadProgressListener progressListener = new UploadProgressListener(messagingTemplate, file.getOriginalFilename(), username);
                
                // 上传原图（带进度监控）
                Map<String, Object> result = s3UploadService.uploadFileWithProgress(file, progressListener);
                
                // 生成缩略图并上传
                String thumbnailUrl = generateAndUploadThumbnail(file, type);
                
                // 创建图片记录
                com.example.blog.entity.Image image = new com.example.blog.entity.Image();
                image.setName((String) result.get("filename"));
                
                // 使用返回的URL，这里已经考虑了自定义域名
                image.setUrl((String) result.get("url"));
                image.setThumbnailUrl(thumbnailUrl);
                
                image.setType(type);
                image.setSize((Long) result.get("size"));
                image.setPath((String) result.get("path"));
                image.setContentType(file.getContentType());
                image.setUser(user);
                
                // 保存图片记录
                com.example.blog.entity.Image savedImage = imageRepository.save(image);
                savedImages.add(savedImage);
                
                logger.info("图片上传成功（带进度监控）：name={}, type={}, user={}", 
                           savedImage.getName(), savedImage.getType(), username);
            } catch (Exception e) {
                logger.error("图片上传失败（带进度监控）：", e);
                throw new RuntimeException("图片上传失败：" + e.getMessage());
            }
        }
        
        return savedImages;
    }
    
    /**
     * 生成缩略图并上传
     */
    private String generateAndUploadThumbnail(MultipartFile file, String type) throws Exception {
        // 读取原图
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            logger.warn("无法读取图片或不支持的图片格式：{}", file.getOriginalFilename());
            return null;
        }
        
        // 计算缩略图尺寸，保持比例
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double ratio = (double) originalWidth / originalHeight;
        
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
        String imageFormat = file.getContentType() != null && file.getContentType().contains("png") ? "png" : "jpeg";
        ImageIO.write(thumbnailImage, imageFormat, baos);
        
        // 构建缩略图文件名
        String originalFilename = file.getOriginalFilename();
        String thumbFilename = originalFilename != null 
                ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "_thumb." + imageFormat
                : "thumbnail_" + System.currentTimeMillis() + "." + imageFormat;
        
        // 准备上传缩略图
        MultipartFileImpl thumbnailFile = new MultipartFileImpl(
                thumbFilename,
                file.getContentType(),
                baos.toByteArray()
        );
        
        // 上传缩略图
        Map<String, Object> result = s3UploadService.uploadFile(thumbnailFile);
        logger.info("缩略图上传成功：{}", result.get("url"));
        
        // 直接使用返回的URL，s3UploadService中已经处理了自定义域名
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
        public byte[] getBytes() {
            return content;
        }
        
        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            throw new UnsupportedOperationException("transferTo() is not supported");
        }
    }

    /**
     * 上传单张图片（带进度监控）
     */
    public Image uploadImageWithProgress(MultipartFile file, String type, String username) throws IOException {
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String fileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;
            String filePath = "images/" + fileName;
            String scope = "user-img-data:" + filePath;
            
            // 创建进度监听器
            UploadProgressListener progressListener = new UploadProgressListener(
                messagingTemplate, 
                originalFilename, 
                username
            );
            
            // 上传文件（带进度监控）
            Map<String, Object> result = s3UploadService.uploadFileWithProgress(file, progressListener);
            
            // 获取用户
            User user = userRepository.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在：" + username);
            }
            
            // 创建图片记录
            Image image = new Image();
            image.setName(fileName);
            image.setType(type);
            image.setUrl((String) result.get("url"));
            image.setUser(user);
            image.setPath(filePath);
            image.setSize(file.getSize());
            image.setContentType(file.getContentType());
            
            // 保存到数据库
            Image savedImage = imageRepository.save(image);
            logger.info("图片上传成功：name={}, type={}, user={}", fileName, type, username);
            
            return savedImage;
        } catch (Exception e) {
            logger.error("图片上传失败：", e);
            throw new IOException("图片上传失败：" + e.getMessage());
        }
    }
} 