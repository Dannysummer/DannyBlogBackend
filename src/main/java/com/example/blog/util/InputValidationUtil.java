package com.example.blog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 输入验证工具类
 * 用于在Controller层进行参数验证，防止SQL注入、XSS等攻击
 */
@Component
public class InputValidationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(InputValidationUtil.class);
    
    // SQL注入检测模式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|expression|eval|alert|confirm|prompt|onload|onerror|onclick|onmouseover|onfocus|onblur|onchange|onsubmit|onreset|onselect|onunload|onabort|onbeforeunload|onerror|onhashchange|onmessage|onoffline|ononline|onpagehide|onpageshow|onpopstate|onresize|onstorage|oncontextmenu|oninput|oninvalid|onsearch|onbeforecopy|onbeforecut|onbeforepaste|oncopy|oncut|onpaste|onselectstart|onmouseenter|onmouseleave|onmouseout|onmousewheel|onkeydown|onkeypress|onkeyup|onmousedown|onmousemove|onmouseup|ontouchstart|ontouchmove|ontouchend|ontouchcancel|ongesturestart|ongesturechange|ongestureend|onorientationchange|onreadystatechange)",
        Pattern.CASE_INSENSITIVE
    );
    
    // XSS检测模式
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|vbscript:|expression|eval|alert|confirm|prompt|onload|onerror|onclick|onmouseover|onfocus|onblur|onchange|onsubmit|onreset|onselect|onunload|onabort|onbeforeunload|onerror|onhashchange|onmessage|onoffline|ononline|onpagehide|onpageshow|onpopstate|onresize|onstorage|oncontextmenu|oninput|oninvalid|onsearch|onbeforecopy|onbeforecut|onbeforepaste|oncopy|oncut|onpaste|onselectstart|onmouseenter|onmouseleave|onmouseout|onmousewheel|onkeydown|onkeypress|onkeyup|onmousedown|onmousemove|onmouseup|ontouchstart|ontouchmove|ontouchend|ontouchcancel|ongesturestart|ongesturechange|ongestureend|onorientationchange|onreadystatechange)",
        Pattern.CASE_INSENSITIVE
    );
    

    
    /**
     * 验证输入是否安全
     * @param input 输入字符串
     * @return 是否安全
     */
    public static boolean isInputSafe(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        
        // 检查SQL注入
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            logger.warn("检测到SQL注入尝试: {}", input);
            return false;
        }
        
        // 检查XSS攻击
        if (XSS_PATTERN.matcher(input).find()) {
            logger.warn("检测到XSS攻击尝试: {}", input);
            return false;
        }
        

        
        return true;
    }
    
    /**
     * 清理输入字符串
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // 移除HTML标签
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // 转义特殊字符
        sanitized = sanitized.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#x27;");
        
        return sanitized;
    }
    
    /**
     * 验证用户名
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // 用户名长度限制
        if (username.length() < 3 || username.length() > 20) {
            return false;
        }
        
        // 只允许字母、数字、下划线
        return username.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * 验证邮箱
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // 简单的邮箱格式验证
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    /**
     * 验证URL
     * @param url URL
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 