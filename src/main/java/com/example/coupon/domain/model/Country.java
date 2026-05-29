package com.example.coupon.domain.model;

/**
 * Country codes.
 * UNKNOWN is used as a fallback when geolocation fails.
 */
public enum Country {
    PL, DE, FR, GB, US, CA, UNKNOWN;

    public static Country fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        try {
            return Country.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
