package com.example.blog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 安全验证工具类
 * 用于统一验证所有用户输入，防止SQL注入、XSS等攻击
 */
@Component
public class SecurityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityValidator.class);
    
    // SQL注入检测模式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|UNION|EXEC|EXECUTE|SCRIPT|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|<script|<iframe|<object|<embed|data:text/html|data:application/javascript)",
        Pattern.CASE_INSENSITIVE
    );
    
    // XSS攻击检测模式
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|<iframe|<object|<embed|data:text/html|data:application/javascript)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 恶意URL检测模式
    private static final Pattern MALICIOUS_URL_PATTERN = Pattern.compile(
        "(?i)(javascript:|vbscript:|data:text/html|data:application/javascript|file://)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 验证输入是否安全
     * @param input 输入字符串
     * @param fieldName 字段名称（用于日志记录）
     * @return 是否安全
     */
    public static boolean isValidInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return true; // 空值允许
        }
        
        String trimmedInput = input.trim();
        
        // 检查SQL注入
        if (SQL_INJECTION_PATTERN.matcher(trimmedInput).find()) {
            logger.warn("检测到SQL注入攻击 - 字段: {}, 内容: {}", fieldName, trimmedInput);
            return false;
        }
        
        // 检查XSS攻击
        if (XSS_PATTERN.matcher(trimmedInput).find()) {
            logger.warn("检测到XSS攻击 - 字段: {}, 内容: {}", fieldName, trimmedInput);
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证URL是否安全
     * @param url URL字符串
     * @param fieldName 字段名称
     * @return 是否安全
     */
    public static boolean isValidUrl(String url, String fieldName) {
        if (url == null || url.trim().isEmpty()) {
            return true; // 空值允许
        }
        
        String trimmedUrl = url.trim();
        
        // 检查恶意URL
        if (MALICIOUS_URL_PATTERN.matcher(trimmedUrl).find()) {
            logger.warn("检测到恶意URL - 字段: {}, URL: {}", fieldName, trimmedUrl);
            return false;
        }
        
        // 检查SQL注入
        if (SQL_INJECTION_PATTERN.matcher(trimmedUrl).find()) {
            logger.warn("检测到SQL注入攻击 - 字段: {}, URL: {}", fieldName, trimmedUrl);
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.trim().matches(emailRegex);
    }
    
    /**
     * 清理输入字符串，移除潜在的危险字符
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // 移除HTML标签
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // 移除JavaScript代码
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)vbscript:", "");
        
        // 移除事件处理器
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=", "");
        
        return sanitized.trim();
    }
    
    /**
     * 验证JSON字符串是否安全
     * @param jsonString JSON字符串
     * @return 是否安全
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return true;
        }
        
        return isValidInput(jsonString, "JSON");
    }
} 