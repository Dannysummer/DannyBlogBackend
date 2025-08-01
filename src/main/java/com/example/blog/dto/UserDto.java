package com.example.blog.dto;

import com.example.blog.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String password; // 只用于创建和更新用户时传输密码，返回给前端时不包含该字段
    private String role;
    private String avatar;
    private UserStatus status;
    private String phoneNumber;
    private String realName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
    private String updatedBy;
    private Long rank; // 用户注册排名
} 