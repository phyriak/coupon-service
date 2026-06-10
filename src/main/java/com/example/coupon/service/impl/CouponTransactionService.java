package com.example.coupon.service.impl;

import com.example.coupon.anonymizer.LogAnonymizer;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.exception.CouponLimitReachedException;
import com.example.coupon.domain.exception.CouponNotFoundException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.domain.model.Coupon;
import com.example.coupon.domain.model.CouponUsage;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.UseCouponResponse;
import com.example.coupon.repository.CouponRepository;
import com.example.coupon.repository.CouponUsageRepository;
import com.example.coupon.validation.CouponValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTransactionService {

    private final CouponUsageRepository couponUsageRepository;
    private final CouponValidator validator;
    private final LogAnonymizer logAnonymizer;
    private final CouponRepository couponRepository;


    @Transactional
    public UseCouponResponse useCoupon(UseCouponRequest request, String ipAddress, Country country) {
        String normalizedCode = request.code().toUpperCase(Locale.ROOT);
        String userId = request.userId();

        String anonymizedUserId = logAnonymizer.anonymize(userId);
        String anonymizedIp = logAnonymizer.anonymize(ipAddress);
        log.info("Attempting to use coupon: code={}, userIdCode={}, ipCode={}",
                normalizedCode, anonymizedUserId, anonymizedIp);

        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException(normalizedCode));

        validator.validateUsage(coupon, userId, ipAddress, country);
        int updated = couponRepository.incrementUsageIfAvailable(normalizedCode);

        if (updated == 0) {
            throw new CouponLimitReachedException(normalizedCode);
        }

        try {
            couponUsageRepository.save(CouponUsage.of(coupon, userId));
        } catch (DataIntegrityViolationException ex) {
            throw new CouponAlreadyUsedException(normalizedCode, anonymizedUserId);
        }

        log.info("Coupon used: code={}, userIdHashcode={}, ipHashcode={}",
                normalizedCode, anonymizedUserId, anonymizedIp);
        return UseCouponResponse.success(normalizedCode, anonymizedUserId);
    }
}
