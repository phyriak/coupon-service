package com.example.coupon.domain.exception;

import com.example.coupon.domain.model.Country;

public class CountryNotAllowedException extends RuntimeException {
    public CountryNotAllowedException(Country countryCode) {
        super("Coupon not available in your country: " + countryCode);
    }
}
