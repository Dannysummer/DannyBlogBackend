package com.example.blog.dto;

import lombok.Data;

@Data
public class EmailCodeRequest {
    private String email;
    private String code;  // 验证码字段
    private String type;  // 验证码类型：admin_verify 或其他
} 