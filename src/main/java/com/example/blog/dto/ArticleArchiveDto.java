package com.example.blog.dto;

import com.example.blog.entity.Article;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章归档DTO
 */
public class ArticleArchiveDto {
    
    /**
     * 年份信息
     */
    public static class YearArchive {
        private String year;
        private List<ArticleListItem> articles;
        
        public YearArchive(String year, List<ArticleListItem> articles) {
            this.year = year;
            this.articles = articles;
        }
        
        // Getters and Setters
        public String getYear() {
            return year;
        }
        
        public void setYear(String year) {
            this.year = year;
        }
        
        public List<ArticleListItem> getArticles() {
            return articles;
        }
        
        public void setArticles(List<ArticleListItem> articles) {
            this.articles = articles;
        }
    }
    
    /**
     * 文章列表项
     */
    public static class ArticleListItem {
        private Long id;
        private String title;
        private String createTime; // 格式化后的创建时间
        private String cover;
        
        public static ArticleListItem fromEntity(Article article) {
            ArticleListItem item = new ArticleListItem();
            item.setId(article.getId());
            item.setTitle(article.getTitle());
            item.setCreateTime(article.getCreatedAt().toLocalDate().toString()); // 只返回日期部分
            item.setCover(article.getCover());
            return item;
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
        
        public String getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }
        
        public String getCover() {
            return cover;
        }
        
        public void setCover(String cover) {
            this.cover = cover;
        }
    }
} 