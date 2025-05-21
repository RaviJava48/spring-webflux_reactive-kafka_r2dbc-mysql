package com.r2dbc.order_service.exception_handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleException(WebExchangeBindException ex) {

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("status", HttpStatus.BAD_REQUEST.value());
        errorMap.put("error", "Validation Failed");

        Map<String, String> fieldErrorsMap = new HashMap<>();
        ex.getFieldErrors().forEach(fieldError -> {
            fieldErrorsMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        errorMap.put("message", fieldErrorsMap);

        LOGGER.info("Validation failed: {}", errorMap);

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap));
    }
}
