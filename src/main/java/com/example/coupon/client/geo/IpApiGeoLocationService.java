package com.example.coupon.client.geo;

import com.example.coupon.client.geo.dto.IpApiResponse;
import com.example.coupon.config.IpApiProperties;
import com.example.coupon.domain.model.Country;
import com.example.coupon.service.GeoLocationService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Geolocation implementation using ip-api.com
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class IpApiGeoLocationService implements GeoLocationService {

    private final RestClient restClient;
    private final IpApiProperties properties;

    @Retry(name = "geoLocation", fallbackMethod = "resolveFallback")
    @Override
    public Country resolveCountry(String ipAddress) {
        if (isPrivateOrLoopback(ipAddress)) {
            log.debug("Private/loopback IP detected ({}), skipping geolocation", ipAddress);
            return Country.UNKNOWN;
        }

        IpApiResponse response = restClient.get()
                .uri(properties.url(), ipAddress)
                .retrieve()
                .body(IpApiResponse.class);

        if (response == null || !"success".equals(response.status())) {
            log.warn("ip-api.com returned non-success for IP {}: {}", ipAddress, response);
            return Country.UNKNOWN;
        }

        Country resolved = Country.fromCode(response.countryCode());
        log.debug("Resolved IP {} -> country {}", ipAddress, resolved);
        return resolved;
    }

    private boolean isPrivateOrLoopback(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private Country resolveFallback(String ipAddress, Exception ex) {
        log.warn("GeoLocation failed for IP {} after retries: {}", ipAddress, ex.getMessage());
        return Country.UNKNOWN;
    }
}

