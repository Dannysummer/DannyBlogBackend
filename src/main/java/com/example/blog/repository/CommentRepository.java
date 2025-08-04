package com.example.blog.repository;

import com.example.blog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * 根据父评论ID查找回复
     */
    List<Comment> findByParentIdOrderByTimeAsc(Long parentId);
    
    /**
     * 查找所有顶级评论（parent_id为null）
     */
    List<Comment> findByParentIdIsNullOrderByTimeDesc();
    
    /**
     * 分页查找顶级评论
     */
    Page<Comment> findByParentIdIsNullOrderByTimeDesc(Pageable pageable);
    
    /**
     * 根据昵称查找评论
     */
    List<Comment> findByNicknameOrderByTimeDesc(String nickname);
    
    /**
     * 统计评论总数
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentId IS NULL")
    long countTopLevelComments();
    
    /**
     * 统计某个父评论的回复数
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentId = :parentId")
    long countRepliesByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查找点赞数最多的评论
     */
    @Query("SELECT c FROM Comment c WHERE c.parentId IS NULL ORDER BY c.likes DESC, c.time DESC")
    List<Comment> findTopCommentsByLikes(Pageable pageable);
    
    /**
     * 根据时间范围查找评论
     */
    @Query("SELECT c FROM Comment c WHERE c.time BETWEEN :startTime AND :endTime ORDER BY c.time DESC")
    List<Comment> findByTimeBetween(@Param("startTime") java.time.LocalDateTime startTime, 
                                   @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 删除指定昵称的所有评论
     */
    void deleteByNickname(String nickname);
    
    /**
     * 更新评论点赞数
     */
    @Query("UPDATE Comment c SET c.likes = :likes WHERE c.id = :id")
    void updateLikes(@Param("id") Long id, @Param("likes") Integer likes);
} 