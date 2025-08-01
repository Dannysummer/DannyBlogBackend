package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.dto.HeatmapDTO;
import com.example.blog.dto.LatestArticleDTO;
import com.example.blog.dto.StatisticsDTO;
import com.example.blog.dto.SystemStatsDTO;
import com.example.blog.entity.Progress;
import com.example.blog.entity.Tag;
import com.example.blog.service.DashboardService;
import com.example.blog.service.ProgressService;
import com.example.blog.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private TagService tagService;
    
    @Autowired
    private ProgressService progressService;
    
    /**
     * 获取系统状态
     */
    @GetMapping("/system/stats")
    public ResponseEntity<?> getSystemStats() {
        try {
            logger.debug("请求系统状态数据");
            SystemStatsDTO stats = dashboardService.getSystemStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取系统状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("获取系统状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            logger.debug("请求统计数据");
            StatisticsDTO stats = dashboardService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取统计数据失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("获取统计数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取文章热图数据
     */
    @GetMapping("/articles/heatmap")
    public ResponseEntity<?> getArticleHeatmap() {
        try {
            logger.debug("请求文章热图数据");
            List<HeatmapDTO> heatmap = dashboardService.getArticleHeatmap();
            return ResponseEntity.ok(heatmap);
        } catch (Exception e) {
            logger.error("获取文章热图失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("获取文章热图失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取最新文章列表
     */
    @GetMapping("/articles/latest")
    public ResponseEntity<?> getLatestArticles() {
        try {
            logger.debug("请求最新文章列表");
            List<LatestArticleDTO> articles = dashboardService.getLatestArticles();
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            logger.error("获取最新文章失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("获取最新文章失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有标签
     */
    @GetMapping("/tags")
    public ResponseEntity<?> getAllTags() {
        try {
            logger.debug("请求所有标签");
            List<Tag> tags = tagService.getAllTags();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("获取标签失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("获取标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建标签
     */
    @PostMapping("/tags")
    public ResponseEntity<?> createTag(@RequestBody Tag tag) {
        try {
            logger.debug("创建标签: {}", tag.getName());
            if (tag.getName() == null || tag.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("标签名称不能为空"));
            }
            
            Tag createdTag = tagService.createTag(tag);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
        } catch (Exception e) {
            logger.error("创建标签失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("创建标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/tags/{id}")
    public ResponseEntity<?> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        try {
            logger.debug("更新标签ID: {}, 新名称: {}", id, tag.getName());
            if (tag.getName() == null || tag.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("标签名称不能为空"));
            }
            
            Tag updatedTag = tagService.updateTag(id, tag);
            if (updatedTag != null) {
                return ResponseEntity.ok(updatedTag);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("未找到ID为" + id + "的标签"));
        } catch (Exception e) {
            logger.error("更新标签失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("更新标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        try {
            logger.debug("删除标签ID: {}", id);
            boolean deleted = tagService.deleteTag(id);
            Map<String, Object> response = new HashMap<>();
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "标签删除成功");
                return ResponseEntity.ok(response);
            }
            
            response.put("success", false);
            response.put("message", "标签不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("删除标签失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("删除标签失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有进度项
     */
    @GetMapping("/progress")
    public ResponseEntity<?> getAllProgress() {
        try {
            logger.debug("请求所有进度项");
            List<Progress> progressItems = progressService.getAllProgress();
            return ResponseEntity.ok(progressItems);
        } catch (Exception e) {
            logger.error("获取进度项失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("获取进度项失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建进度项
     */
    @PostMapping("/progress")
    public ResponseEntity<?> createProgress(@RequestBody Progress progress) {
        try {
            logger.debug("创建进度项: {}", progress.getText());
            if (progress.getText() == null || progress.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("进度项内容不能为空"));
            }
            
            // 确保completed有默认值
            if (progress.getCompleted() == null) {
                progress.setCompleted(false);
            }
            
            Progress createdProgress = progressService.createProgress(progress);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProgress);
        } catch (Exception e) {
            logger.error("创建进度项失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("创建进度项失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新进度项
     */
    @PutMapping("/progress/{id}")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestBody Progress progress) {
        try {
            logger.debug("更新进度项ID: {}, 内容: {}, 完成状态: {}", 
                id, progress.getText(), progress.getCompleted());
                
            if (progress.getText() == null || progress.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("进度项内容不能为空"));
            }
            
            Progress updatedProgress = progressService.updateProgress(id, progress);
            if (updatedProgress != null) {
                return ResponseEntity.ok(updatedProgress);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("未找到ID为" + id + "的进度项"));
        } catch (Exception e) {
            logger.error("更新进度项失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("更新进度项失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除进度项
     */
    @DeleteMapping("/progress/{id}")
    public ResponseEntity<?> deleteProgress(@PathVariable Long id) {
        try {
            logger.debug("删除进度项ID: {}", id);
            boolean deleted = progressService.deleteProgress(id);
            Map<String, Object> response = new HashMap<>();
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "进度项删除成功");
                return ResponseEntity.ok(response);
            }
            
            response.put("success", false);
            response.put("message", "进度项不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("删除进度项失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("删除进度项失败: " + e.getMessage()));
        }
    }
} 