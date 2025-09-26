package org.marionette.controlplane.adapters.inbound.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
public class PrometheusClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PrometheusConfigurationResolver configResolver;

    public PrometheusClient(RestTemplate restTemplate, ObjectMapper objectMapper,
            PrometheusConfigurationResolver configResolver) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.configResolver = configResolver;
    }

    @PostConstruct
    public void initializePrometheusClient() {
        // Configuration is handled by PrometheusConfigurationResolver
        if (configResolver.isPrometheusAvailable()) {
            System.out.println("PrometheusClient initialized successfully with: " + configResolver.getPrometheusUrl());
        } else {
            System.out
                    .println("PrometheusClient initialized but Prometheus is not available - metrics will be disabled");
        }
    }

    /**
     * Query Prometheus for range data
     */
    public List<TimeSeriesDataDTO> queryRange(String query, Instant startTime, Instant endTime, String step) {
        if (!configResolver.isPrometheusAvailable()) {
            System.out.println("Prometheus not available - returning empty metrics");
            return Collections.emptyList();
        }

        try {
            String url = String.format("%s/api/v1/query_range", configResolver.getPrometheusUrl());

            // Manually build the query string to avoid URI template expansion
            String queryString = String.format("query=%s&start=%d&end=%d&step=%s",
                    URLEncoder.encode(query, StandardCharsets.UTF_8.toString()),
                    startTime.getEpochSecond(),
                    endTime.getEpochSecond(),
                    URLEncoder.encode(step, StandardCharsets.UTF_8.toString()));

            String fullUrl = url + "?" + queryString;
            System.out.println("Querying Prometheus: " + fullUrl);

            String response = restTemplate.getForObject(fullUrl, String.class);
            return parsePrometheusResponse(response);

        } catch (Exception e) {
            System.err.println("Error querying Prometheus: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Query for histogram percentiles (e.g., 95th percentile response time)
     */
    public List<TimeSeriesDataDTO> queryRangeHistogramPercentile(String metricName, String percentile,
            String serviceName, Instant startTime,
            Instant endTime, String step) {
        // Fixed: Use proper PromQL syntax for histogram quantiles
        String query = String.format("histogram_quantile(%s, sum(rate(%s_bucket{service=\"%s\"}[1m])) by (le))",
                percentile, metricName, serviceName);
        return queryRange(query, startTime, endTime, step);
    }

    /**
     * Query for request rate
     */
    public List<TimeSeriesDataDTO> queryRangeRate(String metricName, String serviceName,
            Instant startTime, Instant endTime, String step) {
        String query = String.format("rate(%s{service=\"%s\"}[1m])", metricName, serviceName);
        return queryRange(query, startTime, endTime, step);
    }

    /**
     * Query for error rate
     */
    public List<TimeSeriesDataDTO> queryRangeErrorRate(String metricName, String serviceName,
            Instant startTime, Instant endTime, String step) {
        String query = String.format(
                "rate(%s{service=\"%s\",status=~\"4..|5..\"}[1m]) / rate(%s{service=\"%s\"}[1m])",
                metricName, serviceName, metricName, serviceName);
        return queryRange(query, startTime, endTime, step);
    }

    /**
     * Query with method filter for method-specific metrics
     */
    public List<TimeSeriesDataDTO> queryRangeWithMethodFilter(String metricName, String serviceName,
            String methodName, Instant startTime,
            Instant endTime, String step) {
        String query = String.format("histogram_quantile(0.95, rate(%s_bucket{service=\"%s\",method=\"%s\"}[1m]))",
                metricName, serviceName, methodName);
        return queryRange(query, startTime, endTime, step);
    }

    /**
     * Query method-specific request rate
     */
    public List<TimeSeriesDataDTO> queryRangeRateWithMethodFilter(String metricName, String serviceName,
            String methodName, Instant startTime,
            Instant endTime, String step) {
        String query = String.format("rate(%s{service=\"%s\",method=\"%s\"}[1m])",
                metricName, serviceName, methodName);
        return queryRange(query, startTime, endTime, step);
    }

    /**
     * Get current metric values (instant query)
     */
    public Map<String, Double> getCurrentMetrics(String serviceName) {
        if (!configResolver.isPrometheusAvailable()) {
            return Collections.emptyMap();
        }

        Map<String, Double> metrics = new HashMap<>();

        try {
            // Current response time (95th percentile)
            String responseTimeQuery = String.format(
                    "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{service=\"%s\"}[1m]))",
                    serviceName);
            Double responseTime = queryInstant(responseTimeQuery);
            if (responseTime != null)
                metrics.put("responseTime", responseTime);

            // Current request rate
            String requestRateQuery = String.format("rate(http_requests_total{service=\"%s\"}[1m])", serviceName);
            Double requestRate = queryInstant(requestRateQuery);
            if (requestRate != null)
                metrics.put("requestRate", requestRate);

            // Current error rate
            String errorRateQuery = String.format(
                    "rate(http_requests_total{service=\"%s\",status=~\"4..|5..\"}[1m]) / rate(http_requests_total{service=\"%s\"}[1m])",
                    serviceName, serviceName);
            Double errorRate = queryInstant(errorRateQuery);
            if (errorRate != null)
                metrics.put("errorRate", errorRate);

        } catch (Exception e) {
            System.err.println("Error fetching current metrics: " + e.getMessage());
        }

        return metrics;
    }

    private Double queryInstant(String query) {
        if (!configResolver.isPrometheusAvailable())
            return null;

        try {
            String url = String.format("%s/api/v1/query?query=%s",
                    configResolver.getPrometheusUrl(),
                    java.net.URLEncoder.encode(query, "UTF-8"));

            String response = restTemplate.getForObject(url, String.class);
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode result = root.path("data").path("result");

            if (result.isArray() && result.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode value = result.get(0).path("value");
                if (value.isArray() && value.size() > 1) {
                    return value.get(1).asDouble();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in instant query: " + e.getMessage());
        }
        return null;
    }

    private List<TimeSeriesDataDTO> parsePrometheusResponse(String response) {
        if (!configResolver.isPrometheusAvailable())
            return Collections.emptyList();

        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);

            // Check if the response contains an error
            String status = root.path("status").asText();
            if (!"success".equals(status)) {
                String errorType = root.path("errorType").asText();
                String error = root.path("error").asText();
                System.err.println("Prometheus query failed - ErrorType: " + errorType + ", Error: " + error);
                return Collections.emptyList();
            }

            com.fasterxml.jackson.databind.JsonNode result = root.path("data").path("result");

            List<TimeSeriesDataDTO> timeSeriesData = new ArrayList<>();

            for (com.fasterxml.jackson.databind.JsonNode series : result) {
                String metricName = extractMetricName(series.path("metric"));
                com.fasterxml.jackson.databind.JsonNode values = series.path("values");

                List<TimeSeriesDataDTO.DataPoint> dataPoints = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode value : values) {
                    long timestamp = value.get(0).asLong() * 1000; // Convert to milliseconds
                    double val = Double.parseDouble(value.get(1).asText());
                    dataPoints.add(new TimeSeriesDataDTO.DataPoint(timestamp, val));
                }

                timeSeriesData.add(new TimeSeriesDataDTO(metricName, dataPoints));
            }

            return timeSeriesData;

        } catch (Exception e) {
            System.err.println("Error parsing Prometheus response: " + e.getMessage());
            System.err.println("Raw response: " + response); // Log the raw response for debugging
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String extractMetricName(com.fasterxml.jackson.databind.JsonNode metric) {
        // Extract a meaningful name from metric labels
        String job = metric.path("job").asText("");
        String instance = metric.path("instance").asText("");
        String service = metric.path("service").asText("");

        if (!service.isEmpty())
            return service;
        if (!job.isEmpty())
            return job;
        if (!instance.isEmpty())
            return instance;
        return "unknown";
    }

    public boolean isPrometheusAvailable() {
        return configResolver.isPrometheusAvailable();
    }

    // For admin/debugging - expose current configuration
    public String getPrometheusConfiguration() {
        if (configResolver.isPrometheusAvailable()) {
            return configResolver.getPrometheusUrl();
        }
        return "Prometheus not configured";
    }

}