package com.example.coupon.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ip-resolution")
public record IpResolutionProperties(boolean trustForwardedHeaders, List<String> trustedProxies) {
}
