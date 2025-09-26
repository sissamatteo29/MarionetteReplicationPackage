package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration;

import java.util.Collections;
import java.util.List;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.domain.PrometheusMetricConfig;

public class PrometheusConfiguration {

    private final String prometheusUrl;
    private final String internalPath = "/api/v1/query";
    private final List<PrometheusMetricConfig> metrics;
    private final List<String> includedServices;

    public PrometheusConfiguration(String prometheusUrl,
            List<PrometheusMetricConfig> metrics,
            List<String> includedServices) {
        this.prometheusUrl = prometheusUrl != null ? prometheusUrl : "";
        this.metrics = metrics != null ? List.copyOf(metrics) : Collections.emptyList();
        this.includedServices = includedServices != null ? List.copyOf(includedServices) : Collections.emptyList();
    }

    // Backward compatibility constructor
    public PrometheusConfiguration(String prometheusUrl, List<PrometheusMetricConfig> metrics) {
        this(prometheusUrl, metrics, Collections.emptyList());
    }

    public String getPrometheusUrl() {
        return prometheusUrl;
    }

    public List<PrometheusMetricConfig> getMetrics() {
        return metrics;
    }

    public List<String> getIncludedServices() {
        return includedServices;
    }

    public boolean isValid() {
        return !prometheusUrl.isEmpty() && !metrics.isEmpty();
    }

    @Override
    public String toString() {
        return String.format(
                "PrometheusConfiguration{url='%s', metrics=%d, includedServices=%d}",
                prometheusUrl,
                metrics.size(),
                includedServices.size());
    }

    public String getInternalPath() {
        return internalPath;
    }
}