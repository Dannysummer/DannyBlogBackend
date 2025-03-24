package com.example.blog.enums;

/**
 * 用户状态枚举类
 */
public enum UserStatus {
    NORMAL("正常", "用户状态正常，可以正常登录和使用系统"),
    PENDING("待审核", "用户注册后等待管理员审核"),
    LOCKED("锁定", "用户因密码错误次数过多被临时锁定"),
    BANNED("封禁", "用户因违规行为被管理员封禁"),
    DELETED("注销", "用户已注销账号");
    
    private final String displayName;
    private final String description;
    
    UserStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
} 