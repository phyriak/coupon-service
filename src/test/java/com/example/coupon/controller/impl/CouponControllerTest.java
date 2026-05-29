package com.example.coupon.controller.impl;

import com.example.coupon.domain.exception.CountryNotAllowedException;
import com.example.coupon.domain.exception.CouponAlreadyUsedException;
import com.example.coupon.domain.exception.CouponLimitReachedException;
import com.example.coupon.domain.exception.CouponNotFoundException;
import com.example.coupon.domain.model.Country;
import com.example.coupon.dto.request.CreateCouponRequest;
import com.example.coupon.dto.request.UseCouponRequest;
import com.example.coupon.dto.response.CouponResponse;
import com.example.coupon.dto.response.UseCouponResponse;
import com.example.coupon.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    private static final String CREATE_COUPON = "/api/v1/coupons";
    private static final String USE_COUPON = "/api/v1/coupons/use";
    private static final Instant FIXED_TIME =
            Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    void shouldCreateCoupon() throws Exception {
        when(couponService.createCoupon(any()))
                .thenReturn(new CouponResponse(1L, "TEST100", 10, 0, Country.PL, FIXED_TIME));

        mockMvc.perform(post(CREATE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCouponRequest("TEST100", 10, Country.PL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEST100"))
                .andExpect(jsonPath("$.usageCount").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenCouponDoesNotExist() throws Exception {
        when(couponService.useCoupon(any(), anyString())).thenThrow(new CouponNotFoundException("TEST"));

        mockMvc.perform(post(USE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("TEST", "user-1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnGoneWhenCouponLimitReached() throws Exception {
        when(couponService.useCoupon(any(), anyString())).thenThrow(new CouponLimitReachedException("TEST10"));

        mockMvc.perform(post(USE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("TEST10", "user-1"))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("COUPON_LIMIT_REACHED"));
    }

    @Test
    void shouldReturnForbiddenWhenCountryNotAllowed() throws Exception {
        when(couponService.useCoupon(any(), anyString())).thenThrow(new CountryNotAllowedException(Country.US));

        mockMvc.perform(post(USE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("PLONLY", "user-1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void shouldReturnConflictWhenCouponAlreadyUsed() throws Exception {
        when(couponService.useCoupon(any(), anyString())).thenThrow(new CouponAlreadyUsedException("ONEUSE", "user-1"));

        mockMvc.perform(post(USE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("ONEUSE", "user-1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("COUPON_ALREADY_USED"));
    }

    @Test
    void shouldExtractFirstIpFromXForwardedForHeader() throws Exception {
        when(couponService.useCoupon(any(), anyString()))
                .thenReturn(UseCouponResponse.success("CODE", "user-1"));

        mockMvc.perform(post(USE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2, 3.3.3.3")
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("CODE", "user-1"))))
                .andExpect(status().isOk());

        verify(couponService).useCoupon(any(), eq("1.1.1.1"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post(CREATE_COUPON)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCouponRequest("", 0, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}