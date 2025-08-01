package com.example.blog.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // 记录请求URL和方法
        logger.info("=== 收到HTTP请求 ===");
        logger.info("URL: {} {}", httpRequest.getMethod(), httpRequest.getRequestURL());
        logger.info("Content-Type: {}", httpRequest.getContentType());
        
        // 记录请求头
        httpRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            logger.info("Header {}: {}", headerName, httpRequest.getHeader(headerName));
        });
        
        // 记录请求体
        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) || "PUT".equalsIgnoreCase(httpRequest.getMethod())) {
            // 读取JSON格式的请求体
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpRequest.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
                
                if (requestBody.length() > 0) {
                    logger.info("请求体内容: {}", requestBody.toString());
                } else {
                    logger.info("请求体为空");
                }
            } catch (Exception e) {
                logger.error("读取请求体失败: {}", e.getMessage());
            }
        }
        
        logger.info("=== 请求结束 ===");
        
        chain.doFilter(request, response);
    }
}