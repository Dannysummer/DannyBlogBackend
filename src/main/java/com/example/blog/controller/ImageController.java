package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.entity.Image;
import com.example.blog.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    
    @Autowired
    private ImageService imageService;
    
    /**
     * 获取图片列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllImages(
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false) String search) {
        try {
            List<Image> images = imageService.getAllImages(type, sort, search);
            logger.info("成功获取图片列表，共{}张", images.size());
            return ResponseEntity.ok(ApiResponse.success(images));
        } catch (Exception e) {
            logger.error("获取图片列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取图片列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取图片
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getImageById(@PathVariable Long id) {
        try {
            Image image = imageService.getImageById(id);
            logger.info("成功获取图片，ID：{}", id);
            return ResponseEntity.ok(ApiResponse.success(image));
        } catch (Exception e) {
            logger.error("获取图片失败，ID：{}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取图片失败：" + e.getMessage()));
        }
    }
    
    /**
     * 上传图片
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<?>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false, defaultValue = "image") String type) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            List<Image> savedImages = imageService.uploadImages(files, type, username);
            logger.info("成功上传{}张图片", savedImages.size());
            return ResponseEntity.ok(ApiResponse.success(savedImages));
        } catch (Exception e) {
            logger.error("上传图片失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("上传图片失败：" + e.getMessage()));
        }
    }
    
    /**
     * 上传图片（带进度监控）
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload-with-progress")
    public ResponseEntity<ApiResponse<?>> uploadImageWithProgress(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("请选择要上传的图片"));
            }
            
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("开始上传图片（带进度监控）：type={}, file={}, user={}", 
                      type, file.getOriginalFilename(), username);
            
            // 使用带进度监控的上传方法
            com.example.blog.entity.Image image = imageService.uploadImageWithProgress(
                    file, type, username);
            
            logger.info("图片上传成功（带进度监控）：file={}, user={}", 
                      file.getOriginalFilename(), username);
            
            return ResponseEntity.ok(ApiResponse.success(image));
        } catch (Exception e) {
            logger.error("图片上传失败（带进度监控）：", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("图片上传失败：" + e.getMessage()));
        }
    }
    
    /**
     * 删除图片
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteImage(@PathVariable Long id) {
        try {
            imageService.deleteImage(id);
            logger.info("成功删除图片，ID：{}", id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            logger.error("删除图片失败，ID：{}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("删除图片失败：" + e.getMessage()));
        }
    }
} 