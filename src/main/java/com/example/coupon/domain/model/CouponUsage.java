package com.example.coupon.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "coupon_usages",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_coupon_usages_coupon_user",
        columnNames = {"coupon_id", "user_id"}
    )
)
public class CouponUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    /**
     * External user identifier. Could be UUID, email, or any system-provided ID.
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    public static CouponUsage of(Coupon coupon, String userId) {
        CouponUsage usage = new CouponUsage();
        usage.coupon = coupon;
        usage.userId = userId;
        return usage;
    }
}
