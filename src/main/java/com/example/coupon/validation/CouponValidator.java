package com.example.coupon.validation;

import com.example.coupon.domain.exception.CountryNotAllowedException;
import com.example.coupon.domain.exception.CountryResolutionException;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.domain.model.Coupon;
import com.example.coupon.repository.CouponUsageRepository;
import com.example.coupon.service.GeoLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponValidator {

    private final GeoLocationService geoLocationService;
    private final CouponUsageRepository couponUsageRepository;


    public void validateUsage(Coupon coupon, String userId, String ipAddress) {
        validateCountry(coupon, ipAddress);
        validateNotAlreadyUsed(coupon, userId);
    }

    private void validateCountry(Coupon coupon, String ipAddress) {
        Optional<Country> requestCountry = geoLocationService.resolveCountry(ipAddress);
        if (requestCountry.isEmpty()) {
            log.warn(
                    "Country could not be resolved: code={}, ipHashCode={}",
                    coupon.getCode(),
                    ipAddress.hashCode()
            );
            throw new CountryResolutionException(ipAddress);
        }

        if (!coupon.isAllowedCountry(requestCountry.get())) {
            log.warn("Country not allowed: code={}, country={}, ipHashCode={}",
                    coupon.getCode(), requestCountry, ipAddress.hashCode());
            throw new CountryNotAllowedException(requestCountry.get());
        }
    }


    private void validateNotAlreadyUsed(Coupon coupon, String userId) {
        if (couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            log.warn("Coupon already used: code={}, userIdHashCode={}", coupon.getCode(), userId.hashCode());
            throw new CouponAlreadyUsedException(coupon.getCode(), userId);
        }
    }
}
