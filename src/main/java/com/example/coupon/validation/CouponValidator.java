package com.example.coupon.validation;

import com.example.coupon.domain.exception.CountryNotAllowedException;
import com.example.coupon.domain.exception.CouponAlreadyExistsException;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.exception.CouponLimitReachedException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.domain.model.Coupon;
import com.example.coupon.repository.CouponRepository;
import com.example.coupon.repository.CouponUsageRepository;
import com.example.coupon.service.GeoLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponValidator {

    private final GeoLocationService geoLocationService;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponRepository couponRepository;

    public void validateCreate(String code) {
        if (couponRepository.existsByCode(code)) {
            log.warn("Coupon already exists: code={}", code);
            throw new CouponAlreadyExistsException(code);
        }
    }

    public void validateUsage(Coupon coupon, String userId, String ipAddress) {
        validateCountry(coupon, ipAddress);
        validateNotAlreadyUsed(coupon,userId);
        validateNotExhausted(coupon);
    }

    private void validateCountry(Coupon coupon, String ipAddress) {
        Country requestCountry = geoLocationService.resolveCountry(ipAddress);

        if (!coupon.isAllowedCountry(requestCountry)) {
            log.warn("Country not allowed: code={}, country={}, ip={}", coupon.getCode(), requestCountry, ipAddress);
            throw new CountryNotAllowedException(coupon.getCountry());
        }
    }


    private void validateNotAlreadyUsed(Coupon coupon, String userId) {
        if (couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            log.warn("Coupon already used: code={}, userId={}", coupon.getCode(), userId);
            throw new CouponAlreadyUsedException(coupon.getCode(), userId);
        }
    }

    private void validateNotExhausted(Coupon coupon) {
        if (coupon.hasReachedLimit()) {
            log.warn("Coupon limit reached: code={}", coupon.getCode());
            throw new CouponLimitReachedException(coupon.getCode());
        }
    }
}
