package com.example.coupon.client.geo;

import com.example.coupon.BaseIntegrationTest;
import com.example.coupon.domain.model.Country;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


class IpApiGeoLocationServiceTest extends BaseIntegrationTest {
    public static final String DEFAULT_PRIVATE_IP = "0:0:0:0:0:0:0:1";
    public static final String PUBLIC_IP_ADDRESS = "8.8.8.8";

    @Autowired
    private IpApiGeoLocationService service;

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    @Test
    void shouldResolveCountryOnSuccessResponse() {
        mockWebServer.enqueue(jsonResponse("""
                {"status":"success","countryCode":"DE"}
                """));

        assertThat(service.resolveCountry(PUBLIC_IP_ADDRESS)).contains(Country.DE);
    }

    @Test
    void shouldReturnEmptyWhenApiReturnsNonSuccess() {
        mockWebServer.enqueue(jsonResponse("""
                {"status":"fail","countryCode":null}
                """));

        assertThat(service.resolveCountry(PUBLIC_IP_ADDRESS)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenApiReturnsNull() {
        mockWebServer.enqueue(jsonResponse("null"));

        assertThat(service.resolveCountry(PUBLIC_IP_ADDRESS)).isEmpty();
    }

    @Test
    void shouldReturnUnknownForUnrecognizedCountryCode() {
        mockWebServer.enqueue(jsonResponse("""
                {"status":"success","countryCode":"XX"}
                """));

        assertThat(service.resolveCountry(PUBLIC_IP_ADDRESS)).contains(Country.UNKNOWN);
    }

    @Test
    void shouldReturnEmptyForLoopbackIp() {
        assertThat(service.resolveCountry(DEFAULT_PRIVATE_IP)).isEmpty();
    }
}