package com.example.coupon.service.impl;

import com.example.coupon.domain.exception.CouponAlreadyExistsException;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.exception.CouponLimitReachedException;
import com.example.coupon.domain.exception.CouponNotFoundException;
import com.example.coupon.domain.model.Coupon;
import com.example.coupon.domain.model.CouponUsage;
import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;
import com.example.coupon.mapper.CouponMapper;
import com.example.coupon.repository.CouponRepository;
import com.example.coupon.repository.CouponUsageRepository;
import com.example.coupon.service.CouponService;
import com.example.coupon.validation.CouponValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponMapper couponMapper;
    private final CouponValidator validator;

    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String normalizedCode = request.code().toUpperCase();
        validator.validateCreate(normalizedCode);

        //To avoid Race condition, unique constraint + exception handling for rollback
        try {
            Coupon coupon = Coupon.create(normalizedCode, request.usageLimit(), request.country());
            Coupon saved = couponRepository.save(coupon);
            log.info("Coupon created: code={}, limit={}, country={}",
                    normalizedCode, request.usageLimit(), request.country());
            return couponMapper.toResponse(saved);

        } catch (DataIntegrityViolationException ex) {
            throw new CouponAlreadyExistsException(normalizedCode);
        }
    }

    @Override
    @Transactional
    public UseCouponResponse useCoupon(UseCouponRequest request, String ipAddress) {
        String normalizedCode = request.code().toUpperCase();
        String userId = request.userId();

        log.info("Attempting to use coupon: code={}, userId={}, ip={}",
                normalizedCode, userId, ipAddress);

        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException(normalizedCode));

        validator.validateUsage(coupon, userId, ipAddress);
        int updated = couponRepository.incrementUsageIfAvailable(normalizedCode);

        if (updated == 0) {
            throw new CouponLimitReachedException(normalizedCode);
        }

        try {
            couponUsageRepository.save(CouponUsage.of(coupon, userId));
        } catch (DataIntegrityViolationException ex) {
            throw new CouponAlreadyUsedException(normalizedCode, userId);
        }

        log.info("Coupon used: code={}, userId={}, ip={}", normalizedCode, userId, ipAddress);
        return UseCouponResponse.success(normalizedCode, userId);
    }
}
