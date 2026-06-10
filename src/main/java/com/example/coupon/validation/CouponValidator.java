package com.example.coupon.validation;

import com.example.coupon.anonymizer.LogAnonymizer;
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


    private final CouponUsageRepository couponUsageRepository;
    private final LogAnonymizer logAnonymizer;


    public void validateUsage(Coupon coupon, String userId, String ipAddress, Country country) {
        validateCountry(coupon, ipAddress, country);
        validateNotAlreadyUsed(coupon, userId);
    }

    private void validateCountry(Coupon coupon, String ipAddress, Country country) {
        String anonymizedId = logAnonymizer.anonymize(ipAddress);
        if (!coupon.isAllowedCountry(country)) {
            log.warn("Country not allowed: code={}, country={}, anonymized ip={}",
                    coupon.getCode(), country, anonymizedId);
            throw new CountryNotAllowedException(country);
        }
    }


    private void validateNotAlreadyUsed(Coupon coupon, String userId) {
        String anonymizedUserId = logAnonymizer.anonymize(userId);
        if (couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            log.warn("Coupon already used: code={}, anonymized userId={}", coupon.getCode(), anonymizedUserId);
            throw new CouponAlreadyUsedException(coupon.getCode(), anonymizedUserId);
        }
    }
}
