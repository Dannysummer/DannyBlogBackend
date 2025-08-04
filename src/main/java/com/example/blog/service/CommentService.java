package com.example.blog.service;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CommentStatsDto;
import com.example.blog.entity.Comment;
import com.example.blog.repository.CommentRepository;
import com.example.blog.util.SecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private SensitiveWordService sensitiveWordService;
    
    /**
     * 获取评论列表（分页）
     */
    public ApiResponse<Page<CommentDto>> getComments(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Comment> commentPage = commentRepository.findByParentIdIsNullOrderByTimeDesc(pageable);
            
            Page<CommentDto> dtoPage = commentPage.map(this::convertToDto);
            logger.info("获取评论列表成功，共{}条", dtoPage.getTotalElements());
            
            return ApiResponse.success(dtoPage);
        } catch (Exception e) {
            logger.error("获取评论列表失败", e);
            return ApiResponse.error("获取评论列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取热门评论
     */
    public ApiResponse<List<CommentDto>> getHotComments(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Comment> hotComments = commentRepository.findTopCommentsByLikes(pageable);
            
            List<CommentDto> dtoList = hotComments.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("获取热门评论成功，共{}条", dtoList.size());
            return ApiResponse.success(dtoList);
        } catch (Exception e) {
            logger.error("获取热门评论失败", e);
            return ApiResponse.error("获取热门评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建评论
     */
    @Transactional
    public ApiResponse<CommentDto> createComment(CommentDto.CreateCommentRequest request) {
        try {
            // 安全验证
            if (!SecurityValidator.isValidInput(request.getNickname(), "昵称")) {
                return ApiResponse.error("昵称包含潜在的安全风险");
            }
            if (!SecurityValidator.isValidInput(request.getContent(), "评论内容")) {
                return ApiResponse.error("评论内容包含潜在的安全风险");
            }
            if (request.getImage() != null && !SecurityValidator.isValidUrl(request.getImage(), "图片地址")) {
                return ApiResponse.error("图片地址包含潜在的安全风险");
            }
            
            // 敏感词过滤
            String filteredContent = sensitiveWordService.filter(request.getContent());
            if (!filteredContent.equals(request.getContent())) {
                logger.warn("评论内容包含敏感词，已过滤");
                request.setContent(filteredContent);
            }
            
            // 创建评论
            Comment comment = new Comment();
            comment.setNickname(request.getNickname());
            comment.setAvatar(request.getAvatar());
            comment.setContent(request.getContent());
            comment.setImage(request.getImage());
            comment.setTime(LocalDateTime.now());
            comment.setLikes(0);
            comment.setIsLiked(false);
            
            // 设置楼层号
            if (request.getParentId() == null) {
                // 顶级评论，设置楼层号
                long floorNumber = commentRepository.countTopLevelComments() + 1;
                comment.setFloor((int) floorNumber);
            } else {
                // 回复评论，设置父评论
                Comment parentComment = commentRepository.findById(request.getParentId())
                        .orElseThrow(() -> new IllegalArgumentException("父评论不存在"));
                comment.setParent(parentComment);
            }
            
            Comment savedComment = commentRepository.save(comment);
            CommentDto dto = convertToDto(savedComment);
            
            logger.info("创建评论成功，id={}", savedComment.getId());
            return ApiResponse.success(dto);
        } catch (IllegalArgumentException e) {
            logger.warn("创建评论失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            logger.error("创建评论失败", e);
            return ApiResponse.error("创建评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新评论
     */
    @Transactional
    public ApiResponse<CommentDto> updateComment(Long id, CommentDto.UpdateCommentRequest request) {
        try {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("评论不存在"));
            
            // 安全验证
            if (!SecurityValidator.isValidInput(request.getContent(), "评论内容")) {
                return ApiResponse.error("评论内容包含潜在的安全风险");
            }
            if (request.getImage() != null && !SecurityValidator.isValidUrl(request.getImage(), "图片地址")) {
                return ApiResponse.error("图片地址包含潜在的安全风险");
            }
            
            // 敏感词过滤
            String filteredContent = sensitiveWordService.filter(request.getContent());
            if (!filteredContent.equals(request.getContent())) {
                logger.warn("评论内容包含敏感词，已过滤");
                request.setContent(filteredContent);
            }
            
            comment.setContent(request.getContent());
            if (request.getImage() != null) {
                comment.setImage(request.getImage());
            }
            
            Comment updatedComment = commentRepository.save(comment);
            CommentDto dto = convertToDto(updatedComment);
            
            logger.info("更新评论成功，id={}", id);
            return ApiResponse.success(dto);
        } catch (IllegalArgumentException e) {
            logger.warn("更新评论失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            logger.error("更新评论失败", e);
            return ApiResponse.error("更新评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除评论
     */
    @Transactional
    public ApiResponse<String> deleteComment(Long id) {
        try {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("评论不存在"));
            
            // 删除评论及其所有回复
            commentRepository.delete(comment);
            
            logger.info("删除评论成功，id={}", id);
            return ApiResponse.success("评论删除成功");
        } catch (IllegalArgumentException e) {
            logger.warn("删除评论失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            logger.error("删除评论失败", e);
            return ApiResponse.error("删除评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 点赞/取消点赞评论
     */
    @Transactional
    public ApiResponse<CommentDto> likeComment(Long id, CommentDto.LikeCommentRequest request) {
        try {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("评论不存在"));
            
            if (request.getIsLiked()) {
                comment.setLikes(comment.getLikes() + 1);
            } else {
                comment.setLikes(Math.max(0, comment.getLikes() - 1));
            }
            comment.setIsLiked(request.getIsLiked());
            
            Comment updatedComment = commentRepository.save(comment);
            CommentDto dto = convertToDto(updatedComment);
            
            logger.info("{}评论成功，id={}", request.getIsLiked() ? "点赞" : "取消点赞", id);
            return ApiResponse.success(dto);
        } catch (IllegalArgumentException e) {
            logger.warn("点赞评论失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            logger.error("点赞评论失败", e);
            return ApiResponse.error("点赞评论失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取评论回复
     */
    public ApiResponse<List<CommentDto>> getReplies(Long parentId) {
        try {
            List<Comment> replies = commentRepository.findByParentIdOrderByTimeAsc(parentId);
            List<CommentDto> dtoList = replies.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("获取评论回复成功，parentId={}，共{}条", parentId, dtoList.size());
            return ApiResponse.success(dtoList);
        } catch (Exception e) {
            logger.error("获取评论回复失败", e);
            return ApiResponse.error("获取评论回复失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取评论统计信息
     */
    public ApiResponse<CommentStatsDto> getCommentStats() {
        try {
            long totalComments = commentRepository.countTopLevelComments();
            long totalReplies = commentRepository.count() - totalComments;
            
            CommentStatsDto stats = new CommentStatsDto(totalComments, totalReplies);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            logger.error("获取评论统计失败", e);
            return ApiResponse.error("获取评论统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 转换Comment实体为CommentDto
     */
    private CommentDto convertToDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setFloor(comment.getFloor());
        dto.setNickname(comment.getNickname());
        dto.setAvatar(comment.getAvatar());
        dto.setContent(comment.getContent());
        dto.setTime(comment.getTime());
        dto.setLikes(comment.getLikes());
        dto.setImage(comment.getImage());
        dto.setIsLiked(comment.getIsLiked());
        
        if (comment.getParent() != null) {
            dto.setParentId(comment.getParent().getId());
        }
        
        // 获取回复数量
        if (comment.getParent() == null) {
            long replyCount = commentRepository.countRepliesByParentId(comment.getId());
            dto.setReplyCount((int) replyCount);
        }
        
        return dto;
    }
} 