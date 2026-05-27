package com.example.coupon.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "geolocation.ip-api")
@Validated
public record IpApiProperties(
        @NotBlank String url,
        @DurationUnit(ChronoUnit.SECONDS)
        Duration connectTimeout,
        @DurationUnit(ChronoUnit.SECONDS)
        Duration readTimeout
) {
}
