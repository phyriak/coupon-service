package com.example.coupon.dto.request;

import com.example.coupon.domain.model.Country;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(

        @NotBlank(message = "Coupon code must not be blank")
        @Size(max = 100, message = "Coupon code must not exceed 100 characters")
        String code,

        @NotNull(message = "Usage limit is required")
        @Positive(message = "Usage limit must be at least 1")
        Integer usageLimit,

        @NotNull(message = "Country is required")
        Country country
) {
}
