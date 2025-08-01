package com.example.blog.service;

import com.example.blog.entity.User;
import com.example.blog.enums.UserStatus;
import com.example.blog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户排名更新服务
 */
@Service
public class RankUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(RankUpdateService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * 每天凌晨3点执行用户排名更新
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void updateUserRanks() {
        try {
            logger.info("开始执行用户排名更新任务");
            
            // 获取所有正常状态的用户，按创建时间排序
            List<User> users = userRepository.findByStatusOrderByCreateTimeAsc(UserStatus.NORMAL);
            
            if (users == null || users.isEmpty()) {
                logger.info("没有找到需要更新排名的用户");
                return;
            }
            
            // 重新计算排名
            AtomicLong rank = new AtomicLong(1);
            users.forEach(user -> {
                try {
                    user.setRank(rank.getAndIncrement());
                    userRepository.save(user);
                } catch (Exception e) {
                    logger.error("更新用户[id={}]的排名失败: {}", user.getId(), e.getMessage());
                }
            });
            
            logger.info("用户排名更新完成，共更新{}个用户", users.size());
        } catch (Exception e) {
            logger.error("用户排名更新失败: {}", e.getMessage());
            throw e; // 重新抛出异常，让事务回滚
        }
    }
    
    /**
     * 手动触发用户排名更新
     */
    @Transactional
    public void manualUpdateUserRanks() {
        try {
            updateUserRanks();
        } catch (Exception e) {
            logger.error("手动更新用户排名失败: {}", e.getMessage());
            // 手动调用时不重新抛出异常，避免影响调用方
        }
    }
} 