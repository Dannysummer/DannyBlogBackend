package com.example.blog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = true)
public class FriendCategoryConverter implements AttributeConverter<FriendCategory, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendCategoryConverter.class);

    @Override
    public String convertToDatabaseColumn(FriendCategory category) {
        if (category == null) {
            return null;
        }
        return category.getValue().toLowerCase();
    }

    @Override
    public FriendCategory convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            // 先尝试直接通过 fromString 方法转换
            FriendCategory result = FriendCategory.fromString(value);
            if (result != null) {
                return result;
            }
            
            // 如果失败，尝试将值转换为大写后匹配枚举名称
            return FriendCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("无法将值 '{}' 转换为 FriendCategory 枚举", value);
            return null;
        }
    }
} 