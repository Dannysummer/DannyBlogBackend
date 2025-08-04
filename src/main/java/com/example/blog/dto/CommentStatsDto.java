package com.example.blog.dto;

import lombok.Data;

@Data
public class CommentStatsDto {
    private long totalComments;
    private long totalReplies;
    private long totalCount;
    
    public CommentStatsDto(long totalComments, long totalReplies) {
        this.totalComments = totalComments;
        this.totalReplies = totalReplies;
        this.totalCount = totalComments + totalReplies;
    }
} 