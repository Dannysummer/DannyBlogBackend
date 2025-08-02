package com.example.blog.service;

import com.example.blog.entity.FriendLink;
import com.example.blog.entity.FriendCategory;
import com.example.blog.repository.FriendLinkRepository;
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
} 