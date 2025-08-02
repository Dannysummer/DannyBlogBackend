package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.entity.FriendLink;
import com.example.blog.entity.FriendCategory;
import com.example.blog.service.FriendLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend-links")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FriendLinkController {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendLinkController.class);
    
    @Autowired
    private FriendLinkService friendLinkService;
    
    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllFriendLinks() {
        logger.info("收到获取所有友链的请求");
        try {
            List<FriendLink> allLinks = friendLinkService.getAllFriendLinks();
            
            Map<String, String> categoryNames = new HashMap<>();
            categoryNames.put("all", "全部");
            categoryNames.put("bigshot", "大佬");
            categoryNames.put("close", "密友");
            categoryNames.put("friend", "普通朋友");
            categoryNames.put("tech", "官方技术博客");
            
            Map<String, Object> response = new HashMap<>();
            response.put("friends", allLinks);
            response.put("categoryNames", categoryNames);
            
            logger.info("成功获取所有友链，共{}条", allLinks.size());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("获取所有友链失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取友链失败: " + e.getMessage()));
        }
    }
    
    @PreAuthorize("permitAll()")
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<?>> getFriendLinksByCategory(@PathVariable String category) {
        logger.info("收到获取分类{}友链的请求", category);
        try {
            List<FriendLink> links;
            if ("all".equals(category)) {
                links = friendLinkService.getAllFriendLinks();
            } else {
                links = friendLinkService.getFriendLinksByCategory(FriendCategory.valueOf(category.toUpperCase()));
            }
            logger.info("成功获取分类{}友链，共{}条", category, links.size());
            return ResponseEntity.ok(ApiResponse.success(links));
        } catch (Exception e) {
            logger.error("获取分类{}友链失败", category, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取友链失败: " + e.getMessage()));
        }
    }
    
    @PreAuthorize("permitAll()")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllFriendLinksAdmin() {
        logger.info("收到获取所有友链的请求（管理员）");
        try {
            List<FriendLink> allLinks = friendLinkService.getAllFriendLinks();
            logger.info("成功获取所有友链，共{}条", allLinks.size());
            return ResponseEntity.ok(ApiResponse.success(allLinks));
        } catch (Exception e) {
            logger.error("获取所有友链失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取友链失败: " + e.getMessage()));
        }
    }
    

    
    @PostMapping
    public ResponseEntity<ApiResponse<?>> addFriendLink(@RequestBody FriendLink friendLink) {
        FriendLink savedLink = friendLinkService.saveFriendLink(friendLink);
        return ResponseEntity.ok(ApiResponse.success(savedLink));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateFriendLink(
            @PathVariable Long id, 
            @RequestBody FriendLink friendLink) {
        FriendLink updatedLink = friendLinkService.updateFriendLink(id, friendLink);
        if (updatedLink != null) {
            return ResponseEntity.ok(ApiResponse.success(updatedLink));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("友链不存在"));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteFriendLink(@PathVariable Long id) {
        friendLinkService.deleteFriendLink(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    

} 