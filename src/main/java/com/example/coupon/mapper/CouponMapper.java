package com.example.coupon.mapper;

import com.example.coupon.domain.model.Coupon;
import com.example.coupon.dto.response.CouponResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponResponse toResponse(Coupon coupon);
}
