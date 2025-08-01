package com.example.blog.dto;

public class StatisticsDTO {
    private Integer articles;
    private Integer categories;
    private Integer tags;
    
    public StatisticsDTO() {
    }
    
    public StatisticsDTO(Integer articles, Integer categories, Integer tags) {
        this.articles = articles;
        this.categories = categories;
        this.tags = tags;
    }
    
    public Integer getArticles() {
        return articles;
    }
    
    public void setArticles(Integer articles) {
        this.articles = articles;
    }
    
    public Integer getCategories() {
        return categories;
    }
    
    public void setCategories(Integer categories) {
        this.categories = categories;
    }
    
    public Integer getTags() {
        return tags;
    }
    
    public void setTags(Integer tags) {
        this.tags = tags;
    }
} 