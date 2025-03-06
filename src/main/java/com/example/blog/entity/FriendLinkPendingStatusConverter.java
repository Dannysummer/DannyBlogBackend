package com.example.blog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FriendLinkPendingStatusConverter implements AttributeConverter<FriendLinkPendingStatus, String> {

    @Override
    public String convertToDatabaseColumn(FriendLinkPendingStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public FriendLinkPendingStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FriendLinkPendingStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
} 