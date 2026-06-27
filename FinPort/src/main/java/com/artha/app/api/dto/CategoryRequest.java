package com.artha.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "name is mandatory")
        @Size(max = 100, message = "name must be 100 characters or fewer")
        String name
) {
}