package com.example.blog.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "images")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 255, nullable = false)
    private String name;
    
    @Column(length = 255, nullable = false)
    private String url;
    
    @Column(length = 500)
    private String thumbnailUrl;
    
    @Column(length = 50)
    private String type; // image, avatar, cover, article
    
    @Column(nullable = false)
    private Long size; // 文件大小，单位字节
    
    @Column(length = 255, nullable = false)
    private String path; // 存储路径
    
    @Column(length = 100)
    private String contentType; // MIME类型
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 上传用户
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 