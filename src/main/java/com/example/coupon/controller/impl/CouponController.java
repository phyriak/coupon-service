package com.example.coupon.controller.impl;

import com.example.coupon.controller.CouponApi;
import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;
import com.example.coupon.service.CouponService;
import com.example.coupon.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponApi {

    private final CouponService couponService;
    private final ClientIpResolver ipResolver;

    public CouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.createCoupon(request);
    }

    public UseCouponResponse useCoupon(
        @Valid @RequestBody UseCouponRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = ipResolver.resolveClientIp(httpRequest);
        return couponService.useCoupon(request, ipAddress);
    }
}
