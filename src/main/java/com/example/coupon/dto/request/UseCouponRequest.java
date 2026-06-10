package com.example.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UseCouponRequest(

    @NotBlank(message = "Coupon code must not be blank")
    String code,

    @NotBlank(message = "User ID must not be blank")
    @Size(max = 255)
    String userId
) {}
