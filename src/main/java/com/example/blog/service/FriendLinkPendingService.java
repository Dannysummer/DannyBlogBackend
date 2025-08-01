package com.example.blog.service;

import com.example.blog.entity.FriendLink;
import com.example.blog.entity.FriendLinkPending;
import com.example.blog.entity.FriendLinkPendingStatus;
import com.example.blog.repository.FriendLinkPendingRepository;
import com.example.blog.repository.FriendLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendLinkPendingService {
    
    @Autowired
    private FriendLinkPendingRepository friendLinkPendingRepository;
    
    @Autowired
    private FriendLinkRepository friendLinkRepository;
    
    public FriendLinkPending saveFriendLinkPending(FriendLinkPending friendLinkPending) {
        // 检查URL是否已经在正式友链中
        if (friendLinkRepository.existsByUrl(friendLinkPending.getUrl())) {
            throw new IllegalArgumentException("该网站已经是我的友链了哦");
        }
        
        // 检查URL是否已经在待审核友链中
        if (friendLinkPendingRepository.existsByUrl(friendLinkPending.getUrl())) {
            throw new IllegalArgumentException("该网站已经提交过申请，正在审核中");
        }
        
        // 验证URL格式
        if (!isValidUrl(friendLinkPending.getUrl())) {
            throw new IllegalArgumentException("请输入有效的网站地址");
        }
        
        // 设置初始状态为待审核
        friendLinkPending.setStatus(FriendLinkPendingStatus.PENDING);
        return friendLinkPendingRepository.save(friendLinkPending);
    }
    
    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url).toURI();
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    public List<FriendLinkPending> getAllPendingFriendLinks() {
        return friendLinkPendingRepository.findAll();
    }

    @Transactional
    public FriendLink approveFriendLink(Long id) {
        FriendLinkPending pendingLink = friendLinkPendingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("找不到该友链申请"));

        if (pendingLink.getStatus() != FriendLinkPendingStatus.PENDING) {
            throw new IllegalArgumentException("该友链申请已经被处理过了");
        }

        // 创建新的FriendLink对象
        FriendLink friendLink = new FriendLink();
        friendLink.setName(pendingLink.getName());
        friendLink.setUrl(pendingLink.getUrl());
        friendLink.setDescription(pendingLink.getDescription());
        friendLink.setAvatar(pendingLink.getAvatar());
        friendLink.setCover(pendingLink.getCover());
        friendLink.setDelay(pendingLink.getDelay());
        friendLink.setCategory(pendingLink.getCategory());

        // 保存到正式友链表
        FriendLink savedLink = friendLinkRepository.save(friendLink);

        // 审核通过后，直接从待审核表中删除记录
        friendLinkPendingRepository.deleteById(id);

        return savedLink;
    }

    @Transactional
    public void rejectFriendLink(Long id) {
        FriendLinkPending pendingLink = friendLinkPendingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("找不到该友链申请"));

        if (pendingLink.getStatus() != FriendLinkPendingStatus.PENDING) {
            throw new IllegalArgumentException("该友链申请已经被处理过了");
        }

        // 拒绝后也删除记录，保持表的整洁
        friendLinkPendingRepository.deleteById(id);
    }
} 