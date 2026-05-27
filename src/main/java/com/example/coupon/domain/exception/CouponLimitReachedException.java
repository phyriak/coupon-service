package com.example.coupon.domain.exception;

public class CouponLimitReachedException extends RuntimeException {
    public CouponLimitReachedException(String code) {
        super("Coupon has reached its usage limit: " + code);
    }
}
