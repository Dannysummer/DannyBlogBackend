package com.example.blog.service;

import com.example.blog.entity.Article;
import com.example.blog.repository.ArticleRepository;
import com.example.blog.enums.ArticleStatus;
import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleNavigationDto;
import com.example.blog.dto.ArticleArchiveDto;
import com.example.blog.dto.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Transactional
    public Article toggleFeature(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        
        // 切换精选状态
        Boolean currentIsFeatured = article.getIsFeatured();
        article.setIsFeatured(currentIsFeatured == null ? true : !currentIsFeatured);
        
        // 确保在事务中加载关联
        if (article.getUser() != null) {
            article.getUser().getId(); // 触发懒加载
        }
        
        return articleRepository.save(article);
    }
    
    /**
     * 获取最新发布的文章
     */
    public List<ArticleDto> getRecentPublishedArticles(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Article> articles = articleRepository.findRecentPublishedArticles(
            ArticleStatus.PUBLISHED, pageable);
        return articles.stream()
            .map(ArticleDto::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取相关文章
     */
    public List<ArticleDto> getRelatedArticles(Long articleId, int limit) {
        // 首先获取当前文章信息
        Article currentArticle = articleRepository.findById(articleId).orElse(null);
        if (currentArticle == null) {
            return new ArrayList<>();
        }
        
        // 获取第一个标签用于搜索相关文章
        String firstTag = "";
        if (currentArticle.getTags() != null && !currentArticle.getTags().isEmpty()) {
            String[] tags = currentArticle.getTags().split("\\\\");
            if (tags.length > 0) {
                firstTag = tags[0];
            }
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Article> articles = articleRepository.findRelatedArticles(
            articleId, 
            currentArticle.getCategory(), 
            firstTag, 
            pageable);
        
        return articles.stream()
            .map(ArticleDto::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取文章导航信息（上一篇/下一篇）
     */
    public Map<String, ArticleNavigationDto> getArticleNavigation(Long articleId) {
        Map<String, ArticleNavigationDto> navigation = new HashMap<>();
        
        // 获取上一篇文章
        Pageable pageable = PageRequest.of(0, 1);
        List<Article> prevArticles = articleRepository.findPreviousArticle(articleId, pageable);
        ArticleNavigationDto prevArticle = null;
        if (!prevArticles.isEmpty()) {
            prevArticle = ArticleNavigationDto.fromEntity(prevArticles.get(0));
        }
        
        // 获取下一篇文章
        List<Article> nextArticles = articleRepository.findNextArticle(articleId, pageable);
        ArticleNavigationDto nextArticle = null;
        if (!nextArticles.isEmpty()) {
            nextArticle = ArticleNavigationDto.fromEntity(nextArticles.get(0));
        }
        
        navigation.put("prevArticle", prevArticle);
        navigation.put("nextArticle", nextArticle);
        
        return navigation;
    }
    
    /**
     * 获取文章归档数据
     */
    public List<ArticleArchiveDto.YearArchive> getArticleArchive() {
        // 获取年份统计
        List<Object[]> yearStats = articleRepository.findPublishedArticleYearStatistics();
        
        List<ArticleArchiveDto.YearArchive> archives = new ArrayList<>();
        
        for (Object[] stat : yearStats) {
            int year = (Integer) stat[0];
            // 获取该年份的所有文章
            List<Article> articles = articleRepository.findPublishedArticlesByYear(year);
            
            List<ArticleArchiveDto.ArticleListItem> articleItems = articles.stream()
                .map(ArticleArchiveDto.ArticleListItem::fromEntity)
                .collect(Collectors.toList());
            
            archives.add(new ArticleArchiveDto.YearArchive(String.valueOf(year), articleItems));
        }
        
        return archives;
    }
    
    /**
     * 分页获取已发布文章列表
     */
    public Map<String, Object> getPublishedArticles(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit); // page从1开始，Pageable从0开始
        Page<Article> articlePage = articleRepository.findByStatusOrderByCreatedAtDesc(
            ArticleStatus.PUBLISHED, pageable);
        
        List<ArticleDto> articles = articlePage.getContent().stream()
            .map(ArticleDto::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);
        result.put("total", articlePage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        result.put("totalPages", articlePage.getTotalPages());
        
        return result;
    }
    
    /**
     * 获取已发布的文章详情（只返回已发布的文章）
     */
    public ArticleDto getPublishedArticleById(Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            return null;
        }
        
        // 增加访问量
        article.setViews(article.getViews() + 1);
        articleRepository.save(article);
        
        return ArticleDto.fromEntity(article);
    }
    
    /**
     * 获取当前用户的文章列表（支持分页和状态筛选）
     */
    public PagedResponse<ArticleDto> getArticlesByUser(String username, int page, int limit, String status) {
        logger.info("获取用户文章列表: username={}, page={}, limit={}, status={}", username, page, limit, status);
        
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Article> articlePage;
        
        if (status == null || status.isEmpty() || status.equalsIgnoreCase("ALL")) {
            // 获取用户的所有文章
            articlePage = articleRepository.findByAuthorOrderByCreatedAtDesc(username, pageable);
        } else {
            // 按状态筛选
            try {
                ArticleStatus articleStatus = ArticleStatus.valueOf(status.toUpperCase());
                articlePage = articleRepository.findByAuthorAndStatusOrderByCreatedAtDesc(username, articleStatus, pageable);
            } catch (IllegalArgumentException e) {
                logger.warn("无效的文章状态: {}, 返回所有文章", status);
                articlePage = articleRepository.findByAuthorOrderByCreatedAtDesc(username, pageable);
            }
        }
        
        List<ArticleDto> articles = articlePage.getContent().stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(articles, articlePage.getTotalElements(), page, limit);
    }
    
    /**
     * 管理员获取所有文章列表（支持分页、状态筛选、作者筛选、关键词搜索）
     */
    public PagedResponse<ArticleDto> getAllArticlesForAdmin(int page, int limit, String status, String author, String keyword) {
        logger.info("管理员获取所有文章: page={}, limit={}, status={}, author={}, keyword={}", 
                   page, limit, status, author, keyword);
        
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Article> articlePage;
        
        // 解析状态
        ArticleStatus articleStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                articleStatus = ArticleStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("无效的文章状态: {}", status);
            }
        }
        
        // 构建查询条件
        boolean hasAuthor = author != null && !author.trim().isEmpty();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = articleStatus != null;
        
        if (hasAuthor && hasKeyword && hasStatus) {
            // 作者 + 关键词 + 状态
            articlePage = articleRepository.searchByAuthorStatusAndKeyword(author.trim(), articleStatus, keyword.trim(), pageable);
        } else if (hasAuthor && hasKeyword) {
            // 作者 + 关键词
            articlePage = articleRepository.searchByAuthorAndKeyword(author.trim(), keyword.trim(), pageable);
        } else if (hasAuthor && hasStatus) {
            // 作者 + 状态
            articlePage = articleRepository.findByAuthorAndStatusOrderByCreatedAtDesc(author.trim(), articleStatus, pageable);
        } else if (hasKeyword && hasStatus) {
            // 关键词 + 状态
            articlePage = articleRepository.searchByStatusAndKeyword(articleStatus, keyword.trim(), pageable);
        } else if (hasAuthor) {
            // 仅作者
            articlePage = articleRepository.findByAuthorOrderByCreatedAtDesc(author.trim(), pageable);
        } else if (hasKeyword) {
            // 仅关键词
            articlePage = articleRepository.searchByKeyword(keyword.trim(), pageable);
        } else if (hasStatus) {
            // 仅状态
            articlePage = articleRepository.findByStatusOrderByCreatedAtDesc(articleStatus, pageable);
        } else {
            // 获取所有文章
            articlePage = articleRepository.findAllOrderByCreatedAtDesc(pageable);
        }
        
        List<ArticleDto> articles = articlePage.getContent().stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(articles, articlePage.getTotalElements(), page, limit);
    }
    
    /**
     * 获取文章详情（支持公开访问已发布文章，需要认证访问草稿）
     */
    public ArticleDto getArticleById(Long id, String currentUsername) {
        logger.info("获取文章详情: id={}, currentUsername={}", id, currentUsername);
        
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            return null;
        }
        
        // 检查访问权限
        if (article.getStatus() == ArticleStatus.PUBLISHED) {
            // 已发布的文章，任何人都可以访问，增加访问量
            article.setViews(article.getViews() + 1);
            articleRepository.save(article);
        } else {
            // 草稿或其他状态，需要验证是否为文章作者
            if (currentUsername == null || !currentUsername.equals(article.getAuthor())) {
                logger.warn("用户 {} 尝试访问非自己的草稿文章: {}", currentUsername, id);
                return null;
            }
        }
        
        return ArticleDto.fromEntity(article);
    }
    
    /**
     * 获取文章内容（支持公开访问已发布文章，需要认证访问草稿）
     */
    public String getArticleContent(Long id, String currentUsername) {
        ArticleDto article = getArticleById(id, currentUsername);
        if (article == null) {
            return null;
        }
        
        // 返回文章内容，如果内容为空则返回描述
        String content = article.getContent();
        if (content == null || content.trim().isEmpty()) {
            content = article.getDescription();
        }
        
        return content != null ? content : "文章内容暂无";
    }
    
    /**
     * 智能获取文章列表（根据用户权限返回不同的结果）
     */
    public PagedResponse<ArticleDto> getArticlesSmartly(int page, int limit, boolean includeAll, String currentUsername, boolean isAdmin) {
        logger.info("智能获取文章列表: page={}, limit={}, includeAll={}, currentUsername={}, isAdmin={}", 
                   page, limit, includeAll, currentUsername, isAdmin);
        
        if (includeAll && currentUsername != null) {
            if (isAdmin) {
                // 管理员获取所有文章
                return getAllArticlesForAdmin(page, limit, null, null, null);
            } else {
                // 普通用户获取自己的文章
                return getArticlesByUser(currentUsername, page, limit, null);
            }
        } else {
            // 获取公开的已发布文章
            Map<String, Object> publishedResult = getPublishedArticles(page, limit);
            @SuppressWarnings("unchecked")
            List<ArticleDto> articles = (List<ArticleDto>) publishedResult.get("articles");
            long total = (Long) publishedResult.get("total");
            
            return new PagedResponse<>(articles, total, page, limit);
        }
    }
}