package com.example.coupon.integration;

import com.example.coupon.BaseIntegrationTest;
import com.example.coupon.domain.exception.CountryResolutionException;
import com.example.coupon.domain.exception.CouponAlreadyExistsException;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.exception.CouponLimitReachedException;
import com.example.coupon.domain.exception.CountryNotAllowedException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.repository.CouponRepository;
import com.example.coupon.repository.CouponUsageRepository;
import com.example.coupon.service.GeoLocationService;
import com.example.coupon.service.CouponService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CouponServiceIntegrationTest extends BaseIntegrationTest {

    public static final String ONEUSE_CODE = "ONEUSE";
    public static final String PLONLY_CODE = "PLONLY";
    public static final String CONCURRENT_CODE = "CONCURRENT";
    public static final String TEST_USER = "user-42";
    public static final String IP_ADDRESS = "1.2.3.4";
    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    @MockitoBean
    private GeoLocationService geoLocationService;

    @BeforeEach
    void setUp() {
        couponUsageRepository.deleteAll();
        couponRepository.deleteAll();
        when(geoLocationService.resolveCountry(anyString()))
                .thenReturn(Optional.of(Country.PL));
    }

    @Test
    void shouldCreateCoupon() {
        var request = new CreateCouponRequest(PLONLY_CODE, 10, Country.PL);
        var response = couponService.createCoupon(request);

        assertThat(response.code()).isEqualTo(PLONLY_CODE);
        assertThat(response.usageCount()).isZero();
    }

    @Test
    void shouldNormalizeCouponCodeToUppercase() {
        var request = new CreateCouponRequest("test", 5, Country.PL);
        var response = couponService.createCoupon(request);

        assertThat(response.code()).isEqualTo("TEST");
    }

    @Test
    void shouldRejectUsageFromWrongCountry() {
        when(geoLocationService.resolveCountry(anyString()))
                .thenReturn(Optional.of(Country.DE));

        couponService.createCoupon(new CreateCouponRequest(PLONLY_CODE, 10, Country.PL));

        assertThatThrownBy(() ->
            couponService.useCoupon(new UseCouponRequest(PLONLY_CODE, TEST_USER), IP_ADDRESS)
        ).isInstanceOf(CountryNotAllowedException.class)
                .hasMessageContaining("Coupon not available in your country: DE");
    }

    @Test
    void shouldRejectSecondUsageBySameUser() {
        couponService.createCoupon(new CreateCouponRequest(ONEUSE_CODE, 10, Country.PL));
        couponService.useCoupon(new UseCouponRequest(ONEUSE_CODE, TEST_USER), IP_ADDRESS);

        assertThatThrownBy(() ->
            couponService.useCoupon(new UseCouponRequest(ONEUSE_CODE, TEST_USER), IP_ADDRESS)
        ).isInstanceOf(CouponAlreadyUsedException.class)
                .hasMessageContaining("User user-42 has already used coupon: ONEUSE");
    }

    @Test
    void shouldRejectCreateSameCoupon() {
        CreateCouponRequest couponRequest = new CreateCouponRequest(PLONLY_CODE, 10, Country.PL);
        couponService.createCoupon(couponRequest);

        assertThatThrownBy(() ->
                couponService.createCoupon(couponRequest)
        ).isInstanceOf(CouponAlreadyExistsException.class)
                .hasMessageContaining("Coupon already exists: PLONLY");
    }

    @Test
    void shouldRejectUsageWhenCountryCannotBeResolved() {

        when(geoLocationService.resolveCountry(anyString()))
                .thenReturn(Optional.empty());

        couponService.createCoupon(
                new CreateCouponRequest(PLONLY_CODE, 10, Country.PL)
        );

        AssertionsForClassTypes.assertThatThrownBy(() ->
                couponService.useCoupon(
                        new UseCouponRequest(PLONLY_CODE, TEST_USER),
                        IP_ADDRESS
                )
        ).isInstanceOf(CountryResolutionException.class);
    }

    @Test
    void shouldRespectUsageLimitUnderConcurrency() throws InterruptedException {
        int limit = 5;
        int threads = 20;
        couponService.createCoupon(new CreateCouponRequest(CONCURRENT_CODE, limit, Country.PL));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                final String userId = "user-" + i;
                executor.submit(() -> {
                    try {
                        latch.await();
                        couponService.useCoupon(new UseCouponRequest(CONCURRENT_CODE, userId), IP_ADDRESS);
                        successCount.incrementAndGet();
                    } catch (CouponLimitReachedException e) {
                        rejectedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            latch.countDown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertThat(successCount.get()).isEqualTo(limit);
        assertThat(rejectedCount.get()).isEqualTo(threads - limit);
    }
}
