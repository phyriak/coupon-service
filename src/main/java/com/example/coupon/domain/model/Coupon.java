package com.example.coupon.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "coupons",
    indexes = @Index(name = "idx_coupons_code", columnList = "code", unique = true)
)
public class Coupon extends BaseEntity {

    /**
     * Always stored as UPPERCASE — normalization happens at the service layer.
     * Unique constraint is enforced both here and in the DB schema (Liquibase).
     */
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private int usageLimit;

    @Column(nullable = false)
    private int usageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Country country;

    public static Coupon create(String code, int usageLimit, Country country) {
        Coupon coupon = new Coupon();
        coupon.code = code.toUpperCase();
        coupon.usageLimit = usageLimit;
        coupon.usageCount = 0;
        coupon.country = country;
        return coupon;
    }

    public boolean hasReachedLimit() {
        return usageCount >= usageLimit;
    }

    public boolean isAllowedCountry(Country requestCountry) {
        // UNKNOWN country passes through (fail-open for geolocation failures)
        if (requestCountry == Country.UNKNOWN) {
            return true;
        }
        return this.country == requestCountry;
    }
}
