package com.example.coupon.service;

import com.example.coupon.domain.model.Country;

public interface GeoLocationService {

    /**
     * Resolves the country for the given IP address.
     * Returns {@link Country#UNKNOWN} when resolution is not possible
     */
    Country resolveCountry(String ipAddress);
}
