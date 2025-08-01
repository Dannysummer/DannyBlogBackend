package com.example.blog.dto;

import java.util.List;

/**
 * 分页响应数据传输对象
 */
public class PagedResponse<T> {
    
    private List<T> content;
    private long total;
    private int page;
    private int limit;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public PagedResponse() {}
    
    public PagedResponse(List<T> content, long total, int page, int limit) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.limit = limit;
        this.totalPages = (int) Math.ceil((double) total / limit);
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }
    
    // Getters and Setters
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public boolean isHasNext() {
        return hasNext;
    }
    
    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public boolean isHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
} 