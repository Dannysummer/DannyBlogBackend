package com.example.blog.config;

import com.example.blog.dto.ApiResponse;
import com.example.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;
import java.io.IOException;
import java.util.Arrays;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // 添加自定义认证入口点
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new AuthenticationEntryPoint() {
            private final Logger logger = LoggerFactory.getLogger(this.getClass());
            
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                                AuthenticationException authException) throws IOException, ServletException {
                
                logger.warn("未授权访问: {}, 错误: {}", request.getRequestURI(), authException.getMessage());
                
                ApiResponse<?> apiResponse = ApiResponse.error("请大人先登录哦");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                
                new ObjectMapper().writeValue(response.getOutputStream(), apiResponse);
            }
        };
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 添加日志记录安全配置
        Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
        logger.info("正在配置安全过滤链...");
        
        http
            .cors(cors -> {
                logger.info("配置CORS...");
                cors.configurationSource(corsConfigurationSource());
            })
            .csrf(csrf -> {
                logger.info("禁用CSRF保护...");
                csrf.disable();
            })
            .authorizeHttpRequests(auth -> {
                logger.info("配置URL访问权限...");
                logger.info("设置以下路径允许匿名访问: /api/auth/*, /api/friend-links/*");
                auth
                    .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/send-email-code",
                        "/api/auth/verify-email-code",
                        "/api/auth/send-login-code",
                        "/api/auth/reset-password",
                        "/api/auth/logout",
                        "/api/friend-links",
                        "/api/friend-links/all",
                        "/api/friend-links/category/**",
                        "/api/bullet-chats",
                        "/api/bullet-chats/{id}",
                        "/api/bullet-chats/status/**",
                        "/error"
                    ).permitAll()
                    .requestMatchers(
                        "/api/friend-links-pending",
                        "/api/friend-links-pending/**",
                        "/api/bullet-chats/{id}/**"
                    ).hasRole("ADMIN")
                    .anyRequest().authenticated();
            })
            .exceptionHandling(exceptions -> {
                logger.info("配置异常处理...");
                exceptions.authenticationEntryPoint(authenticationEntryPoint());
            })
            .sessionManagement(session -> {
                logger.info("配置会话管理为无状态...");
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            })
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        
        logger.info("安全过滤链配置完成");
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 