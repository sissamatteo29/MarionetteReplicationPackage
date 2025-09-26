package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Simple template-based Prometheus query builder.
 * 
 * Users provide queries with placeholders:
 * - {service} or <service-name> -> replaced with actual service name
 * - {timespan} or <time-slice> -> replaced with time duration (for rate
 * functions)
 * - {sampling} or <sampling-period> -> replaced with sampling period (for
 * averaging, smoothing)
 * 
 * Examples:
 * - "rate(http_requests_total{service=\"{service}\"}[{timespan}])"
 * - "histogram_quantile(0.90, sum by (le)
 * (rate(http_server_requests_seconds_bucket{service=\"<service-name>\"}[<time-slice>])))"
 * - "avg_over_time(jvm_memory_used_bytes{service=\"{service}\"}[{sampling}])"
 * - "sum(rate(jvm_memory_used_bytes{service=\"{service}\"}[{timespan}])) / 1024
 * / 1024"
 */
public class PrometheusQueryBuilder {

    private static final String SERVICE_PLACEHOLDER_1 = "{service}";
    private static final String SERVICE_PLACEHOLDER_2 = "<service-name>";
    private static final String TIMESPAN_PLACEHOLDER_1 = "{timespan}";
    private static final String TIMESPAN_PLACEHOLDER_2 = "<time-slice>";
    private static final String SAMPLING_PLACEHOLDER_1 = "{sampling}";
    private static final String SAMPLING_PLACEHOLDER_2 = "<sampling-period>";

    /**
     * Builds a complete Prometheus query URL from a user template.
     * 
     * @param prometheusUrl  Base Prometheus URL (e.g., "http://prometheus:9090")
     * @param apiPath        API path (e.g., "/api/v1/query")
     * @param queryTemplate  User-provided query with placeholders
     * @param serviceName    Service name to substitute
     * @param timespan       Time duration for the query (used for {timespan}
     *                       placeholders)
     * @param samplingPeriod Sampling period for averaging/smoothing (used for
     *                       {sampling} placeholders)
     * @return Complete encoded Prometheus query URL
     */
    public static String buildQuery(String prometheusUrl, String apiPath,
            String queryTemplate, String serviceName,
            Duration timespan, Duration samplingPeriod) {

        validateInputs(prometheusUrl, apiPath, queryTemplate, serviceName, timespan, samplingPeriod);

        // Substitute placeholders
        String processedQuery = substituteServiceName(queryTemplate, serviceName);
        processedQuery = substituteTimespan(processedQuery, timespan);
        processedQuery = substituteSamplingPeriod(processedQuery, samplingPeriod);

        // URL encode the query
        String encodedQuery = URLEncoder.encode(processedQuery, StandardCharsets.UTF_8);

        // Build final URL
        String finalUrl = prometheusUrl + apiPath + "?query=" + encodedQuery;

        logQueryBuilding(queryTemplate, processedQuery, finalUrl);

        return finalUrl;
    }

    /**
     * Substitute service name placeholders in the query template.
     */
    private static String substituteServiceName(String queryTemplate, String serviceName) {
        String result = queryTemplate;
        result = result.replace(SERVICE_PLACEHOLDER_1, serviceName);
        result = result.replace(SERVICE_PLACEHOLDER_2, serviceName);
        return result;
    }

    /**
     * Substitute timespan placeholders in the query template.
     */
    private static String substituteTimespan(String queryTemplate, Duration timespan) {
        if (timespan == null) {
            return queryTemplate; // No substitution if timespan is null
        }

        String prometheusTimeFormat = toPrometheusTimeFormat(timespan);
        String result = queryTemplate;
        result = result.replace(TIMESPAN_PLACEHOLDER_1, prometheusTimeFormat);
        result = result.replace(TIMESPAN_PLACEHOLDER_2, prometheusTimeFormat);
        return result;
    }

    /**
     * Substitute sampling period placeholders in the query template.
     */
    private static String substituteSamplingPeriod(String queryTemplate, Duration samplingPeriod) {
        if (samplingPeriod == null) {
            return queryTemplate; // No substitution if samplingPeriod is null
        }

        String prometheusSamplingFormat = toPrometheusTimeFormat(samplingPeriod);
        String result = queryTemplate;
        result = result.replace(SAMPLING_PLACEHOLDER_1, prometheusSamplingFormat);
        result = result.replace(SAMPLING_PLACEHOLDER_2, prometheusSamplingFormat);
        return result;
    }

    /**
     * Converts Java Duration to Prometheus time format.
     */
    private static String toPrometheusTimeFormat(Duration duration) {
        long seconds = duration.getSeconds();

        if (seconds % 31536000 == 0) { // years (365d)
            return (seconds / 31536000) + "y";
        } else if (seconds % 604800 == 0) { // weeks
            return (seconds / 604800) + "w";
        } else if (seconds % 86400 == 0) { // days
            return (seconds / 86400) + "d";
        } else if (seconds % 3600 == 0) { // hours
            return (seconds / 3600) + "h";
        } else if (seconds % 60 == 0) { // minutes
            return (seconds / 60) + "m";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Validate all input parameters.
     */
    private static void validateInputs(String prometheusUrl, String apiPath,
            String queryTemplate, String serviceName,
            Duration timespan, Duration samplingPeriod) {
        Objects.requireNonNull(prometheusUrl, "Prometheus URL cannot be null");
        Objects.requireNonNull(apiPath, "API path cannot be null");
        Objects.requireNonNull(queryTemplate, "Query template cannot be null");
        Objects.requireNonNull(serviceName, "Service name cannot be null");
        Objects.requireNonNull(timespan, "Timespan cannot be null");
        // Note: samplingPeriod can be null - it's optional

        if (prometheusUrl.isBlank()) {
            throw new IllegalArgumentException("Prometheus URL cannot be blank");
        }
        if (queryTemplate.isBlank()) {
            throw new IllegalArgumentException("Query template cannot be blank");
        }
        if (serviceName.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be blank");
        }
        if (timespan.isNegative() || timespan.isZero()) {
            throw new IllegalArgumentException("Timespan must be positive");
        }
        if (samplingPeriod != null && (samplingPeriod.isNegative() || samplingPeriod.isZero())) {
            throw new IllegalArgumentException("Sampling period must be positive when provided");
        }
    }

    /**
     * Log the query building process for debugging.
     */
    private static void logQueryBuilding(String template, String processed, String finalUrl) {
        System.out.println("=== Prometheus Query Building ===");
        System.out.println("Template: " + template);
        System.out.println("Processed: " + processed);
        System.out.println("Final URL: " + finalUrl);
        System.out.println("================================");
    }

    /**
     * Utility method to check if a template contains valid placeholders.
     * Useful for validation during configuration.
     */
    public static boolean hasValidPlaceholders(String queryTemplate) {
        if (queryTemplate == null)
            return false;

        boolean hasServicePlaceholder = queryTemplate.contains(SERVICE_PLACEHOLDER_1) ||
                queryTemplate.contains(SERVICE_PLACEHOLDER_2);

        return hasServicePlaceholder; // Service placeholder is mandatory, others are optional
    }

    /**
     * Example usage and testing
     */
    public static void main(String[] args) {
        // Example 1: Rate query with timespan
        String rateTemplate = "rate(http_server_requests_seconds_count{service=\"{service}\"}[{timespan}])";
        String url1 = buildQuery(
                "http://localhost:9090",
                "/api/v1/query",
                rateTemplate,
                "imagestore-service",
                Duration.ofMinutes(5),
                null // No sampling period needed
        );

        // Example 2: Average over time with sampling period
        String avgTemplate = "avg_over_time(jvm_memory_used_bytes{area=\"heap\",service=\"{service}\"}[{sampling}])";
        String url2 = buildQuery(
                "http://localhost:9090",
                "/api/v1/query",
                avgTemplate,
                "imagestore-service",
                Duration.ofMinutes(1), // Required but unused in this query
                Duration.ofMinutes(10) // Used for sampling
        );

        // Example 3: Complex query with both timespan and sampling
        String complexTemplate = """
                (
                  rate(http_requests_total{service="{service}"}[{timespan}]) -
                  avg_over_time(rate(http_requests_total{service="{service}"}[{timespan}])[{sampling}:])
                ) / avg_over_time(rate(http_requests_total{service="{service}"}[{timespan}])[{sampling}:])
                """;
        String url3 = buildQuery(
                "http://localhost:9090",
                "/api/v1/query",
                complexTemplate,
                "imagestore-service",
                Duration.ofMinutes(1), // Short window for rate calculation
                Duration.ofMinutes(15) // Longer window for baseline averaging
        );

        // Example 4: 90th percentile with different placeholders style
        String percentileTemplate = "histogram_quantile(0.90, sum by (le) (rate(http_server_requests_seconds_bucket{service=\"<service-name>\"}[<time-slice>])))";
        String url4 = buildQuery(
                "http://localhost:9090",
                "/api/v1/query",
                percentileTemplate,
                "imagestore-service",
                Duration.ofMinutes(5),
                null);

        // Validation examples
        System.out.println("Template validation:");
        System.out.println("Valid rate template: " + hasValidPlaceholders(rateTemplate));
        System.out.println("Valid avg template: " + hasValidPlaceholders(avgTemplate));
        System.out.println("Invalid template: " + hasValidPlaceholders("sum(cpu_usage)"));
    }
}