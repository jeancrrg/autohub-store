package com.autohubstore.gateway.model;

public record ServiceRouteDefinition(String serviceId, String pathPrefix, String serviceUrl) {

    public String fallbackName() {
        return serviceId.replace("-service", "");
    }

    public String circuitBreakerName() {
        return serviceId + "-cb";
    }

}
