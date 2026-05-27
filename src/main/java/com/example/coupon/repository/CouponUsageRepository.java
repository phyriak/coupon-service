package com.example.coupon.repository;

import com.example.coupon.domain.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, String userId);

}
