package com.example.blog.entity;

import com.example.blog.enums.ArticleLicense;
import com.example.blog.enums.ArticleStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "articles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column
    private Integer views;
    
    @Column
    private String cover;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
    
    @Column
    private String category;
    
    @Column
    private String author;
    
    /**
     * 从字符串转换为枚举的转换器
     */
    @Converter
    public static class LicenseConverter implements AttributeConverter<ArticleLicense, String> {
        @Override
        public String convertToDatabaseColumn(ArticleLicense license) {
            return license != null ? license.name() : null;
        }
        
        @Override
        public ArticleLicense convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return ArticleLicense.CC_BY_NC_SA_4_0; // 默认许可证
            }
            
            // 尝试将数据库中的值直接转换为枚举名称
            try {
                return ArticleLicense.valueOf(dbData);
            } catch (IllegalArgumentException e) {
                // 如果失败，可能是旧格式的字符串，尝试通过code查找
                return ArticleLicense.fromCode(dbData);
            }
        }
    }
    
    @Column
    @Enumerated(EnumType.STRING)
    @Convert(converter = LicenseConverter.class)
    private ArticleLicense license = ArticleLicense.CC_BY_NC_SA_4_0; // 默认使用CC BY-NC-SA 4.0
    
    @Column(name = "file_url", nullable = false)
    private String fileUrl;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ArticleStatus status = ArticleStatus.DRAFT; // 默认为草稿状态
    
    @Column(name = "is_featured")
    private Boolean isFeatured = false; // 是否为热门文章
    
    @Column(columnDefinition = "TEXT")
    private String tags; // 使用\分隔的标签列表
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "email", "role", "createdAt", "updatedAt"})
    private User user;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
    
    public ArticleLicense getLicense() {
        return license;
    }
    
    public void setLicense(ArticleLicense license) {
        this.license = license;
    }
    
    /**
     * 获取许可证代码
     */
    @Transient
    public String getLicenseCode() {
        return license != null ? license.getCode() : null;
    }
    
    // 接受字符串设置许可证，便于从前端接收数据
    public void setLicenseCode(String licenseCode) {
        this.license = ArticleLicense.fromCode(licenseCode);
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
    
    public ArticleStatus getStatus() {
        return status;
    }
    
    public void setStatus(ArticleStatus status) {
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
    
    // 获取标签数组
    @Transient // 不映射到数据库字段
    public String[] getTagArray() {
        if (tags == null || tags.isEmpty()) {
            return new String[0];
        }
        return tags.split("\\\\");
    }
    
    // 设置标签数组
    public void setTagArray(String[] tagArray) {
        if (tagArray == null || tagArray.length == 0) {
            this.tags = null;
            return;
        }
        this.tags = String.join("\\", tagArray);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
}