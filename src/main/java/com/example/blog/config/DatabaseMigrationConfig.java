package com.example.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 数据库迁移配置
 */
@Configuration
public class DatabaseMigrationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Bean
    public CommandLineRunner migrateLicenseFields() {
        return args -> {
            logger.info("开始迁移文章许可证字段...");
            try {
                // 将字符串值转换为枚举名称
                int updated1 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_NC_SA_4_0' WHERE license = 'CC BY-NC-SA 4.0'");
                int updated2 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_NC_4_0' WHERE license = 'CC BY-NC 4.0'");
                int updated3 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_4_0' WHERE license = 'CC BY 4.0'");
                int updated4 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_SA_4_0' WHERE license = 'CC BY-SA 4.0'");
                int updated5 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_ND_4_0' WHERE license = 'CC BY-ND 4.0'");
                int updated6 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_NC_ND_4_0' WHERE license = 'CC BY-NC-ND 4.0'");
                int updated7 = jdbcTemplate.update("UPDATE articles SET license = 'CC0_1_0' WHERE license = 'CC0 1.0'");
                int updated8 = jdbcTemplate.update("UPDATE articles SET license = 'MIT' WHERE license = 'MIT'");
                int updated9 = jdbcTemplate.update("UPDATE articles SET license = 'APACHE_2_0' WHERE license = 'Apache 2.0'");
                int updated10 = jdbcTemplate.update("UPDATE articles SET license = 'GPL_3_0' WHERE license = 'GPL 3.0'");
                int updated11 = jdbcTemplate.update("UPDATE articles SET license = 'ALL_RIGHTS_RESERVED' WHERE license = 'All Rights Reserved'");
                
                // 设置默认值
                int updated12 = jdbcTemplate.update("UPDATE articles SET license = 'CC_BY_NC_SA_4_0' WHERE license IS NULL OR license = ''");
                
                int totalUpdated = updated1 + updated2 + updated3 + updated4 + updated5 + updated6 + updated7 + updated8 + updated9 + updated10 + updated11 + updated12;
                logger.info("成功迁移 {} 条文章许可证记录", totalUpdated);
            } catch (Exception e) {
                logger.error("迁移文章许可证字段失败", e);
            }
        };
    }
} 