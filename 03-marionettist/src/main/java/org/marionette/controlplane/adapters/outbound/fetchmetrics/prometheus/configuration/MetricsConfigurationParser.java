package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.domain.PrometheusMetricConfig;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.OptimizationDirection;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.ServiceAggregator;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.TimeAggregator;

public class MetricsConfigurationParser {

    private static final Pattern METRICS_PATTERN = Pattern.compile(
            "^MARIONETTE_METRICS_CONFIG_([A-Z_]+)_(QUERY|TIMEAGGREGATOR|SERVICEAGGREGATOR|ORDER|DIRECTION|DISPLAYNAME|UNIT|DESCRIPTION)$");

    public List<PrometheusMetricConfig> parseMetricsFromEnvironment() {
        Map<String, PrometheusMetricConfig> metricsMap = new LinkedHashMap<>();

        System.getenv().forEach((envKey, envValue) -> {
            parseEnvironmentVariable(envKey, envValue, metricsMap);
        });

        return sortMetricsByOrder(metricsMap);
    }

    private void parseEnvironmentVariable(String envKey, String envValue,
            Map<String, PrometheusMetricConfig> metricsMap) {
        Matcher matcher = METRICS_PATTERN.matcher(envKey);
        if (!matcher.matches()) {
            return; // Skip non-matching environment variables
        }

        String metricKey = matcher.group(1).toLowerCase();
        String property = matcher.group(2).toLowerCase();

        PrometheusMetricConfig config = metricsMap.computeIfAbsent(metricKey, k -> new PrometheusMetricConfig());
        setConfigProperty(config, property, envValue);
    }

    private void setConfigProperty(PrometheusMetricConfig config, String property, String value) {
        switch (property) {
            case "query":
                config.setQuery(value);
                break;
            case "timeaggregator":
                config.setTimeAggregator(TimeAggregator.fromString(value));
                break;
            case "serviceaggregator":
                config.setServiceAggregator(ServiceAggregator.fromString(value));
                break;
            case "order":
                config.setOrder(parseIntegerSafely(value));
                break;
            case "direction":
                config.setDirection(OptimizationDirection.fromString(value));
                break;
            case "displayname":
                config.setDisplayName(value);
                break;
            case "unit":
                config.setUnit(value);
                break;
            case "description":
                config.setDescription(value);
                break;
            default:
                // Unknown property - could log warning here
                break;
        }
    }

    private int parseIntegerSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0; // Default order
        }
    }

    private List<PrometheusMetricConfig> sortMetricsByOrder(Map<String, PrometheusMetricConfig> metricsMap) {
        return metricsMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getValue().getOrder(),
                        b.getValue().getOrder()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public boolean isValidMetricConfiguration(PrometheusMetricConfig config) {
        return config.getQuery() != null && !config.getQuery().trim().isEmpty() &&
                config.getDisplayName() != null && !config.getDisplayName().trim().isEmpty();
    }
}