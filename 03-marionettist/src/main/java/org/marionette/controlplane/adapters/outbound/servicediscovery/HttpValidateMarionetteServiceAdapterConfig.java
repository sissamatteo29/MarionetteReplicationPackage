package org.marionette.controlplane.adapters.outbound.servicediscovery;

import java.time.Duration;

public record HttpValidateMarionetteServiceAdapterConfig(
    String validationEndpointPath,
    Duration connectTimeout,
    Duration requestTimeout,
    int maxRetries
) {
    
    public static HttpValidateMarionetteServiceAdapterConfig defaultConfig() {
        return new HttpValidateMarionetteServiceAdapterConfig(
            "/marionette/api/isMarionette",
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            3
        );
    }

    public static HttpValidateMarionetteServiceAdapterConfig fromExternalConfigs(String validationEndpointPath) {
        return new HttpValidateMarionetteServiceAdapterConfig(validationEndpointPath, Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            3);
    }
}