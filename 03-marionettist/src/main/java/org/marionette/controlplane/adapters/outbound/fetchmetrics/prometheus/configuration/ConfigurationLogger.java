package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration;

import java.util.List;
import java.util.stream.Collectors;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.domain.PrometheusMetricConfig;

public class ConfigurationLogger {

    public void logLoadStart() {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║              LOADING PROMETHEUS CONFIGURATION                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }

    public void logUrlDiscoveryResult(String prometheusUrl) {
        System.out.println("\n🔍 Prometheus URL Discovery:");
        if (prometheusUrl.isEmpty()) {
            System.out.println("   ⚠️  WARNING: No Prometheus URL found in environment variables");
            System.out.println("   📝 Consider setting: PROMETHEUS_URL, PROMETHEUS_ENDPOINT, etc.");
        } else {
            System.out.println("   ✅ Found Prometheus URL: " + prometheusUrl);
        }
    }

    public void logMetricsParsingResult(List<PrometheusMetricConfig> metrics) {
        System.out.println("\n📊 Metrics Configuration Discovery:");
        System.out.println("   🎯 Found " + metrics.size() + " metric configurations");

        if (metrics.isEmpty()) {
            System.out.println("   ⚠️  WARNING: No valid metric configurations found!");
            System.out.println("   📝 Make sure environment variables follow the pattern:");
            System.out.println("      MARIONETTE_METRICS_CONFIG_<METRIC_NAME>_<PROPERTY>");
        } else {
            System.out.println("   📋 Loaded metrics:");
            metrics.forEach(config -> {
                String status = isCompleteConfig(config) ? "✅" : "⚠️";
                System.out.println("      " + status + " " + config.getDisplayName() +
                        " (order: " + config.getOrder() + ")");
            });
        }
    }

    public void logServicesParsingResult(List<String> includedServices) {
        System.out.println("\n🔗 Included Services Discovery:");
        if (includedServices.isEmpty()) {
            System.out.println("   ℹ️  No included services configured");
            System.out.println("   📝 To include services, set: MARIONETTE_METRICS_INCLUDED_SERVICES");
        } else {
            System.out.println("   ✅ Found " + includedServices.size() + " included services:");
            includedServices.forEach(service -> System.out.println("      • " + service));
        }
    }

    public void logFinalConfiguration(PrometheusConfiguration configuration) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    CONFIGURATION SUMMARY                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        // URL Status
        String urlStatus = configuration.getPrometheusUrl().isEmpty() ? "❌ NOT CONFIGURED"
                : "✅ " + configuration.getPrometheusUrl();
        System.out.println("🌐 Prometheus URL: " + urlStatus);

        // Metrics Status
        System.out.println("📊 Total Metrics: " + configuration.getMetrics().size());
        if (!configuration.getMetrics().isEmpty()) {
            String orderSequence = configuration.getMetrics().stream()
                    .map(config -> String.valueOf(config.getOrder()))
                    .collect(Collectors.joining(" → "));
            System.out.println("🔢 Metric Order: " + orderSequence);
        }

        // Services Status
        System.out.println("🔗 Included Services: " + configuration.getIncludedServices().size());

        // Overall Status
        if (isValidConfiguration(configuration)) {
            System.out.println("✅ Configuration loaded successfully!");
        } else {
            System.out.println("⚠️  Configuration loaded with warnings - check logs above");
        }

        System.out.println("\n" + configuration);

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║            PROMETHEUS CONFIGURATION LOAD COMPLETE             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }

    public void logError(String message, Exception e) {
        System.err.println("\n❌ ERROR: " + message);
        System.err.println("   Exception: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("   Caused by: " + e.getCause().getMessage());
        }
    }

    private boolean isCompleteConfig(PrometheusMetricConfig config) {
        return config.getQuery() != null && !config.getQuery().trim().isEmpty() &&
                config.getDisplayName() != null && !config.getDisplayName().trim().isEmpty();
    }

    private boolean isValidConfiguration(PrometheusConfiguration configuration) {
        return !configuration.getPrometheusUrl().isEmpty() &&
                !configuration.getMetrics().isEmpty();
    }
}