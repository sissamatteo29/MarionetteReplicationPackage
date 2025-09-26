package org.marionette.controlplane.adapters.inbound.metrics;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Component
public class KubernetesPrometheusDiscovery {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Common Prometheus service patterns in Kubernetes
    private final List<String> COMMON_PROMETHEUS_PATTERNS = Arrays.asList(
        "http://prometheus:9090",
        "http://prometheus-server:9090", 
        "http://prometheus-service:9090",
        "http://kube-prometheus-prometheus:9090",
        "http://prometheus-operator-prometheus:9090",
        "http://monitoring-prometheus:9090",
        "http://prometheus-kube-prometheus-prometheus:9090"
    );

    public KubernetesPrometheusDiscovery(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Discover Prometheus instance in the Kubernetes cluster
     */
    public String discoverPrometheus() {
        System.out.println("Starting Prometheus auto-discovery...");
        
        // Method 1: Try common service names
        for (String prometheusUrl : COMMON_PROMETHEUS_PATTERNS) {
            if (testPrometheusConnection(prometheusUrl)) {
                System.out.println("Found Prometheus at: " + prometheusUrl);
                return prometheusUrl;
            }
        }
        
        // Method 2: Query Kubernetes API for services (if we have permissions)
        String kubernetesDiscovered = discoverViaKubernetesAPI();
        if (kubernetesDiscovered != null) {
            return kubernetesDiscovered;
        }
        
        // Method 3: Try localhost (for development/port-forward scenarios)
        String localhostUrl = "http://localhost:9090";
        if (testPrometheusConnection(localhostUrl)) {
            System.out.println("Found Prometheus at: " + localhostUrl);
            return localhostUrl;
        }
        
        System.err.println("Could not auto-discover Prometheus. Please configure prometheus.url manually.");
        return null;
    }

    private boolean testPrometheusConnection(String prometheusUrl) {
        try {
            String testUrl = prometheusUrl + "/api/v1/query?query=up";
            String response = restTemplate.getForObject(testUrl, String.class);
            
            // Parse response to ensure it's valid Prometheus
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            
            return "success".equals(status);
            
        } catch (ResourceAccessException e) {
            // Connection failed - service not available
            return false;
        } catch (Exception e) {
            // Other errors (parsing, etc.) - probably not Prometheus
            return false;
        }
    }

    private String discoverViaKubernetesAPI() {
        try {
            // Check if we're running inside a pod
            if (!Files.exists(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token"))) {
                return null;
            }

            String kubernetesApiUrl = "https://kubernetes.default.svc.cluster.local/api/v1/services";
            String token = new String(Files.readAllBytes(
                Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")
            ));

            // This is a simplified approach - in practice you'd need proper SSL handling
            // and better error handling for the Kubernetes API
            
            System.out.println("Attempting Kubernetes API discovery (this may require additional RBAC permissions)...");
            
            // For now, return null and rely on the common patterns
            // You could implement full Kubernetes API integration here if needed
            return null;
            
        } catch (IOException e) {
            System.out.println("Not running in Kubernetes pod or no service account token available");
            return null;
        }
    }

    /**
     * Alternative discovery method using environment variables
     * Useful when Prometheus URL is injected via ConfigMap/Secret
     */
    public String discoverViaEnvironment() {
        String[] envVars = {
            "PROMETHEUS_URL",
            "PROMETHEUS_SERVICE_URL", 
            "MONITORING_PROMETHEUS_URL"
        };
        
        for (String envVar : envVars) {
            String url = System.getenv(envVar);
            if (url != null && !url.isEmpty()) {
                if (testPrometheusConnection(url)) {
                    return url;
                }
            }
        }
        
        return null;
    }
}