package com.fleetlens.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest request(String uri) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", uri);
        return new ServletWebRequest(servletRequest);
    }

    @Test
    void mapsNoSuchElementExceptionTo404WithMessagePreserved() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new NoSuchElementException("No version 5 for topic order-events"), request("/api/v1/schema/topics/order-events/diff"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("No version 5 for topic order-events");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void mapsIllegalArgumentExceptionTo400() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new IllegalArgumentException("targetBaseUrl is required"), request("/api/v1/traces/x/replay"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("targetBaseUrl is required");
    }

    @Test
    void mapsUnexpectedExceptionTo500WithoutLeakingInternalDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new RuntimeException("Connection refused: connect"), request("/api/v1/config/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().message()).doesNotContain("Connection refused");
    }
}
