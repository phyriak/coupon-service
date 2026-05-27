package com.example.coupon.mapper;

import com.example.coupon.domain.model.Coupon;
import com.example.coupon.dto.response.CouponResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponResponse toResponse(Coupon coupon);
}
