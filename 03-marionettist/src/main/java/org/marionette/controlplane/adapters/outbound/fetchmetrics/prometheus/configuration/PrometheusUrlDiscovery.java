package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration;

import java.util.Arrays;
import java.util.List;

public class PrometheusUrlDiscovery {

    private static final List<String> PROMETHEUS_ENV_VARS = Arrays.asList(
            "PROMETHEUS_URL", // Primary
            "PROMETHEUS_ENDPOINT", // Alternative
            "PROMETHEUS_SERVICE_URL", // Kubernetes style
            "MONITORING_PROMETHEUS_URL" // Namespace-specific
    );

    public String discoverPrometheusUrl() {
        for (String envVar : PROMETHEUS_ENV_VARS) {
            String url = System.getenv(envVar);
            if (isValidUrl(url)) {
                return url.trim();
            }
        }
        return ""; // Empty string indicates no URL found
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.trim().isEmpty();
    }

    public List<String> getSupportedEnvironmentVariables() {
        return PROMETHEUS_ENV_VARS;
    }
}