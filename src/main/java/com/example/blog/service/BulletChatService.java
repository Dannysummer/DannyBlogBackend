package com.example.blog.service;

import com.example.blog.entity.BulletChat;
import com.example.blog.repository.BulletChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
// import java.time.LocalDateTime;
import java.util.List;

@Service
public class BulletChatService {
    private static final Logger logger = LoggerFactory.getLogger(BulletChatService.class);
    
    @Autowired
    private BulletChatRepository bulletChatRepository;
    
    @Autowired
    private SensitiveWordService sensitiveWordService;
    
    public List<BulletChat> getAllBulletChats() {
        return bulletChatRepository.findByOrderByCreateTimeDesc();
    }
    
    public List<BulletChat> getBulletChatsByStatus(String status) {
        return bulletChatRepository.findByStatus(status);
    }
    
    public BulletChat getBulletChatById(Integer id) {
        return bulletChatRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("弹幕不存在"));
    }
    
    public BulletChat createBulletChat(BulletChat bulletChat) {
        // 敏感词过滤
        String filteredContent = sensitiveWordService.filter(bulletChat.getContent());
        
        // 检查是否包含敏感词
        boolean containsSensitiveWord = !filteredContent.equals(bulletChat.getContent());
        if (containsSensitiveWord) {
            logger.warn("弹幕内容包含敏感词，原内容：{}，过滤后：{}", bulletChat.getContent(), filteredContent);
        }
        
        // 设置过滤后的内容
        bulletChat.setContent(filteredContent);
        
        // 由@PrePersist自动设置createTime和status
        logger.info("正在保存新弹幕: {}", bulletChat.getContent());
        return bulletChatRepository.save(bulletChat);
    }
    
    public BulletChat updateBulletChat(Integer id, BulletChat bulletChat) {
        BulletChat existingBulletChat = getBulletChatById(id);
        
        if (bulletChat.getContent() != null) {
            // 敏感词过滤
            String filteredContent = sensitiveWordService.filter(bulletChat.getContent());
            existingBulletChat.setContent(filteredContent);
        }
        if (bulletChat.getAvatar() != null) {
            existingBulletChat.setAvatar(bulletChat.getAvatar());
        }
        if (bulletChat.getStatus() != null) {
            existingBulletChat.setStatus(bulletChat.getStatus());
        }
        
        return bulletChatRepository.save(existingBulletChat);
    }
    
    public void deleteBulletChat(Integer id) {
        bulletChatRepository.deleteById(id);
    }
} 