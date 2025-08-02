package com.example.blog.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private boolean rememberMe;
    
    @JsonProperty("captcha")
    private String verifyCode;
} 