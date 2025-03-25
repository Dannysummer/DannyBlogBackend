package com.example.blog.dto;

import com.example.blog.entity.Article;
import com.example.blog.enums.ArticleLicense;
import com.example.blog.enums.ArticleStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 文章数据传输对象，用于控制返回给前端的字段
 */
public class ArticleDto {
    private Long id;
    private String title;
    private Integer views;
    private String cover;
    private String content;
    private String description;
    private String aiSummary;  // 统一使用驼峰命名，与Entity保持一致
    private String category;
    private String author;
    private String license; // 保留字符串形式的许可证码
    private String licenseDescription; // 许可证描述文本
    private String fileUrl;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String status; // 文章状态：PUBLISHED、DRAFT、DELETED
    private Boolean isFeatured; // 是否为热门文章
    private String tags; // 使用\分隔的标签列表
    private String[] tagArray; // 标签数组，便于前端使用
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 不返回用户全部信息，但可以返回用户ID
    private Long userId;
    
    /**
     * 从文章实体创建DTO
     */
    public static ArticleDto fromEntity(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setViews(article.getViews());
        dto.setCover(article.getCover());
        dto.setContent(article.getContent());
        dto.setDescription(article.getDescription());
        dto.setAiSummary(article.getAiSummary());
        dto.setCategory(article.getCategory());
        dto.setAuthor(article.getAuthor());
        
        // 处理许可证
        ArticleLicense articleLicense = article.getLicense();
        if (articleLicense != null) {
            dto.setLicense(articleLicense.getCode());
            dto.setLicenseDescription(articleLicense.getDescription());
        } else {
            // 兼容旧数据，如果license是null，尝试使用默认许可证
            ArticleLicense defaultLicense = ArticleLicense.CC_BY_NC_SA_4_0;
            dto.setLicense(defaultLicense.getCode());
            dto.setLicenseDescription(defaultLicense.getDescription());
        }
        
        dto.setFileUrl(article.getFileUrl());
        dto.setFilePath(article.getFilePath());
        dto.setFileSize(article.getFileSize());
        dto.setFileType(article.getFileType());
        
        // 设置新添加的字段
        if (article.getStatus() != null) {
            dto.setStatus(article.getStatus().name());
        }
        dto.setIsFeatured(article.getIsFeatured());
        dto.setTags(article.getTags());
        dto.setTagArray(article.getTagArray());
        
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        
        // 设置用户ID（如果用户不为null）
        if (article.getUser() != null) {
            dto.setUserId(article.getUser().getId());
        }
        
        return dto;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Integer getViews() {
        return views;
    }
    
    public void setViews(Integer views) {
        this.views = views;
    }
    
    public String getCover() {
        return cover;
    }
    
    public void setCover(String cover) {
        this.cover = cover;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAiSummary() {
        return aiSummary;
    }
    
    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getLicense() {
        return license;
    }
    
    public void setLicense(String license) {
        this.license = license;
    }
    
    public String getLicenseDescription() {
        return licenseDescription;
    }
    
    public void setLicenseDescription(String licenseDescription) {
        this.licenseDescription = licenseDescription;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getIsFeatured() {
        return isFeatured;
    }
    
    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String[] getTagArray() {
        return tagArray;
    }
    
    public void setTagArray(String[] tagArray) {
        this.tagArray = tagArray;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 