package com.fleetlens.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real HTTP endpoint backing the "Capture demo trace" flow in the dashboard,
 * so trace replay has an actual target to hit instead of always 404ing.
 */
@RestController
public class OrderController {

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", UUID.randomUUID().toString());
        response.put("status", "CREATED");
        response.put("amount", request != null ? request.getOrDefault("amount", 42.5) : 42.5);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
