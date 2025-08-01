package com.example.blog.repository;

import com.example.blog.entity.Article;
import com.example.blog.entity.User;
import com.example.blog.enums.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

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
     * 查找已发布的文章，按创建时间降序排列，支持分页
     */
    Page<Article> findByStatusOrderByCreatedAtDesc(ArticleStatus status, Pageable pageable);
    
    /**
     * 查找最新发布的文章，限制数量
     */
    @Query("SELECT a FROM Article a WHERE a.status = :status ORDER BY a.createdAt DESC")
    List<Article> findRecentPublishedArticles(@Param("status") ArticleStatus status, Pageable pageable);
    
    /**
     * 根据分类和标签查找相关文章（排除当前文章）
     */
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.id != :excludeId AND " +
           "(a.category = :category OR a.tags LIKE %:tag%) ORDER BY a.createdAt DESC")
    List<Article> findRelatedArticles(@Param("excludeId") Long excludeId, 
                                     @Param("category") String category, 
                                     @Param("tag") String tag, 
                                     Pageable pageable);
    
    /**
     * 查找指定文章的上一篇文章（按创建时间）
     */
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.createdAt < " +
           "(SELECT article.createdAt FROM Article article WHERE article.id = :articleId) " +
           "ORDER BY a.createdAt DESC")
    List<Article> findPreviousArticle(@Param("articleId") Long articleId, Pageable pageable);
    
    /**
     * 查找指定文章的下一篇文章（按创建时间）
     */
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND a.createdAt > " +
           "(SELECT article.createdAt FROM Article article WHERE article.id = :articleId) " +
           "ORDER BY a.createdAt ASC")
    List<Article> findNextArticle(@Param("articleId") Long articleId, Pageable pageable);
    
    /**
     * 查找已发布文章的年份统计
     */
    @Query("SELECT YEAR(a.createdAt) as year, COUNT(a) as count FROM Article a " +
           "WHERE a.status = 'PUBLISHED' GROUP BY YEAR(a.createdAt) ORDER BY year DESC")
    List<Object[]> findPublishedArticleYearStatistics();
    
    /**
     * 查找指定年份的已发布文章
     */
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND YEAR(a.createdAt) = :year " +
           "ORDER BY a.createdAt DESC")
    List<Article> findPublishedArticlesByYear(@Param("year") int year);
    
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
    
    /**
     * 根据作者名称分页查询文章，按创建时间降序排列
     */
    @Query("SELECT a FROM Article a WHERE a.author = :author ORDER BY a.createdAt DESC")
    Page<Article> findByAuthorOrderByCreatedAtDesc(@Param("author") String author, Pageable pageable);
    
    /**
     * 根据作者名称和状态分页查询文章，按创建时间降序排列
     */
    @Query("SELECT a FROM Article a WHERE a.author = :author AND a.status = :status ORDER BY a.createdAt DESC")
    Page<Article> findByAuthorAndStatusOrderByCreatedAtDesc(@Param("author") String author, 
                                                            @Param("status") ArticleStatus status, 
                                                            Pageable pageable);
    
    /**
     * 查找所有文章（支持分页），按创建时间降序排列
     */
    @Query("SELECT a FROM Article a ORDER BY a.createdAt DESC")
    Page<Article> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 根据关键词搜索文章（标题或内容），支持分页
     */
    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY a.createdAt DESC")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 根据作者名称和关键词搜索文章，支持分页
     */
    @Query("SELECT a FROM Article a WHERE a.author = :author AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY a.createdAt DESC")
    Page<Article> searchByAuthorAndKeyword(@Param("author") String author, 
                                          @Param("keyword") String keyword, 
                                          Pageable pageable);
    
    /**
     * 根据状态和关键词搜索文章，支持分页
     */
    @Query("SELECT a FROM Article a WHERE a.status = :status AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY a.createdAt DESC")
    Page<Article> searchByStatusAndKeyword(@Param("status") ArticleStatus status, 
                                          @Param("keyword") String keyword, 
                                          Pageable pageable);
    
    /**
     * 根据作者名称、状态和关键词搜索文章，支持分页
     */
    @Query("SELECT a FROM Article a WHERE a.author = :author AND a.status = :status AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY a.createdAt DESC")
    Page<Article> searchByAuthorStatusAndKeyword(@Param("author") String author, 
                                                @Param("status") ArticleStatus status, 
                                                @Param("keyword") String keyword, 
                                                Pageable pageable);
    
    /**
     * 根据作者名称查询所有文章，按创建时间降序排列（不分页）
     */
    @Query("SELECT a FROM Article a WHERE a.author = :author ORDER BY a.createdAt DESC")
    List<Article> findByAuthorOrderByCreatedAtDesc(@Param("author") String author);
    
    /**
     * 根据作者名称和状态查询所有文章，按创建时间降序排列（不分页）
     */
    @Query("SELECT a FROM Article a WHERE a.author = :author AND a.status = :status ORDER BY a.createdAt DESC")
    List<Article> findByAuthorAndStatusOrderByCreatedAtDesc(@Param("author") String author, 
                                                            @Param("status") ArticleStatus status);
} 