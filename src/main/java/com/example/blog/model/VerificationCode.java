package com.example.blog.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VerificationCode {
    private String code;
    private LocalDateTime expireTime;
    
    public VerificationCode(String code) {
        this.code = code;
        this.expireTime = LocalDateTime.now().plusMinutes(5);  // 5分钟有效期
    }
    
    public boolean isValid() {
        return LocalDateTime.now().isBefore(expireTime);
    }
} 