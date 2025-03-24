package com.example.blog.repository;

import com.example.blog.entity.Image;
import com.example.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    // 根据类型查询图片
    List<Image> findByType(String type);
    
    // 根据用户查询图片
    List<Image> findByUser(User user);
    
    // 根据类型和用户查询图片
    List<Image> findByTypeAndUser(String type, User user);
    
    // 根据名称模糊搜索
    @Query("SELECT i FROM Image i WHERE i.name LIKE %:keyword%")
    List<Image> searchByName(@Param("keyword") String keyword);
    
    // 根据名称模糊搜索和类型筛选
    @Query("SELECT i FROM Image i WHERE i.name LIKE %:keyword% AND i.type = :type")
    List<Image> searchByNameAndType(@Param("keyword") String keyword, @Param("type") String type);
    
    // 按创建时间升序排列
    @Query("SELECT i FROM Image i ORDER BY i.createdAt ASC")
    List<Image> findAllOrderByCreatedAtAsc();
    
    // 按创建时间降序排列
    @Query("SELECT i FROM Image i ORDER BY i.createdAt DESC")
    List<Image> findAllOrderByCreatedAtDesc();
    
    // 按创建时间降序排列，带类型筛选
    @Query("SELECT i FROM Image i WHERE i.type = :type ORDER BY i.createdAt DESC")
    List<Image> findByTypeOrderByCreatedAtDesc(@Param("type") String type);
    
    // 按创建时间升序排列，带类型筛选
    @Query("SELECT i FROM Image i WHERE i.type = :type ORDER BY i.createdAt ASC")
    List<Image> findByTypeOrderByCreatedAtAsc(@Param("type") String type);
} 