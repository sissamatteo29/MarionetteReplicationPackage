package org.marionette.controlplane.adapters.outbound.servicediscovery;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.marionette.controlplane.usecases.domain.dto.DiscoveredServiceMetadata;
import org.marionette.controlplane.usecases.outbound.servicediscovery.ValidateMarionetteServicePort;

import static java.util.Objects.requireNonNull;

public class HttpValidateMarionetteServiceAdapter implements ValidateMarionetteServicePort {
    
    private final HttpClient httpClient;
    private final HttpValidateMarionetteServiceAdapterConfig config;
    
    public HttpValidateMarionetteServiceAdapter(HttpValidateMarionetteServiceAdapterConfig config) {

        requireNonNull(config, "The configuration for the http validator of marionette nodes is null");

        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build();
    }
    
    public HttpValidateMarionetteServiceAdapter() {
        this(HttpValidateMarionetteServiceAdapterConfig.defaultConfig());
    }
    
    @Override
    public boolean validateCandidateNode(DiscoveredServiceMetadata candidate) {
        return isValidMarionetteService(candidate.endpoint());
    }
    
    public boolean isValidMarionetteService(String serviceBaseUrl) {
        URI baseEndpoint = URI.create(serviceBaseUrl);
        URI validationEndpoint = baseEndpoint.resolve(config.validationEndpointPath());
        
        System.out.println("Validating the marionette candidate " + validationEndpoint);
        
        for (int attempt = 1; attempt <= config.maxRetries(); attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(validationEndpoint)
                    .timeout(config.requestTimeout())
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    System.out.println("The service " + baseEndpoint + " responded successfully to the probing");
                    return true;
                }
                
                if (statusCode >= 400 && statusCode < 500) {
                    System.out.println("The service " + baseEndpoint + " returned an error code - discarding...");
                    return false;
                }
                
                // In case of 5xx errors -> retry

            } catch (Exception e) {
                // Any exception (timeout, connection error, etc.) - continue to retry
                // Could log here: System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
            }
        }

        System.out.println("The service " + baseEndpoint + " did not respond to any of the attempts - discarding...");

        return false;
    }
}