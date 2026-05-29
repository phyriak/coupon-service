package com.example.coupon.validation;

import com.example.coupon.domain.exception.CountryNotAllowedException;
import com.example.coupon.domain.exception.CountryResolutionException;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.domain.model.Coupon;
import com.example.coupon.repository.CouponUsageRepository;
import com.example.coupon.service.GeoLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponValidatorTest {

    public static final String RANDOM_IP = "1.2.3.4";
    public static final String DUMMY_USER = "dummyUser";
    @Mock
    private GeoLocationService geoLocationService;
    @Mock
    private CouponUsageRepository couponUsageRepository;

    private CouponValidator validator;

    @BeforeEach
    void set() {
        validator = new CouponValidator(
                geoLocationService,
                couponUsageRepository
        );

    }

    @Test
    void shouldPassWhenCouponIsValidAndNotUsed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);

        when(geoLocationService.resolveCountry(anyString())).thenReturn(Optional.of(Country.PL));
        when(couponUsageRepository.existsByCouponIdAndUserId(any(), anyString())).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateUsage(coupon, DUMMY_USER, RANDOM_IP));
    }

    @Test
    void shouldThrownWhenUserCountryIsNotAllowed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Optional.of(Country.DE));

        assertThrows(CountryNotAllowedException.class,
                () -> validator.validateUsage(coupon, DUMMY_USER, RANDOM_IP));
    }

    @Test
    void shouldThrowWhenCouponAlreadyUsed() {
        Coupon coupon = Coupon.create("PL", 3, Country.PL);

        when(geoLocationService.resolveCountry(anyString()))
                .thenReturn(Optional.of(Country.PL));
        when(couponUsageRepository.existsByCouponIdAndUserId(any(),
                anyString())).thenReturn(true);
        assertThrows(CouponAlreadyUsedException.class,
                () -> validator.validateUsage(coupon, DUMMY_USER, RANDOM_IP));
    }
}