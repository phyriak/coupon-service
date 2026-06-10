package com.example.coupon.anonymizer;

import com.example.coupon.anonymizer.properties.LoggingProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class LogAnonymizer {

    private final LoggingProperties properties;

    public String anonymize(String value) {
        return HashUtil.sha256(
                value + properties.pepper()
        ).substring(0, 12);
    }
}