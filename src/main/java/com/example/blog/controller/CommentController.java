package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CommentStatsDto;
import com.example.blog.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CommentController {
    
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    
    @Autowired
    private CommentService commentService;
    
    /**
     * 获取评论列表（分页）
     * GET /api/comments?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommentDto>>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("获取评论列表，page={}, size={}", page, size);
        ApiResponse<Page<CommentDto>> response = commentService.getComments(page, size);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取热门评论
     * GET /api/comments/hot?limit=5
     */
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getHotComments(
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("获取热门评论，limit={}", limit);
        ApiResponse<List<CommentDto>> response = commentService.getHotComments(limit);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建评论
     * POST /api/comments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentDto>> createComment(@RequestBody CommentDto.CreateCommentRequest request) {
        logger.info("创建评论请求: nickname={}, content={}", request.getNickname(), request.getContent());
        
        ApiResponse<CommentDto> response = commentService.createComment(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新评论
     * PUT /api/comments/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentDto>> updateComment(
            @PathVariable Long id,
            @RequestBody CommentDto.UpdateCommentRequest request) {
        
        logger.info("更新评论请求: id={}", id);
        ApiResponse<CommentDto> response = commentService.updateComment(id, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除评论
     * DELETE /api/comments/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable Long id) {
        logger.info("删除评论请求: id={}", id);
        
        ApiResponse<String> response = commentService.deleteComment(id);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 点赞/取消点赞评论
     * PUT /api/comments/{id}/like
     */
    @PutMapping("/{id}/like")
    public ResponseEntity<ApiResponse<CommentDto>> likeComment(
            @PathVariable Long id,
            @RequestBody CommentDto.LikeCommentRequest request) {
        
        logger.info("点赞评论请求: id={}, isLiked={}", id, request.getIsLiked());
        
        ApiResponse<CommentDto> response = commentService.likeComment(id, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取评论回复
     * GET /api/comments/{parentId}/replies
     */
    @GetMapping("/{parentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getReplies(@PathVariable Long parentId) {
        logger.info("获取评论回复请求: parentId={}", parentId);
        
        ApiResponse<List<CommentDto>> response = commentService.getReplies(parentId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取评论统计信息
     * GET /api/comments/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CommentStatsDto>> getCommentStats() {
        logger.info("获取评论统计信息请求");
        
        ApiResponse<CommentStatsDto> response = commentService.getCommentStats();
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 