package com.example.coupon.repository;

import com.example.coupon.domain.model.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Returns number of affected rows:
     * - 1 → success
     * - 0 → concurrent update won the race OR limit already reached
     */
    @Modifying
    @Query("""
    UPDATE Coupon c
    SET c.usageCount = c.usageCount + 1
    WHERE c.code = :code
    AND c.usageCount < c.usageLimit
    """)
    int incrementUsageIfAvailable(@Param("code") String code);
}
