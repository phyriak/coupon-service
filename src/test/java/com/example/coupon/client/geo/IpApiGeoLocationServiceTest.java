package com.example.coupon.client.geo;

import com.example.coupon.config.IpApiProperties;
import com.example.coupon.domain.model.Country;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IpApiGeoLocationServiceTest {

    private MockWebServer mockWebServer;

    @Mock
    private RestClient restClient;

    @Mock
    private IpApiProperties properties;

    private IpApiGeoLocationService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        IpApiProperties properties = new IpApiProperties(
                mockWebServer.url("/json/{ip}?fields=countryCode,status").toString(),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        service = new IpApiGeoLocationService(restClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnUnknownWhenApiReturnsNonSuccess() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {"status":"fail","countryCode":null}
                        """)
                .addHeader("Content-Type", "application/json"));

        assertThat(service.resolveCountry("1.2.3.4")).isEqualTo(Country.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownWhenApiReturnsNull() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("null")
                .addHeader("Content-Type", "application/json"));

        assertThat(service.resolveCountry("1.2.3.4")).isEqualTo(Country.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownForUnrecognizedCountryCode() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {"status":"success","countryCode":"XX"}
                        """)
                .addHeader("Content-Type", "application/json"));

        assertThat(service.resolveCountry("1.2.3.4")).isEqualTo(Country.UNKNOWN);
    }
}