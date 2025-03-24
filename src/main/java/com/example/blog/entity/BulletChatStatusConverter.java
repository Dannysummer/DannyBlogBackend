package com.example.blog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BulletChatStatusConverter implements AttributeConverter<BulletChatStatus, String> {

    @Override
    public String convertToDatabaseColumn(BulletChatStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public BulletChatStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return BulletChatStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
} 