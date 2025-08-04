package com.example.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDto {
    private Long id;
    private Integer floor;
    private String nickname;
    private String avatar;
    private String content;
    private LocalDateTime time;
    private Integer likes;
    private String image;
    private Boolean isLiked;
    private Long parentId;
    private List<CommentDto> replies;
    private Integer replyCount;
    
    // 用于创建评论的请求DTO
    @Data
    public static class CreateCommentRequest {
        private String nickname;
        private String avatar;
        private String content;
        private String image;
        private Long parentId;
    }
    
    // 用于更新评论的请求DTO
    @Data
    public static class UpdateCommentRequest {
        private String content;
        private String image;
    }
    
    // 用于点赞的请求DTO
    @Data
    public static class LikeCommentRequest {
        private Boolean isLiked;
    }
} 