package com.example.blog.repository;

import com.example.blog.entity.Article;
import com.example.blog.entity.User;
import com.example.blog.enums.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    /**
     * 查找指定用户的所有文章，按创建时间降序排列（最新的在前）
     */
    List<Article> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 查找指定用户的所有文章，按创建时间升序排列（最旧的在前）
     */
    List<Article> findByUserOrderByCreatedAtAsc(User user);
    
    /**
     * 根据标题关键词搜索文章
     */
    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY a.createdAt DESC")
    List<Article> searchByTitle(String keyword);
    
    /**
     * 根据标题关键词和用户搜索文章
     */
    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND a.user = :user ORDER BY a.createdAt DESC")
    List<Article> searchByTitleAndUser(String keyword, User user);
    
    /**
     * 查找所有文章，按创建时间降序排列
     */
    @Query("SELECT a FROM Article a ORDER BY a.createdAt DESC")
    List<Article> findAllOrderByCreatedAtDesc();
    
    /**
     * 查找所有文章，按创建时间升序排列
     */
    @Query("SELECT a FROM Article a ORDER BY a.createdAt ASC")
    List<Article> findAllOrderByCreatedAtAsc();
    
    /**
     * 查找热门文章（被标记为isFeatured=true的文章），按创建时间降序排列
     */
    @Query("SELECT a FROM Article a WHERE a.isFeatured = true ORDER BY a.createdAt DESC")
    List<Article> findFeaturedArticles();
    
    /**
     * 查找热门文章并限制数量，按创建时间降序排列
     */
    @Query("SELECT a FROM Article a WHERE a.isFeatured = true ORDER BY a.createdAt DESC")
    List<Article> findFeaturedArticlesWithLimit(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 查找阅读量最高的文章（自动热门）
     */
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' ORDER BY a.views DESC")
    List<Article> findMostViewedArticles(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 根据文章状态查找文章
     */
    List<Article> findByStatus(ArticleStatus status);
    
    /**
     * 根据用户和文章状态查找文章
     */
    List<Article> findByUserAndStatus(User user, ArticleStatus status);
    
    /**
     * 根据文章状态查找文章，按创建时间降序排列
     */
    List<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status);
    
    /**
     * 根据标签查找文章（模糊匹配）
     */
    @Query("SELECT a FROM Article a WHERE a.tags LIKE %:tag% ORDER BY a.createdAt DESC")
    List<Article> findByTagContaining(@Param("tag") String tag);
    
    /**
     * 查找并转换许可证字段
     * 这个方法会在加载实体后自动设置正确的枚举值
     */
    @Query("SELECT a FROM Article a")
    default List<Article> findAllWithLicenseConverted() {
        List<Article> articles = findAll();
        for (Article article : articles) {
            // 如果license字段是字符串，则转换为枚举值
            if (article.getLicense() == null && article.getLicenseCode() != null) {
                article.setLicenseCode(article.getLicenseCode());
            }
        }
        return articles;
    }
} 