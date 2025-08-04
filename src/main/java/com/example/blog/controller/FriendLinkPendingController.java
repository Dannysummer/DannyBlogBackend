package com.example.blog.controller;

import com.example.blog.dto.ApiResponse;
import com.example.blog.entity.FriendLink;
import com.example.blog.entity.FriendLinkPending;
import com.example.blog.service.FriendLinkPendingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;
import com.example.blog.util.SecurityValidator;

@RestController
@RequestMapping("/api/friend-links")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FriendLinkPendingController {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendLinkPendingController.class);
    

    
    @Autowired
    private FriendLinkPendingService friendLinkPendingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @PostMapping("/pending/apply")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<?>> applyFriendLink(@RequestBody String rawJson) {
        logger.info("=== 原始JSON字符串 ===");
        logger.info("原始请求体: {}", rawJson);
        
        try {
            // 首先进行安全验证
            if (!SecurityValidator.isValidJson(rawJson)) {
                logger.warn("检测到恶意内容，拒绝友链申请: {}", rawJson);
                return ResponseEntity.badRequest().body(ApiResponse.error("输入内容包含潜在的安全风险，请检查后重试"));
            }
            
            // 手动解析JSON
            FriendLinkPending friendLinkPending = objectMapper.readValue(rawJson, FriendLinkPending.class);
            logger.info("解析后的对象: {}", friendLinkPending);
            
            // 对每个字段进行安全验证
            if (!SecurityValidator.isValidInput(friendLinkPending.getName(), "博客名称")) {
                logger.warn("友链申请验证失败: 博客名称包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("博客名称包含潜在的安全风险"));
            }
            if (!SecurityValidator.isValidUrl(friendLinkPending.getUrl(), "博客地址")) {
                logger.warn("友链申请验证失败: 博客地址包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("博客地址包含潜在的安全风险"));
            }
            if (!SecurityValidator.isValidInput(friendLinkPending.getDescription(), "博客描述")) {
                logger.warn("友链申请验证失败: 博客描述包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("博客描述包含潜在的安全风险"));
            }
            if (!SecurityValidator.isValidInput(friendLinkPending.getPendingEmail(), "联系邮箱")) {
                logger.warn("友链申请验证失败: 联系邮箱包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("联系邮箱包含潜在的安全风险"));
            }
            if (friendLinkPending.getCategory() != null && !SecurityValidator.isValidInput(friendLinkPending.getCategory().name(), "分类")) {
                logger.warn("友链申请验证失败: 分类包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("分类包含潜在的安全风险"));
            }
            if (!SecurityValidator.isValidUrl(friendLinkPending.getAvatar(), "头像地址")) {
                logger.warn("友链申请验证失败: 头像地址包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("头像地址包含潜在的安全风险"));
            }
            if (!SecurityValidator.isValidUrl(friendLinkPending.getCover(), "封面地址")) {
                logger.warn("友链申请验证失败: 封面地址包含恶意内容");
                return ResponseEntity.badRequest().body(ApiResponse.error("封面地址包含潜在的安全风险"));
            }
            
            // 打印原始请求数据
            logger.info("=== 原始请求数据开始 ===");
            logger.info("友链申请原始JSON数据: {}", objectMapper.writeValueAsString(friendLinkPending));
            logger.info("原始name值: '{}' (类型: {})", friendLinkPending.getName(), friendLinkPending.getName() != null ? friendLinkPending.getName().getClass().getName() : "null");
            logger.info("原始url值: '{}' (类型: {})", friendLinkPending.getUrl(), friendLinkPending.getUrl() != null ? friendLinkPending.getUrl().getClass().getName() : "null");
            logger.info("原始description值: '{}' (类型: {})", friendLinkPending.getDescription(), friendLinkPending.getDescription() != null ? friendLinkPending.getDescription().getClass().getName() : "null");
            logger.info("原始email值: '{}' (类型: {})", friendLinkPending.getPendingEmail(), friendLinkPending.getPendingEmail() != null ? friendLinkPending.getPendingEmail().getClass().getName() : "null");
            logger.info("原始category值: '{}' (类型: {})", friendLinkPending.getCategory(), friendLinkPending.getCategory() != null ? friendLinkPending.getCategory().getClass().getName() : "null");
            logger.info("原始avatar值: '{}' (类型: {})", friendLinkPending.getAvatar(), friendLinkPending.getAvatar() != null ? friendLinkPending.getAvatar().getClass().getName() : "null");
            logger.info("=== 原始请求数据结束 ===");
            
            logger.info("收到友链申请请求: name={}, url={}, category={}, email={}", 
                friendLinkPending.getName(), 
                friendLinkPending.getUrl(),
                friendLinkPending.getCategory(),
                friendLinkPending.getPendingEmail());
            
            if (friendLinkPending.getName() == null || friendLinkPending.getName().trim().isEmpty()) {
                logger.warn("友链申请验证失败: 博客名称为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("博客名称不能为空"));
            }
            if (friendLinkPending.getUrl() == null || friendLinkPending.getUrl().trim().isEmpty()) {
                logger.warn("友链申请验证失败: 博客地址为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("博客地址不能为空"));
            }
            if (friendLinkPending.getDescription() == null || friendLinkPending.getDescription().trim().isEmpty()) {
                logger.warn("友链申请验证失败: 博客描述为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("博客描述不能为空"));
            }
            if (friendLinkPending.getPendingEmail() == null || friendLinkPending.getPendingEmail().trim().isEmpty()) {
                logger.warn("友链申请验证失败: 联系邮箱为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("联系邮箱不能为空"));
            }
            
            // 验证邮箱格式
            if (!SecurityValidator.isValidEmail(friendLinkPending.getPendingEmail())) {
                logger.warn("友链申请验证失败: 邮箱格式无效");
                return ResponseEntity.badRequest().body(ApiResponse.error("请输入有效的邮箱地址"));
            }
            
            // 检查是否有登录用户（可选）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = null;
            if (authentication != null && !authentication.getName().equals("anonymousUser")) {
                username = authentication.getName();
                logger.info("登录用户 {} 提交友链申请", username);
            } else {
                logger.info("匿名用户提交友链申请");
            }
            
            logger.info("友链申请基本验证通过，准备保存");
            // 状态在 service 层会自动设置为 PENDING
            FriendLinkPending saved = friendLinkPendingService.saveFriendLinkPending(friendLinkPending);
            logger.info("友链申请保存成功: id={}", saved.getId());
            return ResponseEntity.ok(ApiResponse.success(saved));
        } catch (IllegalArgumentException e) {
            logger.warn("友链申请验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("友链申请保存失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("申请失败：" + e.getMessage()));
        }
    }
    


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<?>> getAllPendingFriendLinks() {
        try {
            logger.info("获取所有待审核友链");
            List<FriendLinkPending> pendingLinks = friendLinkPendingService.getAllPendingFriendLinks();
            logger.info("成功获取所有待审核友链，共{}条", pendingLinks.size());
            return ResponseEntity.ok(ApiResponse.success(pendingLinks));
        } catch (Exception e) {
            logger.error("获取待审核友链失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取待审核友链失败：" + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/pending/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approveFriendLink(@PathVariable Long id, @RequestBody String rawJson) {
        try {
            logger.info("审核通过友链申请：id={}", id);
            logger.info("收到的请求体：{}", rawJson);
            
            // 解析请求体，获取category等信息
            FriendLinkPending updateData = objectMapper.readValue(rawJson, FriendLinkPending.class);
            logger.info("解析后的category：{}", updateData.getCategory());
            
            FriendLink approvedLink = friendLinkPendingService.approveFriendLink(id, updateData);
            logger.info("友链申请审核通过：id={}", id);
            return ResponseEntity.ok(ApiResponse.success(approvedLink));
        } catch (Exception e) {
            logger.error("审核通过友链失败：id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("审核失败：" + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/pending/{id}/reject")
    public ResponseEntity<ApiResponse<?>> rejectFriendLink(@PathVariable Long id) {
        try {
            logger.info("拒绝友链申请：id={}", id);
            friendLinkPendingService.rejectFriendLink(id);
            logger.info("友链申请已拒绝：id={}", id);
            return ResponseEntity.ok(ApiResponse.success("友链申请已拒绝"));
        } catch (Exception e) {
            logger.error("拒绝友链失败：id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("拒绝失败：" + e.getMessage()));
        }
    }
}