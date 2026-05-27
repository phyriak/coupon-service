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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponValidatorTest {

    @Mock
    private GeoLocationService geoLocationService;
    @Mock
    private CouponUsageRepository couponUsageRepository;
    @Mock
    private CouponRepository couponRepository;

    private CouponValidator validator;

    @BeforeEach
    void set() {
        validator = new CouponValidator(
                geoLocationService,
                couponUsageRepository,
                couponRepository
        );

    }

    @Test
    void shouldPassWhenCouponIsValidAndNotUsed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);

        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.PL);
        when(couponUsageRepository.existsByCouponIdAndUserId(any(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateUsage(coupon, "dummyUser", "1.2.3.4"));
    }

    @Test
    void shouldPassWhenUserHasUnknownCountryAndCouponIsNotUsed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);

        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.UNKNOWN);
        when(couponUsageRepository.existsByCouponIdAndUserId(any(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateUsage(coupon, "dummyUser", "1.2.3.4"));
    }

    @Test
    void shouldThrowWhenCouponAlreadyExist() {
        when(couponRepository.existsByCode(anyString())).thenReturn(true);
        assertThrows(CouponAlreadyExistsException.class,
                () -> validator.validateCreate("code"));
    }

    @Test
    void shouldThrownWhenUserCountryIsNotAllowed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.DE);

        assertThrows(CountryNotAllowedException.class,
                () -> validator.validateUsage(coupon, "dummyUser", "1.2.3.4"));
    }

    @Test
    void shouldThrowWhenCouponAlreadyUsed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);

        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.PL);
        when(couponUsageRepository.existsByCouponIdAndUserId(any(), anyString())).thenReturn(true);
        assertThrows(CouponAlreadyUsedException.class,
                () -> validator.validateUsage(coupon, "dummyUser", "1.2.3.4"));
    }

    @Test
    void shouldThrowWhenCouponReachLimit() {
        Coupon coupon = Coupon.create("PL", 0, Country.PL);


        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.PL);
        when(couponUsageRepository.existsByCouponIdAndUserId(any(), anyString())).thenReturn(false);
        assertThrows(CouponLimitReachedException.class,
                () -> validator.validateUsage(coupon, "dummyUser", "1.2.3.4"));
    }

    @Test
    void shouldPassWhenCouponDoesNotExist() {
        when(couponRepository.existsByCode("CODE")).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateCreate("CODE"));
    }
}