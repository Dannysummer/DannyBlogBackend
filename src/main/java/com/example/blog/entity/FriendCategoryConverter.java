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
        if (value == null || value.trim().isEmpty()) {
            logger.warn("category字段为空或null，使用默认值FRIEND");
            return FriendCategory.FRIEND;  // 使用默认值
        }
        
        try {
            // 使用fromString方法转换，该方法已经处理了所有情况
            return FriendCategory.fromString(value);
        } catch (Exception e) {
            logger.warn("无法将值 '{}' 转换为 FriendCategory 枚举，使用默认值FRIEND", value);
            return FriendCategory.FRIEND;  // 转换失败时使用默认值
        }
    }
}