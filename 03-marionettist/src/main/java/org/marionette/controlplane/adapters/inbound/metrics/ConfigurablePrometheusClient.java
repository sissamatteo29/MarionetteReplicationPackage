package org.marionette.controlplane.adapters.inbound.metrics;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ConfigurablePrometheusClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PrometheusConfigurationResolver configResolver;
    private final MetricsConfiguration metricsConfig;

    public ConfigurablePrometheusClient(RestTemplate restTemplate, ObjectMapper objectMapper,
                                       PrometheusConfigurationResolver configResolver,
                                       MetricsConfiguration metricsConfig) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.configResolver = configResolver;
        this.metricsConfig = metricsConfig;
    }

    @PostConstruct
    public void initializePrometheusClient() {
        if (configResolver.isPrometheusAvailable() && metricsConfig.isEnabled()) {
            System.out.println("ConfigurablePrometheusClient initialized with " + 
                             metricsConfig.getQueries().size() + " metric queries configured");
            
            // Log configured queries for debugging
            metricsConfig.getQueries().forEach((key, config) -> {
                if (config.isEnabled()) {
                    System.out.println("  - " + key + ": " + config.getDisplayName());
                }
            });
        } else {
            System.out.println("Prometheus metrics disabled or unavailable");
        }
    }

    /**
     * Get all configured metrics for a service
     */
    public Map<String, List<TimeSeriesDataDTO>> getAllServiceMetrics(String serviceName, 
                                                                     Instant startTime, 
                                                                     Instant endTime, 
                                                                     String step) {
        if (!isAvailable()) {
            return Collections.emptyMap();
        }

        Map<String, List<TimeSeriesDataDTO>> results = new HashMap<>();
        
        metricsConfig.getQueries().forEach((metricKey, config) -> {
            if (config.isEnabled()) {
                try {
                    String query = substituteServiceName(config.getQuery(), serviceName);
                    List<TimeSeriesDataDTO> data = executeQuery(query, startTime, endTime, step);
                    if(data.isEmpty()) {
                        System.out.println("Found no data for the query { " + config.getQuery() + " }");
                    }
                    if (!data.isEmpty()) {
                        results.put(metricKey, data);
                        System.out.println("Found " + data.size() + " series for metric: " + metricKey);
                    } else {
                        System.out.println("No data found for metric: " + metricKey + " (query: " + query + ")");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch metric " + metricKey + ": " + e.getMessage());
                }
            }
        });
        
        return results;
    }

    /**
     * Get live/instant metrics for a service
     */
    public Map<String, Double> getLiveMetrics(String serviceName) {
        if (!isAvailable()) {
            return Collections.emptyMap();
        }

        Map<String, Double> results = new HashMap<>();
        
        metricsConfig.getQueries().forEach((metricKey, config) -> {
            if (config.isEnabled()) {
                try {
                    String query = substituteServiceName(config.getQuery(), serviceName);
                    Double value = executeInstantQuery(query);
                    
                    if (value != null) {
                        System.out.println("Executed query { " + query + " }, obtained the data { " + value);
                        results.put(metricKey, value);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch live metric " + metricKey + ": " + e.getMessage());
                }
            }
        });
        
        return results;
    }

    /**
     * Get available metrics that have data for a service (discovery mode)
     */
    public List<String> discoverAvailableMetrics(String serviceName) {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        List<String> availableMetrics = new ArrayList<>();
        
        metricsConfig.getQueries().forEach((metricKey, config) -> {
            try {
                String query = substituteServiceName(config.getQuery(), serviceName);
                Double testValue = executeInstantQuery(query);
                
                if (testValue != null) {
                    availableMetrics.add(metricKey);
                }
            } catch (Exception e) {
                // Ignore errors during discovery
            }
        });
        
        return availableMetrics;
    }

    /**
     * Execute a range query
     */
    private List<TimeSeriesDataDTO> executeQuery(String query, Instant startTime, Instant endTime, String step) {
        try {
            String url = String.format("%s/api/v1/query_range", configResolver.getPrometheusUrl());
            
            String queryString = String.format("query=%s&start=%d&end=%d&step=%s",
                    URLEncoder.encode(query, StandardCharsets.UTF_8.toString()),
                    startTime.getEpochSecond(),
                    endTime.getEpochSecond(),
                    URLEncoder.encode(step, StandardCharsets.UTF_8.toString()));

            String fullUrl = url + "?" + queryString;
            System.out.println("Executing query: " + query);

            String response = restTemplate.getForObject(fullUrl, String.class);
            return parsePrometheusResponse(response);

        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Execute an instant query
     */
    private Double executeInstantQuery(String query) {
        try {
            String url = String.format("%s/api/v1/query?query=%s",
                    configResolver.getPrometheusUrl(),
                    URLEncoder.encode(query, StandardCharsets.UTF_8.toString()));

            String response = restTemplate.getForObject(url, String.class);
            return parseInstantResponse(response);

        } catch (Exception e) {
            System.err.println("Error executing instant query: " + e.getMessage());
            return null;
        }
    }

    private Double parseInstantResponse(String response) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            
            String status = root.path("status").asText();
            if (!"success".equals(status)) {
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode result = root.path("data").path("result");
            
            if (result.isArray() && result.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode value = result.get(0).path("value");
                if (value.isArray() && value.size() > 1) {
                    return value.get(1).asDouble();
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing instant response: " + e.getMessage());
        }
        return null;
    }

    private List<TimeSeriesDataDTO> parsePrometheusResponse(String response) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);

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
                    long timestamp = value.get(0).asLong() * 1000;
                    double val = Double.parseDouble(value.get(1).asText());
                    dataPoints.add(new TimeSeriesDataDTO.DataPoint(timestamp, val));
                }

                timeSeriesData.add(new TimeSeriesDataDTO(metricName, dataPoints));
            }

            return timeSeriesData;

        } catch (Exception e) {
            System.err.println("Error parsing Prometheus response: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String extractMetricName(com.fasterxml.jackson.databind.JsonNode metric) {
        // Try to find the most meaningful label
        String[] preferredLabels = {"__name__", "job", "service", "instance", "container"};
        
        for (String label : preferredLabels) {
            String value = metric.path(label).asText("");
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "unknown";
    }

    /**
     * Replace {service} placeholder in queries with actual service name
     */
    private String substituteServiceName(String queryTemplate, String serviceName) {
        return queryTemplate.replace("{service}", serviceName);
    }

    private boolean isAvailable() {
        return configResolver.isPrometheusAvailable() && metricsConfig.isEnabled();
    }

    // Public methods for backward compatibility and debugging
    public boolean isPrometheusAvailable() {
        return isAvailable();
    }

    public String getPrometheusConfiguration() {
        if (isAvailable()) {
            return configResolver.getPrometheusUrl() + " (" + 
                   metricsConfig.getQueries().size() + " metrics configured)";
        }
        return "Prometheus not configured or metrics disabled";
    }

    /**
     * Get metric configuration for UI purposes
     */
    public Map<String, MetricsConfiguration.MetricQueryConfig> getMetricsConfiguration() {
        return metricsConfig.getQueries().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Test all configured queries against a service to see which ones return data
     */
    public Map<String, Boolean> testQueries(String serviceName) {
        Map<String, Boolean> results = new HashMap<>();
        
        metricsConfig.getQueries().forEach((key, config) -> {
            if (config.isEnabled()) {
                try {
                    String query = substituteServiceName(config.getQuery(), serviceName);
                    Double value = executeInstantQuery(query);
                    results.put(key, value != null);
                } catch (Exception e) {
                    results.put(key, false);
                }
            }
        });
        
        return results;
    }
}