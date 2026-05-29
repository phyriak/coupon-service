package com.example.coupon.client.geo;

import com.example.coupon.BaseIntegrationTest;
import com.example.coupon.domain.model.Country;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IpApiGeoLocationRetryTest extends BaseIntegrationTest {

    @Autowired
    private IpApiGeoLocationService service;

    private static MockResponse connectionReset() {
        return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST);
    }

    private static MockResponse success(String countryCode) {
        return new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"status":"success","countryCode":"%s"}
                        """.formatted(countryCode));
    }

    @Test
    void shouldRetryOnTimeoutAndSucceedOnThirdAttempt() throws InterruptedException {
        mockWebServer.enqueue(connectionReset());
        mockWebServer.enqueue(connectionReset());
        mockWebServer.enqueue(success("DE"));

        assertThat(service.resolveCountry("8.8.8.8")).contains(Country.DE);

        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
    }

    @Test
    void shouldInvokeFallbackAfterAllAttemptsTimeout() throws InterruptedException {
        mockWebServer.enqueue(connectionReset());
        mockWebServer.enqueue(connectionReset());
        mockWebServer.enqueue(connectionReset());

        assertThat(service.resolveCountry("8.8.8.8")).isEmpty();

        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
    }
}