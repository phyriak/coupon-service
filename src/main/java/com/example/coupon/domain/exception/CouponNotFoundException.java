package com.example.coupon.domain.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String code) {
        super("Coupon not found: " + code);
    }
}
