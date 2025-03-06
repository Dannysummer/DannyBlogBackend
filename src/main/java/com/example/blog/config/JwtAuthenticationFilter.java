package com.example.blog.config;

import com.example.blog.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtUtil jwtUtil;
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从 Cookie 中获取 token
            String token = Optional.ofNullable(request.getCookies())
                    .flatMap(cookies -> Arrays.stream(cookies)
                            .filter(cookie -> "token".equals(cookie.getName()))
                            .map(Cookie::getValue)
                            .findFirst())
                    .orElse(null);
            
            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                
                // 处理角色映射和前缀
                String roleWithPrefix;
                if (role != null) {
                    if (role.equals("superAdmin")) {
                        roleWithPrefix = "ROLE_ADMIN";
                    } else {
                        roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    }
                } else {
                    roleWithPrefix = "ROLE_USER";
                }
                
                // 设置认证信息
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix))
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("为用户 {} 设置安全上下文", username);
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
} 