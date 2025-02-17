package com.example.blog.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer views;
    
    private String cover;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String aiSummary;
    
    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    private List<String> tags;
    
    private String category;
    
    private String author;
    
    private String license;
} 