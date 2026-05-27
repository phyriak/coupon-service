package com.example.coupon.service;

import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;

public interface CouponService {

    /**
     * Creates a new coupon. Code uniqueness is enforced case-insensitively.
     *
     * @throws com.example.coupon.domain.exception.CouponAlreadyExistsException when code already taken
     */
    CouponResponse createCoupon(CreateCouponRequest request);

    /**
     * Attempts to redeem a coupon for the given user from the given IP address.
     *
     * @throws com.example.coupon.domain.exception.CouponNotFoundException      when code not found
     * @throws com.example.coupon.domain.exception.CouponLimitReachedException  when usage limit exhausted
     * @throws com.example.coupon.domain.exception.CountryNotAllowedException   when IP country doesn't match
     * @throws com.example.coupon.domain.exception.CouponAlreadyUsedException   when user already redeemed it
     */
    UseCouponResponse useCoupon(UseCouponRequest request, String ipAddress);
}
