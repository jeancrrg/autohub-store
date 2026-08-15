package com.autohubstore.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, String>> serviceFallback(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "503",
                        "error", "Service Unavailable",
                        "message", "The " + service + " service is temporarily unavailable. Please try again later.",
                        "service", service
                ));
    }

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, String>> genericFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "503",
                        "error", "Service Unavailable",
                        "message", "The requested service is temporarily unavailable. Please try again later."
                ));
    }

}
