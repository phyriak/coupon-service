package com.example.coupon.dto.response;

import com.example.coupon.domain.model.Country;

import java.time.Instant;

public record CouponResponse(
    Long id,
    String code,
    int usageLimit,
    int usageCount,
    Country country,
    Instant createdAt
) {}
