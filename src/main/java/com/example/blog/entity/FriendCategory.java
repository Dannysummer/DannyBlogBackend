package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FriendCategory {
    BIGSHOT("bigshot"),    // 大佬
    CLOSE("close"),        // 密友
    FRIEND("friend"),      // 普通朋友
    TECH("tech");         // 技术博客

    private final String value;

    FriendCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FriendCategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FRIEND;  // 空值时使用默认值
        }
        // 去除前后空格
        value = value.trim();
        for (FriendCategory category : FriendCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return FRIEND;  // 无法匹配时使用默认值
    }
}