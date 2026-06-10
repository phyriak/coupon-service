package com.example.coupon.service.impl;

import com.example.coupon.domain.exception.CountryResolutionException;
import com.example.coupon.domain.exception.CouponAlreadyExistsException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.domain.model.Coupon;
import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;
import com.example.coupon.mapper.CouponMapper;
import com.example.coupon.repository.CouponRepository;
import com.example.coupon.service.CouponService;
import com.example.coupon.service.GeoLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;
    private final GeoLocationService geoLocationService;
    private final CouponTransactionService transactionService;


    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String normalizedCode = request.code().toUpperCase(Locale.ROOT);

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
    public UseCouponResponse useCoupon(UseCouponRequest request, String ipAddress) {
        Country country = geoLocationService.resolveCountry(ipAddress)
                .orElseThrow(() -> {
                    log.warn(
                            "Country could not be resolved: code={}, ipHashCode={}",
                            request.code(),
                            ipAddress.hashCode()
                    );
                    return new CountryResolutionException(ipAddress);
                });

        return transactionService.useCoupon(request, ipAddress, country);
    }

}
