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
        if (value == null) {
            return null;
        }
        for (FriendCategory category : FriendCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;
    }
} 