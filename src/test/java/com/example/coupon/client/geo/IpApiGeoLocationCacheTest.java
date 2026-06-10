package com.example.coupon.client.geo;

import com.example.coupon.config.BaseIntegrationTest;
import com.example.coupon.config.CacheConfig;
import com.example.coupon.domain.model.Country;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import(CacheConfig.class)
class IpApiGeoLocationCacheTest extends BaseIntegrationTest {

    @Autowired
    private IpApiGeoLocationService service;

    private static MockResponse success(String countryCode) {
        return new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"status":"success","countryCode":"%s"}
                        """.formatted(countryCode));
    }

    @Test
    void shouldCacheResultAndNotCallApiTwice() throws InterruptedException {
        mockWebServer.enqueue(success("DE"));   // only ONE response enqueued

        // first call: hits ip-api
        assertThat(service.resolveCountry("8.8.8.8")).contains(Country.DE);
        // second call: must come from cache, NOT ip-api
        assertThat(service.resolveCountry("8.8.8.8")).contains(Country.DE);

        // exactly one HTTP request reached the server
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();  // the 1st call
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNull();      // no 2nd call
    }

    @Test
    void shouldNotCacheEmptyResults() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"fail\"}"));
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"fail\"}"));

        assertThat(service.resolveCountry("9.9.9.9")).isEmpty();
        assertThat(service.resolveCountry("9.9.9.9")).isEmpty();

        // hit BOTH times — empty result must not be cached
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
        assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNotNull();
    }
}