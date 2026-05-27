package com.example.coupon.dto.response;

public record UseCouponResponse(
    String code,
    String userId,
    String message
) {
    public static UseCouponResponse success(String code, String userId) {
        return new UseCouponResponse(code, userId, "Coupon applied successfully");
    }
}
