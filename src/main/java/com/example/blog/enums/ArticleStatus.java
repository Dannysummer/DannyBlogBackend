package com.example.blog.enums;

/**
 * 文章状态枚举
 */
public enum ArticleStatus {
    /**
     * 已发布 - 文章已正式发布，可供所有有权限的用户查看
     */
    PUBLISHED("已发布"),
    
    /**
     * 草稿 - 文章尚未发布，仅作者可见
     */
    DRAFT("草稿"),
    
    /**
     * 已删除 - 文章已被删除，但未从数据库物理删除
     */
    DELETED("已删除");
    
    private final String description;
    
    ArticleStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 