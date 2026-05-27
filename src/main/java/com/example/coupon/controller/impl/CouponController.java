package com.example.coupon.controller.impl;

import com.example.coupon.controller.CouponApi;
import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;
import com.example.coupon.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;

    public CouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.createCoupon(request);
    }

    public UseCouponResponse useCoupon(
        @Valid @RequestBody UseCouponRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = resolveClientIp(httpRequest);
        return couponService.useCoupon(request, ipAddress);
    }

    /**
     * Resolves real client IP, accounting for reverse proxies via X-Forwarded-For.
     * Takes the first entry (original client), not the last (proxy).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
