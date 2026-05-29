package com.example.coupon.service;

import com.example.coupon.domain.model.Country;

import java.util.Optional;

public interface GeoLocationService {

    /**
     * Resolves country from client IP address.
     * Returns UNKNOWN when private IP detected
     * Return Empty Optional when Geo Api fails.
     */
    Optional<Country> resolveCountry(String ipAddress);
}
