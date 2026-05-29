package com.example.coupon.domain.exception;

public class CountryResolutionException extends RuntimeException {
    public CountryResolutionException(String ipAddress) {
        super("Unable to determine country for provided IP " + ipAddress.hashCode());
    }
}
