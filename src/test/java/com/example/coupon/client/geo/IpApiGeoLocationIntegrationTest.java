package com.example.coupon.client.geo;

import com.example.coupon.BaseIntegrationTest;
import com.example.coupon.domain.model.Country;
import com.example.coupon.service.GeoLocationService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IpApiGeoLocationIntegrationTest extends BaseIntegrationTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private GeoLocationService geoLocationService;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void geoApiUrl(DynamicPropertyRegistry registry) {
        String base = mockWebServer.url("/").toString();
        registry.add(
                "geolocation.ip-api.url",
                () -> base + "json/{ip}?fields=countryCode,status"
        );
    }


    @Test
    void shouldResolveCountryWhenApiReturnsSuccess() {
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("""
                {
                  "status":"success",
                  "countryCode":"PL"
                }
                """)
                        .addHeader("Content-Type", "application/json")
        );

        Country result = geoLocationService.resolveCountry("8.8.8.8");

        assertThat(result).isEqualTo(Country.PL);
    }

    @Test
    void shouldRetryOnTimeoutAndFallbackToUnknown() {
        int requestsBefore = mockWebServer.getRequestCount();

        mockWebServer.enqueue(
                new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(
                new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(
                new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        Country result = geoLocationService.resolveCountry("8.8.8.8");

        assertThat(result).isEqualTo(Country.UNKNOWN);

        int requestsMade = mockWebServer.getRequestCount() - requestsBefore;
        assertThat(requestsMade).isEqualTo(3);
    }

    @Test
    void shouldNotRetryOnServerErrorAndFallbackToUnknown() {
        int requestsBefore = mockWebServer.getRequestCount();

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Country result = geoLocationService.resolveCountry("8.8.8.8");

        assertThat(result).isEqualTo(Country.UNKNOWN);

        int requestsMade = mockWebServer.getRequestCount() - requestsBefore;

        // HttpServerErrorException is not configured for retry
        assertThat(requestsMade).isEqualTo(1);
    }
}