package com.example.coupon.integration;

import com.example.coupon.BaseIntegrationTest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.PL);
    }

    @Test
    void shouldCreateCoupon() {
        var request = new CreateCouponRequest("SPRING2024", 10, Country.PL);
        var response = couponService.createCoupon(request);

        assertThat(response.code()).isEqualTo("SPRING2024");
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
        when(geoLocationService.resolveCountry(anyString())).thenReturn(Country.DE);

        couponService.createCoupon(new CreateCouponRequest("PLONLY", 10, Country.PL));

        assertThatThrownBy(() ->
            couponService.useCoupon(new UseCouponRequest("PLONLY", "user-1"), "1.2.3.4")
        ).isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void shouldRejectSecondUsageBySameUser() {
        couponService.createCoupon(new CreateCouponRequest("ONEUSE", 10, Country.PL));
        couponService.useCoupon(new UseCouponRequest("ONEUSE", "user-42"), "1.2.3.4");

        assertThatThrownBy(() ->
            couponService.useCoupon(new UseCouponRequest("ONEUSE", "user-42"), "1.2.3.4")
        ).isInstanceOf(CouponAlreadyUsedException.class);
    }

    @Test
    void shouldRespectUsageLimitUnderConcurrency() throws InterruptedException {
        int limit = 5;
        int threads = 20;
        couponService.createCoupon(new CreateCouponRequest("CONCURRENT", limit, Country.PL));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                final String userId = "user-" + i;
                executor.submit(() -> {
                    try {
                        latch.await();
                        couponService.useCoupon(new UseCouponRequest("CONCURRENT", userId), "1.2.3.4");
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
