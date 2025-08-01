package com.example.blog.dto;

import lombok.Data;

/**
 * 更新用户个人信息请求
 */
@Data
public class UpdateProfileRequest {
    private String email;
    private String phoneNumber;
    private String realName;
    private String remark;
} 