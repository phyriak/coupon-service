package com.example.coupon.controller;

import com.example.coupon.domain.exception.*;
import com.example.coupon.dto.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(CouponNotFoundException ex) {
        return ApiErrorResponse.of(404, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(CouponAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleAlreadyExists(CouponAlreadyExistsException ex) {
        return ApiErrorResponse.of(409, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(CouponLimitReachedException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiErrorResponse handleLimitReached(CouponLimitReachedException ex) {
        return ApiErrorResponse.of(410, "COUPON_LIMIT_REACHED", ex.getMessage());
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleCountryNotAllowed(CountryNotAllowedException ex) {
        return ApiErrorResponse.of(403, "COUNTRY_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(CouponAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleAlreadyUsed(CouponAlreadyUsedException ex) {
        return ApiErrorResponse.of(409, "COUPON_ALREADY_USED", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return ApiErrorResponse.of(400, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred");
    }
}
