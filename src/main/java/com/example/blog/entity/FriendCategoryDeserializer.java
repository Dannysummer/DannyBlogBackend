package com.example.blog.entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FriendCategoryDeserializer extends JsonDeserializer<FriendCategory> {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendCategoryDeserializer.class);

    @Override
    public FriendCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        logger.info("正在反序列化category字段: {}", value);
        
        if (value == null || value.trim().isEmpty()) {
            logger.warn("category字段为空字符串或null，使用默认值FRIEND");
            return FriendCategory.FRIEND;  // 使用默认值而不是null
        }
        
        FriendCategory category = FriendCategory.fromString(value);
        if (category == null) {
            logger.warn("无法解析category值: {}，使用默认值FRIEND", value);
            return FriendCategory.FRIEND;  // 解析失败时使用默认值
        }
        logger.info("反序列化结果: {}", category);
        return category;
    }
}