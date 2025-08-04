package com.example.blog.filter;

import com.example.blog.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL注入防护过滤器
 * 基于正则表达式检测常见的SQL注入模式
 */
@Component
@Order(1) // 确保在其他过滤器之前执行
public class SqlInjectionFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlInjectionFilter.class);
    
    // SQL注入检测模式 - 更精确的版本，避免误判正常请求
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        // 明显的SQL注入攻击模式
        Pattern.compile("(?i)(union\\s+select|select\\s+from|insert\\s+into|update\\s+set|delete\\s+from|drop\\s+table|create\\s+table|alter\\s+table)", Pattern.CASE_INSENSITIVE),
        
        // SQL注释攻击
        Pattern.compile("(?i)(--\\s*$|#\\s*$|/\\*.*\\*/)", Pattern.CASE_INSENSITIVE),
        
        // 危险SQL函数
        Pattern.compile("(?i)(sleep\\s*\\(|benchmark\\s*\\()", Pattern.CASE_INSENSITIVE),
        
        // 布尔注入攻击
        Pattern.compile("(?i)(and\\s+\\d+\\s*=\\s*\\d+\\s*--|or\\s+\\d+\\s*=\\s*\\d+\\s*--)", Pattern.CASE_INSENSITIVE),
        
        // 堆叠查询攻击
        Pattern.compile("(?i)(;\\s*(select|insert|update|delete|drop|create|alter))", Pattern.CASE_INSENSITIVE),
        
        // 编码绕过攻击
        Pattern.compile("(?i)(%27\\s+or\\s+%271%27=%271|%22\\s+or\\s+%221%22=%221)", Pattern.CASE_INSENSITIVE),
        
        // 双写绕过攻击
        Pattern.compile("(?i)(sselect|iinsert|uupdate|ddelete|ddrop|ccreate|aalter)", Pattern.CASE_INSENSITIVE),
        
        // 十六进制绕过攻击
        Pattern.compile("(?i)(0x73656c656374|0x696e73657274|0x757064617465|0x64656c657465)", Pattern.CASE_INSENSITIVE),
        
        // URL编码绕过攻击
        Pattern.compile("(?i)(%73%65%6c%65%63%74|%69%6e%73%65%72%74|%75%70%64%61%74%65|%64%65%6c%65%74%65)", Pattern.CASE_INSENSITIVE),
        
        // HTML实体编码绕过攻击
        Pattern.compile("(?i)(&#115;&#101;&#108;&#101;&#99;&#116;|&#105;&#110;&#115;&#101;&#114;&#116;|&#117;&#112;&#100;&#97;&#116;&#101;|&#100;&#101;&#108;&#101;&#116;&#101;)", Pattern.CASE_INSENSITIVE),
        
        // Unicode编码绕过攻击
        Pattern.compile("(?i)(\\u0073\\u0065\\u006c\\u0065\\u0063\\u0074|\\u0069\\u006e\\u0073\\u0065\\u0072\\u0074|\\u0075\\u0070\\u0064\\u0061\\u0074\\u0065|\\u0064\\u0065\\u006c\\u0065\\u0074\\u0065)", Pattern.CASE_INSENSITIVE),
        
        // 混合编码绕过攻击
        Pattern.compile("(?i)(s%65l%65ct|i%6es%65rt|u%70d%61te|d%65l%65te)", Pattern.CASE_INSENSITIVE),
        
        // 注释内联绕过攻击
        Pattern.compile("(?i)(select/\\*!\\*/|insert/\\*!\\*/|update/\\*!\\*/|delete/\\*!\\*/)", Pattern.CASE_INSENSITIVE),
        
        // 条件注释绕过攻击
        Pattern.compile("(?i)(select/\\*!\\d+\\*/|insert/\\*!\\d+\\*/|update/\\*!\\d+\\*/|delete/\\*!\\d+\\*/)", Pattern.CASE_INSENSITIVE),
        
        // 多行注释绕过攻击
        Pattern.compile("(?i)(select/\\*.*\\*/|insert/\\*.*\\*/|update/\\*.*\\*/|delete/\\*.*\\*/)", Pattern.CASE_INSENSITIVE),
        
        // 单行注释绕过攻击
        Pattern.compile("(?i)(select--|insert--|update--|delete--|drop--|create--|alter--)", Pattern.CASE_INSENSITIVE),
        
        // 井号注释绕过攻击
        Pattern.compile("(?i)(select#|insert#|update#|delete#|drop#|create#|alter#)", Pattern.CASE_INSENSITIVE),
        
        // 分号绕过攻击
        Pattern.compile("(?i)(;\\s*select|;\\s*insert|;\\s*update|;\\s*delete|;\\s*drop|;\\s*create|;\\s*alter)", Pattern.CASE_INSENSITIVE),
        
        // 空格绕过攻击
        Pattern.compile("(?i)(select\\s+from|insert\\s+into|update\\s+set|delete\\s+from|drop\\s+table|create\\s+table|alter\\s+table)", Pattern.CASE_INSENSITIVE),
        
        // 点号绕过攻击
        Pattern.compile("(?i)(select\\.from|insert\\.into|update\\.set|delete\\.from|drop\\.table|create\\.table|alter\\.table)", Pattern.CASE_INSENSITIVE),
        
        // 下划线绕过攻击
        Pattern.compile("(?i)(select_from|insert_into|update_set|delete_from|drop_table|create_table|alter_table)", Pattern.CASE_INSENSITIVE),
        
        // 连字符绕过攻击
        Pattern.compile("(?i)(select-from|insert-into|update-set|delete-from|drop-table|create-table|alter-table)", Pattern.CASE_INSENSITIVE),
        
        // 加号绕过攻击
        Pattern.compile("(?i)(select\\+from|insert\\+into|update\\+set|delete\\+from|drop\\+table|create\\+table|alter\\+table)", Pattern.CASE_INSENSITIVE),
        
        // 等号绕过攻击
        Pattern.compile("(?i)(select=from|insert=into|update=set|delete=from|drop=table|create=table|alter=table)", Pattern.CASE_INSENSITIVE),
        
        // 问号绕过攻击
        Pattern.compile("(?i)(select\\?from|insert\\?into|update\\?set|delete\\?from|drop\\?table|create\\?table|alter\\?table)", Pattern.CASE_INSENSITIVE),
        
        // 感叹号绕过攻击
        Pattern.compile("(?i)(select!from|insert!into|update!set|delete!from|drop!table|create!table|alter!table)", Pattern.CASE_INSENSITIVE),
        
        // 百分号绕过攻击
        Pattern.compile("(?i)(select%from|insert%into|update%set|delete%from|drop%table|create%table|alter%table)", Pattern.CASE_INSENSITIVE),
        
        // 星号绕过攻击
        Pattern.compile("(?i)(select\\*from|insert\\*into|update\\*set|delete\\*from|drop\\*table|create\\*table|alter\\*table)", Pattern.CASE_INSENSITIVE),
        
        // 括号绕过攻击
        Pattern.compile("(?i)(select\\(from|insert\\(into|update\\(set|delete\\(from|drop\\(table|create\\(table|alter\\(table)", Pattern.CASE_INSENSITIVE),
        
        // 方括号绕过攻击
        Pattern.compile("(?i)(select\\[from|insert\\[into|update\\[set|delete\\[from|drop\\[table|create\\[table|alter\\[table)", Pattern.CASE_INSENSITIVE),
        
        // 大括号绕过攻击
        Pattern.compile("(?i)(select\\{from|insert\\{into|update\\{set|delete\\{from|drop\\{table|create\\{table|alter\\{table)", Pattern.CASE_INSENSITIVE),
        
        // 尖括号绕过攻击
        Pattern.compile("(?i)(select<from|insert<into|update<set|delete<from|drop<table|create<table|alter<table)", Pattern.CASE_INSENSITIVE),
        
        // 大于号绕过攻击
        Pattern.compile("(?i)(select>from|insert>into|update>set|delete>from|drop>table|create>table|alter>table)", Pattern.CASE_INSENSITIVE),
        
        // 管道符绕过攻击
        Pattern.compile("(?i)(select\\|from|insert\\|into|update\\|set|delete\\|from|drop\\|table|create\\|table|alter\\|table)", Pattern.CASE_INSENSITIVE),
        
        // 反斜杠绕过攻击
        Pattern.compile("(?i)(select\\\\from|insert\\\\into|update\\\\set|delete\\\\from|drop\\\\table|create\\\\table|alter\\\\table)", Pattern.CASE_INSENSITIVE),
        
        // 正斜杠绕过攻击
        Pattern.compile("(?i)(select/from|insert/into|update/set|delete/from|drop/table|create/table|alter/table)", Pattern.CASE_INSENSITIVE),
        
        // 反引号绕过攻击
        Pattern.compile("(?i)(select`from|insert`into|update`set|delete`from|drop`table|create`table|alter`table)", Pattern.CASE_INSENSITIVE),
        
        // 单引号绕过攻击
        Pattern.compile("(?i)(select'from|insert'into|update'set|delete'from|drop'table|create'table|alter'table)", Pattern.CASE_INSENSITIVE),
        
        // 双引号绕过攻击
        Pattern.compile("(?i)(select\"from|insert\"into|update\"set|delete\"from|drop\"table|create\"table|alter\"table)", Pattern.CASE_INSENSITIVE),
        
        // 波浪号绕过攻击
        Pattern.compile("(?i)(select~from|insert~into|update~set|delete~from|drop~table|create~table|alter~table)", Pattern.CASE_INSENSITIVE),
        
        // 脱字符绕过攻击
        Pattern.compile("(?i)(select\\^from|insert\\^into|update\\^set|delete\\^from|drop\\^table|create\\^table|alter\\^table)", Pattern.CASE_INSENSITIVE),
        
        // 美元符绕过攻击
        Pattern.compile("(?i)(select\\$from|insert\\$into|update\\$set|delete\\$from|drop\\$table|create\\$table|alter\\$table)", Pattern.CASE_INSENSITIVE),
        
        // 与号绕过攻击
        Pattern.compile("(?i)(select&from|insert&into|update&set|delete&from|drop&table|create&table|alter&table)", Pattern.CASE_INSENSITIVE),
        
        // 分号绕过攻击
        Pattern.compile("(?i)(select;from|insert;into|update;set|delete;from|drop;table|create;table|alter;table)", Pattern.CASE_INSENSITIVE),
        
        // 冒号绕过攻击
        Pattern.compile("(?i)(select:from|insert:into|update:set|delete:from|drop:table|create:table|alter:table)", Pattern.CASE_INSENSITIVE),
        
        // 逗号绕过攻击
        Pattern.compile("(?i)(select,from|insert,into|update,set|delete,from|drop,table|create,table|alter,table)", Pattern.CASE_INSENSITIVE),
        
        // 新增：检测单独的SQL关键字（更严格的模式）
        Pattern.compile("(?i)\\b(select|insert|update|delete|drop|create|alter|union|exec|execute|script)\\b", Pattern.CASE_INSENSITIVE),
        
        // 新增：检测SQL注入的常见组合
        Pattern.compile("(?i)(select.*from|insert.*into|update.*set|delete.*from)", Pattern.CASE_INSENSITIVE),
        
        // 新增：检测XSS和SQL注入的组合攻击
        Pattern.compile("(?i)(<script.*>|javascript:|vbscript:|onload=|onerror=)", Pattern.CASE_INSENSITIVE)
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 获取请求信息
        String requestURI = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String method = httpRequest.getMethod();
        
        logger.debug("SQL注入过滤器检查请求: {} {} {}", method, requestURI, queryString);
        
        // 检查URL参数
        if (queryString != null && containsSqlInjection(queryString)) {
            logger.warn("检测到SQL注入攻击 - URL参数: {}", queryString);
            blockRequest(httpResponse, "URL参数包含潜在的安全风险");
            return;
        }
        
        // 检查请求头
        if (containsSqlInjectionInHeaders(httpRequest)) {
            logger.warn("检测到SQL注入攻击 - 请求头");
            blockRequest(httpResponse, "请求头包含潜在的安全风险");
            return;
        }
        
        // 对于POST/PUT请求，检查请求体
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            // 检查Content-Type
            String contentType = httpRequest.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // 创建可重复读取的请求包装器
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
                
                // 读取请求体进行检查
                String requestBody = StreamUtils.copyToString(cachedRequest.getInputStream(), StandardCharsets.UTF_8);
                
                if (requestBody != null && !requestBody.trim().isEmpty()) {
                    logger.debug("检查POST请求体: {}", requestBody);
                    
                    if (containsSqlInjection(requestBody)) {
                        logger.warn("检测到SQL注入攻击 - 请求体: {}", requestBody);
                        blockRequest(httpResponse, "请求体包含潜在的安全风险");
                        return;
                    }
                }
                
                // 使用包装后的请求继续过滤器链
                chain.doFilter(cachedRequest, response);
                return;
            }
        }
        
        // 继续过滤器链
        chain.doFilter(request, response);
    }
    
    /**
     * 检查字符串是否包含SQL注入模式
     */
    private boolean containsSqlInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // URL解码
        String decodedInput;
        try {
            decodedInput = java.net.URLDecoder.decode(input, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decodedInput = input;
        }
        
        // 检查每个模式
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(decodedInput).find()) {
                logger.warn("SQL注入检测匹配模式: {} - 输入: {}", pattern.pattern(), decodedInput);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查请求头是否包含SQL注入模式
     */
    private boolean containsSqlInjectionInHeaders(HttpServletRequest request) {
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            if (headerValue != null && containsSqlInjection(headerValue)) {
                logger.warn("检测到SQL注入攻击 - 请求头: {} = {}", headerName, headerValue);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 阻止请求并返回错误响应
     */
    private void blockRequest(HttpServletResponse response, String message) throws IOException {
        // 设置CORS头，确保前端能收到响应
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        
        // 使用200状态码而不是403，让前端更容易处理
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 创建更友好的错误响应
        ApiResponse<?> apiResponse = ApiResponse.error(
            "安全防护：检测到潜在的恶意请求。您的请求包含可能的安全风险，已被系统自动拦截。请检查输入内容并重试。"
        );
        
        new ObjectMapper().writeValue(response.getOutputStream(), apiResponse);
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("SQL注入防护过滤器初始化完成");
    }
    
    @Override
    public void destroy() {
        logger.info("SQL注入防护过滤器销毁");
    }
    
    /**
     * 可重复读取的HTTP请求包装器
     */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private byte[] cachedBody;
        
        public CachedBodyHttpServletRequest(jakarta.servlet.http.HttpServletRequest request) throws IOException {
            super(request);
            // 缓存请求体
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }
        
        @Override
        public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
            return new jakarta.servlet.ServletInputStream() {
                private final java.io.ByteArrayInputStream byteArrayInputStream = new java.io.ByteArrayInputStream(cachedBody);
                
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }
                
                @Override
                public boolean isReady() {
                    return true;
                }
                
                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    // 不需要实现
                }
                
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        }
        
        @Override
        public java.io.BufferedReader getReader() throws IOException {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream()));
        }
    }
} 