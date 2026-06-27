package com.artha.app.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record CategorizeRequest(
        @NotBlank(message = "description is required")
        String description,

        @DecimalMin(value = "0.0", inclusive = false)
        Double amount
) {}