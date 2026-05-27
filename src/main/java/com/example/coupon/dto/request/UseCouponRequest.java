package com.example.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UseCouponRequest(

    @NotBlank(message = "Coupon code must not be blank")
    String code,

    @NotBlank(message = "User ID must not be blank")
    String userId
) {}
