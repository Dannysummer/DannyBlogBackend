package com.example.blog.service;

import com.example.blog.entity.FriendLink;
import com.example.blog.entity.FriendLinkPending;
import com.example.blog.entity.FriendCategory;
import com.example.blog.entity.FriendLinkPendingStatus;
import com.example.blog.repository.FriendLinkRepository;
import com.example.blog.repository.FriendLinkPendingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendLinkService {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendLinkService.class);
    
    @Autowired
    private FriendLinkRepository friendLinkRepository;
    
    @Autowired
    private FriendLinkPendingRepository friendLinkPendingRepository;
    
    @Autowired
    private EmailService emailService;
    
    public List<FriendLink> getAllFriendLinks() {
        logger.info("从数据库获取所有友链");
        try {
            List<FriendLink> links = friendLinkRepository.findAll();
            logger.info("成功获取所有友链，共{}条", links.size());
            return links;
        } catch (Exception e) {
            logger.error("获取所有友链失败", e);
            throw e;
        }
    }
    
    public List<FriendLink> getFriendLinksByCategory(FriendCategory category) {
        return friendLinkRepository.findByCategory(category);
    }
    
    public FriendLink saveFriendLink(FriendLink friendLink) {
        return friendLinkRepository.save(friendLink);
    }
    
    public FriendLink updateFriendLink(Long id, FriendLink friendLink) {
        if (friendLinkRepository.existsById(id)) {
            friendLink.setId(id);
            return friendLinkRepository.save(friendLink);
        }
        return null;
    }
    
    public void deleteFriendLink(Long id) {
        friendLinkRepository.deleteById(id);
    }
    
    public List<FriendLinkPending> getPendingFriendLinks() {
        logger.info("获取所有待审核友链");
        return friendLinkPendingRepository.findByStatus(FriendLinkPendingStatus.PENDING);
    }
    
    @Transactional
    public void approveFriendLink(Long id) {
        FriendLinkPending pending = friendLinkPendingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("待审核友链不存在"));
        
        // 创建新的已审核友链
        FriendLink approvedLink = new FriendLink();
        approvedLink.setAvatar(pending.getAvatar());
        approvedLink.setCover(pending.getCover());
        approvedLink.setName(pending.getName());
        approvedLink.setDescription(pending.getDescription());
        approvedLink.setUrl(pending.getUrl());
        approvedLink.setDelay(pending.getDelay());
        approvedLink.setCategory(pending.getCategory());
        
        // 保存到已审核友链表
        friendLinkRepository.save(approvedLink);
        
        // 更新待审核友链状态为已通过
        pending.setStatus(FriendLinkPendingStatus.PASSED);
        friendLinkPendingRepository.save(pending);
        
        // 发送通过通知邮件
        try {
            String emailContent = String.format(
                "尊敬的大人：\n  恭喜您！您在Danny's Blog中的友链申请已通过领主大人审核喽！\n" +
                "  您的博客名称：%s\n" +
                "  您的博客地址：%s\n" +
                "  代表领主大人以及全体领地居民欢迎您的加入，以后就是一家人啦！",
                pending.getName(),
                pending.getUrl()
            );
            emailService.sendEmail(pending.getPendingEmail(), "友链申请通过通知", emailContent);
        } catch (Exception e) {
            logger.error("发送友链通过通知邮件失败", e);
        }
        
        logger.info("友链审核通过：id={}, name={}, email={}", id, pending.getName(), pending.getPendingEmail());
    }
    
    @Transactional
    public void rejectFriendLink(Long id, String reason) {
        FriendLinkPending pending = friendLinkPendingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("待审核友链不存在"));
            
        // 更新状态为已拒绝
        pending.setStatus(FriendLinkPendingStatus.DENIED);
        friendLinkPendingRepository.save(pending);
        
        // 发送拒绝通知邮件
        try {
            String emailContent = String.format(
                "尊敬的大人，抱歉啦！\n您在Danny's Blog中的友链申请未能通过领主大人审核呢\n" +
                "您的博客名称：%s\n" +
                "您的博客地址：%s\n" +
                "领主大人的婉拒理由是：%s\n" +
                "您有什么疑问的话，还劳烦大人您重新提交申请丫。",
                pending.getName(),
                pending.getUrl(),
                reason
            );
            emailService.sendEmail(pending.getPendingEmail(), "友链申请未通过通知", emailContent);
        } catch (Exception e) {
            logger.error("发送友链拒绝通知邮件失败", e);
        }
        
        logger.info("友链审核拒绝：id={}, name={}, email={}", id, pending.getName(), pending.getPendingEmail());
    }
    
    public FriendLinkPending savePendingFriendLink(FriendLinkPending friendLinkPending) {
        logger.info("保存新的友链申请：{}", friendLinkPending.getName());
        
        // 检查URL是否已经在正式友链中
        if (friendLinkRepository.existsByUrl(friendLinkPending.getUrl())) {
            throw new IllegalArgumentException("该网站已经是我的友链了哦");
        }
        
        // 检查URL是否已经在待审核友链中
        if (friendLinkPendingRepository.existsByUrl(friendLinkPending.getUrl())) {
            throw new IllegalArgumentException("该网站已经提交过申请，请耐心等待审核");
        }
        
        // 确保状态为 PENDING
        friendLinkPending.setStatus(FriendLinkPendingStatus.PENDING);
        return friendLinkPendingRepository.save(friendLinkPending);
    }
} 