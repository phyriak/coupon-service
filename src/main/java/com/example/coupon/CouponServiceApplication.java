package com.example.coupon;

import com.example.coupon.anonymizer.properties.LoggingProperties;
import com.example.coupon.config.GeoApiProperties;
import com.example.coupon.security.ApiKeyProperties;
import com.example.coupon.web.IpResolutionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        GeoApiProperties.class,
        LoggingProperties.class,
        IpResolutionProperties.class,
        ApiKeyProperties.class
})
public class CouponServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponServiceApplication.class, args);
    }
}
