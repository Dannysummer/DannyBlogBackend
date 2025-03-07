package com.example.blog.service;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;

@Service
public class SensitiveWordService {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordService.class);
    
    private SensitiveWordBs sensitiveWordBs;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("${app.sensitive-word.file-path:classpath:sensitive-words.txt}")
    private String sensitiveWordFilePath;
    
    @Value("${app.sensitive-word.enabled:true}")
    private boolean enabled;
    
    @PostConstruct
    public void init() {
        try {
            // 加载自定义敏感词
            Set<String> customWords = loadCustomWords();
            
            // 初始化敏感词过滤器
            sensitiveWordBs = SensitiveWordBs.newInstance();
            sensitiveWordBs.init();
            
            logger.info("敏感词过滤服务初始化完成");
        } catch (Exception e) {
            logger.error("初始化敏感词过滤服务失败", e);
        }
    }
    
    /**
     * 加载自定义敏感词库
     */
    private Set<String> loadCustomWords() {
        Set<String> words = new HashSet<>();
        try {
            Resource resource = resourceLoader.getResource(sensitiveWordFilePath);
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            words.add(line);
                        }
                    }
                }
                
                if (!words.isEmpty()) {
                    logger.info("已加载自定义敏感词 {} 个", words.size());
                }
            } else {
                logger.info("未找到敏感词文件，使用默认词库");
            }
        } catch (Exception e) {
            logger.warn("加载敏感词文件失败，使用默认词库: {}", e.getMessage());
        }
        return words;
    }
    
    /**
     * 检查文本是否包含敏感词
     * @param text 待检查文本
     * @return 是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return false;
        }
        return sensitiveWordBs.contains(text);
    }
    
    /**
     * 获取文本中的所有敏感词
     * @param text 待检查文本
     * @return 敏感词列表
     */
    public List<String> findSensitiveWords(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        return sensitiveWordBs.findAll(text);
    }
    
    /**
     * 替换敏感词
     * @param text 待过滤文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }
        return sensitiveWordBs.replace(text);
    }
} 