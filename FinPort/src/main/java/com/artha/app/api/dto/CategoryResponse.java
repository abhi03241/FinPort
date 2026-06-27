package com.artha.app.api.dto;

import com.artha.app.models.Category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
    }
}