package com.example.blog.dto;

public class LatestArticleDTO {
    private Long id;
    private String title;
    private String createdAt;
    private String summary;
    private String tags;
    
    public LatestArticleDTO() {
    }
    
    public LatestArticleDTO(Long id, String title, String createdAt, String summary, String tags) {
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
        this.summary = summary;
        this.tags = tags;
    }
    
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
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
} 