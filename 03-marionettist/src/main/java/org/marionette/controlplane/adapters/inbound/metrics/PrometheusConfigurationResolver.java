package org.marionette.controlplane.adapters.inbound.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Component
public class PrometheusConfigurationResolver {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KubernetesPrometheusDiscovery prometheusDiscovery;
    
    // Configuration sources in priority order
    @Value("${prometheus.url:}")
    private String configuredPrometheusUrl;
    
    private String resolvedPrometheusUrl;
    private boolean prometheusAvailable = false;

    public PrometheusConfigurationResolver(RestTemplate restTemplate, ObjectMapper objectMapper, 
                                         KubernetesPrometheusDiscovery prometheusDiscovery) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.prometheusDiscovery = prometheusDiscovery;
    }

    @PostConstruct
    public void resolvePrometheusConfiguration() {
        System.out.println("=== Prometheus Configuration Resolution ===");
        
        // Priority 1: Environment Variable
        String envUrl = resolveFromEnvironment();
        if (envUrl != null) {
            System.out.println("Using Prometheus URL from environment: " + envUrl);
            resolvedPrometheusUrl = envUrl;
            prometheusAvailable = testConnection(envUrl);
            return;
        }
        
        // Priority 2: Application Properties
        if (configuredPrometheusUrl != null && !configuredPrometheusUrl.trim().isEmpty()) {
            System.out.println("Using Prometheus URL from application.properties: " + configuredPrometheusUrl);
            resolvedPrometheusUrl = configuredPrometheusUrl;
            prometheusAvailable = testConnection(configuredPrometheusUrl);
            return;
        }
        
        // Priority 3: Kubernetes ConfigMap/Secret
        String k8sUrl = resolveFromKubernetesConfig();
        if (k8sUrl != null) {
            System.out.println("Using Prometheus URL from Kubernetes config: " + k8sUrl);
            resolvedPrometheusUrl = k8sUrl;
            prometheusAvailable = testConnection(k8sUrl);
            return;
        }
        
        // Priority 4: Auto-discovery (fallback)
        System.out.println("No explicit Prometheus configuration found, attempting auto-discovery...");
        String discoveredUrl = prometheusDiscovery.discoverPrometheus();
        if (discoveredUrl != null) {
            System.out.println("Auto-discovered Prometheus at: " + discoveredUrl);
            resolvedPrometheusUrl = discoveredUrl;
            prometheusAvailable = true; // Discovery already tested the connection
            return;
        }
        
        // No Prometheus found
        System.err.println("No Prometheus instance could be found or configured.");
        System.err.println("To configure Prometheus manually, set one of:");
        System.err.println("  - Environment variable: PROMETHEUS_URL");
        System.err.println("  - Application property: prometheus.url");
        System.err.println("  - Kubernetes ConfigMap/Secret with prometheus-url key");
        prometheusAvailable = false;
    }

    /**
     * Priority 1: Check environment variables
     */
    private String resolveFromEnvironment() {
        List<String> envVarNames = Arrays.asList(
            "PROMETHEUS_URL",           // Primary
            "PROMETHEUS_ENDPOINT",      // Alternative
            "PROMETHEUS_SERVICE_URL",   // Kubernetes style
            "MONITORING_PROMETHEUS_URL" // Namespace-specific
        );
        
        for (String envVar : envVarNames) {
            String url = System.getenv(envVar);
            if (url != null && !url.trim().isEmpty()) {
                System.out.println("Found Prometheus URL in environment variable: " + envVar);
                return url.trim();
            }
        }
        
        return null;
    }

    /**
     * Priority 3: Check Kubernetes ConfigMap/Secret mounted files
     */
    private String resolveFromKubernetesConfig() {
        List<String> configPaths = Arrays.asList(
            "/etc/config/prometheus-url",           // ConfigMap mount
            "/etc/secrets/prometheus-url",          // Secret mount
            "/config/prometheus.url",               // Alternative path
            "/app/config/prometheus-endpoint"       // App-specific path
        );
        
        for (String path : configPaths) {
            try {
                if (Files.exists(Paths.get(path))) {
                    String url = Files.readString(Paths.get(path)).trim();
                    if (!url.isEmpty()) {
                        System.out.println("Found Prometheus URL in Kubernetes config: " + path);
                        return url;
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not read config file: " + path + " (" + e.getMessage() + ")");
            }
        }
        
        return null;
    }

    private boolean testConnection(String prometheusUrl) {
        try {
            String testUrl = prometheusUrl + "/api/v1/query?query=up";
            String response = restTemplate.getForObject(testUrl, String.class);
            
            // Parse response to ensure it's valid Prometheus
            if (response != null) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
                String status = root.path("status").asText();
                
                if ("success".equals(status)) {
                    System.out.println("Prometheus connection test successful");
                    return true;
                } else {
                    System.err.println("Prometheus returned non-success status: " + status);
                }
            }
        } catch (Exception e) {
            System.err.println("Prometheus connection test failed: " + e.getMessage());
        }
        
        return false;
    }

    // Getters
    public String getPrometheusUrl() {
        return resolvedPrometheusUrl;
    }

    public boolean isPrometheusAvailable() {
        return prometheusAvailable;
    }

    // For runtime reconfiguration (useful for admin endpoints)
    public boolean reconfigure(String newPrometheusUrl) {
        if (testConnection(newPrometheusUrl)) {
            resolvedPrometheusUrl = newPrometheusUrl;
            prometheusAvailable = true;
            System.out.println("Prometheus reconfigured to: " + newPrometheusUrl);
            return true;
        }
        return false;
    }
}