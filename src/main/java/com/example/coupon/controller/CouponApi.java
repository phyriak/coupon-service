package com.example.coupon.controller;

import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;


@RequestMapping("/api/v1/coupons")
public interface CouponApi {

    @Operation(summary = "Create a new coupon")
    @ApiResponse(responseCode = "201", description = "Coupon created successfully")
    @ApiResponse(responseCode = "409", description = "Coupon code already exists")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request);


    @Operation(summary = "Redeem a coupon")
    @ApiResponse(responseCode = "200", description = "Coupon redeemed successfully")
    @ApiResponse(responseCode = "404", description = "Coupon not found")
    @ApiResponse(responseCode = "410", description = "Coupon limit reached")
    @ApiResponse(responseCode = "403", description = "Country not allowed")
    @ApiResponse(responseCode = "409", description = "Coupon already used by this user")
    @PostMapping("/use")
    UseCouponResponse useCoupon(
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest
    );
}
