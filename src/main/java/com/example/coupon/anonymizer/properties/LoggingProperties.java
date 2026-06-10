package com.example.coupon.anonymizer.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logging")
public record LoggingProperties(String pepper) {
}
