package com.example.coupon.client.geo;

import com.example.coupon.anonymizer.LogAnonymizer;
import com.example.coupon.client.geo.dto.IpApiResponse;
import com.example.coupon.config.GeoApiProperties;
import com.example.coupon.domain.model.Country;
import com.example.coupon.service.GeoLocationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Geolocation implementation using ip-api.com
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class IpApiGeoLocationService implements GeoLocationService {

    private final RestClient restClient;
    private final GeoApiProperties properties;
    private final LogAnonymizer logAnonymizer;


    @Override
    @Cacheable(cacheNames = "geoByIp", unless = "#result == null")
    @Retry(name = "geoLocation", fallbackMethod = "resolveFallback")
    @CircuitBreaker(name = "geoLocation")
    public Optional<Country> resolveCountry(String ipAddress) {
        String anonymizedIp = logAnonymizer.anonymize(ipAddress);
        if (isPrivateOrLoopback(ipAddress)) {
            if (properties.defaultCountryForPrivateIp() != null) {
                log.info("Private/loopback IP ({}), using configured default {}",
                        anonymizedIp, properties.defaultCountryForPrivateIp());
                return Optional.of(properties.defaultCountryForPrivateIp());
            }
            log.info("Private/loopback IP ({}), skipping geolocation", anonymizedIp);
            return Optional.empty();   // fail-closed
        }

        IpApiResponse response = restClient.get()
                .uri(properties.url(), ipAddress)
                .retrieve()
                .body(IpApiResponse.class);

        if (response == null || !"success".equals(response.status())) {
            log.warn("ip-api.com returned non-success for IP {}", anonymizedIp);
            return Optional.empty();
        }

        Country resolved = Country.fromCode(response.countryCode());
        log.info("Resolved IP {} -> country {}", anonymizedIp, resolved);
        return Optional.of(resolved);
    }


    private boolean isPrivateOrLoopback(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private Optional<Country> resolveFallback(String ipAddress, Exception ex) {
        log.warn("GeoLocation failed for IP {} after retries: {}", logAnonymizer.anonymize(ipAddress), ex.getMessage());
        return Optional.empty();
    }
}

