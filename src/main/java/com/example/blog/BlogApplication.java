package com.example.blog;

import com.example.blog.service.RankUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableAsync
@EnableMethodSecurity
@EnableScheduling
public class BlogApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(BlogApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
    
    /**
     * 应用启动后执行用户排名更新
     */
    @Bean
    public CommandLineRunner initializeUserRanks(RankUpdateService rankUpdateService) {
        return args -> {
            try {
                // 系统启动时更新用户排名
                logger.info("正在初始化用户排名...");
                rankUpdateService.manualUpdateUserRanks();
                logger.info("用户排名初始化完成");
            } catch (Exception e) {
                // 捕获异常但不影响应用启动
                logger.error("用户排名初始化失败，请手动执行更新: {}", e.getMessage());
            }
        };
    }
}