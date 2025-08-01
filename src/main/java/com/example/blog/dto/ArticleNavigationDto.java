package com.example.blog.dto;

import com.example.blog.entity.Article;

/**
 * 文章导航DTO，用于上一篇/下一篇文章信息
 */
public class ArticleNavigationDto {
    private Long id;
    private String title;
    private String cover;
    private String description;
    
    public static ArticleNavigationDto fromEntity(Article article) {
        if (article == null) {
            return null;
        }
        
        ArticleNavigationDto dto = new ArticleNavigationDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setCover(article.getCover());
        dto.setDescription(article.getDescription());
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
    
    public String getCover() {
        return cover;
    }
    
    public void setCover(String cover) {
        this.cover = cover;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
} 