package org.marionette.controlplane.adapters.inbound.controllers;

import org.marionette.controlplane.adapters.inbound.metrics.ConfigurablePrometheusClient;
import org.marionette.controlplane.adapters.inbound.metrics.MetricsConfiguration;
import org.marionette.controlplane.adapters.inbound.metrics.TimeSeriesDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class ConfigurableMetricsController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurableMetricsController.class);
    private final ConfigurablePrometheusClient prometheusClient;
    private final MetricsConfiguration metricsConfig;

    public ConfigurableMetricsController(ConfigurablePrometheusClient prometheusClient, 
                                        MetricsConfiguration metricsConfig) {
        this.prometheusClient = prometheusClient;
        this.metricsConfig = metricsConfig;
    }

    /**
     * GET /api/metrics/{serviceName} - Get all configured metrics for a service
     */
    @GetMapping("/{serviceName}")
    public ResponseEntity<ConfigurableMetricsResponse> getServiceMetrics(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "15") int minutes) {
        
        try {
            logger.info("Fetching configurable metrics for service: {}, time range: {} minutes", 
                       serviceName, minutes);
            
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(minutes, ChronoUnit.MINUTES);
            String step = calculateStep(minutes);
            
            // Get all configured metrics
            Map<String, List<TimeSeriesDataDTO>> allMetrics = prometheusClient
                .getAllServiceMetrics(serviceName, startTime, endTime, step);
            
            // Get metric configurations for the response
            Map<String, MetricsConfiguration.MetricQueryConfig> configs = prometheusClient
                .getMetricsConfiguration();
            
            ConfigurableMetricsResponse response = new ConfigurableMetricsResponse(
                serviceName,
                allMetrics,
                configs,
                startTime,
                endTime,
                step
            );

            logger.info("Successfully fetched {} metrics for service: {}", 
                       allMetrics.size(), serviceName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to fetch metrics for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/metrics/{serviceName}/live - Get current metric values
     */
    @GetMapping("/{serviceName}/live")
    public ResponseEntity<Map<String, Object>> getLiveMetrics(@PathVariable String serviceName) {
        try {
            logger.info("Fetching live metrics for service: {}", serviceName);
            
            Map<String, Double> liveValues = prometheusClient.getLiveMetrics(serviceName);
            Map<String, MetricsConfiguration.MetricQueryConfig> configs = prometheusClient
                .getMetricsConfiguration();
            
            // Combine values with their configurations
            Map<String, Object> response = new HashMap<>();
            response.put("serviceName", serviceName);
            response.put("timestamp", Instant.now().toString());
            response.put("metrics", liveValues);
            response.put("configurations", configs);
            
            logger.info("Live metrics for {}: {} values found", serviceName, liveValues.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to fetch live metrics for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/metrics/{serviceName}/discover - Discover which metrics have data
     */
    @GetMapping("/{serviceName}/discover")
    public ResponseEntity<MetricsDiscoveryResponse> discoverMetrics(@PathVariable String serviceName) {
        try {
            logger.info("Discovering available metrics for service: {}", serviceName);
            
            List<String> availableMetrics = prometheusClient.discoverAvailableMetrics(serviceName);
            Map<String, Boolean> testResults = prometheusClient.testQueries(serviceName);
            Map<String, MetricsConfiguration.MetricQueryConfig> allConfigs = prometheusClient
                .getMetricsConfiguration();
            
            MetricsDiscoveryResponse response = new MetricsDiscoveryResponse(
                serviceName,
                availableMetrics,
                testResults,
                allConfigs,
                Instant.now()
            );
            
            logger.info("Discovery for {}: {}/{} metrics have data", 
                       serviceName, availableMetrics.size(), allConfigs.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to discover metrics for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/metrics/config - Get current metrics configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMetricsConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", metricsConfig.isEnabled());
            config.put("defaultTimeRangeMinutes", metricsConfig.getDefaultTimeRangeMinutes());
            config.put("defaultStep", metricsConfig.getDefaultStep());
            config.put("queries", metricsConfig.getQueries());
            config.put("prometheusUrl", prometheusClient.getPrometheusConfiguration());
            config.put("totalQueries", metricsConfig.getQueries().size());
            config.put("enabledQueries", metricsConfig.getQueries().values().stream()
                .mapToInt(q -> q.isEnabled() ? 1 : 0).sum());
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("Failed to get metrics configuration", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/metrics/{serviceName}/test - Test specific metric query
     */
    @PostMapping("/{serviceName}/test")
    public ResponseEntity<Map<String, Object>> testMetricQuery(
            @PathVariable String serviceName,
            @RequestParam String metricKey) {
        
        try {
            Map<String, Boolean> testResults = prometheusClient.testQueries(serviceName);
            MetricsConfiguration.MetricQueryConfig config = metricsConfig.getQueries().get(metricKey);
            
            if (config == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown metric key: " + metricKey
                ));
            }
            
            Boolean hasData = testResults.get(metricKey);
            String substitutedQuery = config.getQuery().replace("{service}", serviceName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serviceName", serviceName);
            response.put("metricKey", metricKey);
            response.put("hasData", hasData != null ? hasData : false);
            response.put("query", substitutedQuery);
            response.put("config", config);
            response.put("timestamp", Instant.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to test metric query for {}: {}", serviceName, metricKey, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Diagnostic endpoint to check Prometheus connectivity and configuration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean available = prometheusClient.isPrometheusAvailable();
            String config = prometheusClient.getPrometheusConfiguration();
            
            Map<String, Object> health = new HashMap<>();
            health.put("prometheusAvailable", available);
            health.put("prometheusConfig", config);
            health.put("metricsEnabled", metricsConfig.isEnabled());
            health.put("totalQueries", metricsConfig.getQueries().size());
            health.put("enabledQueries", metricsConfig.getQueries().values().stream()
                .mapToInt(q -> q.isEnabled() ? 1 : 0).sum());
            health.put("timestamp", Instant.now().toString());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Health check failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to calculate appropriate step based on time range
     */
    private String calculateStep(int minutes) {
        if (minutes <= 5) {
            return "15s";
        } else if (minutes <= 15) {
            return "30s";
        } else if (minutes <= 60) {
            return "1m";
        } else {
            return "5m";
        }
    }

    // Response DTOs
    public static class ConfigurableMetricsResponse {
        private final String serviceName;
        private final Map<String, List<TimeSeriesDataDTO>> metrics;
        private final Map<String, MetricsConfiguration.MetricQueryConfig> configurations;
        private final Instant startTime;
        private final Instant endTime;
        private final String step;

        public ConfigurableMetricsResponse(String serviceName,
                                         Map<String, List<TimeSeriesDataDTO>> metrics,
                                         Map<String, MetricsConfiguration.MetricQueryConfig> configurations,
                                         Instant startTime, Instant endTime, String step) {
            this.serviceName = serviceName;
            this.metrics = metrics;
            this.configurations = configurations;
            this.startTime = startTime;
            this.endTime = endTime;
            this.step = step;
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public Map<String, List<TimeSeriesDataDTO>> getMetrics() { return metrics; }
        public Map<String, MetricsConfiguration.MetricQueryConfig> getConfigurations() { return configurations; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public String getStep() { return step; }
    }

    public static class MetricsDiscoveryResponse {
        private final String serviceName;
        private final List<String> availableMetrics;
        private final Map<String, Boolean> testResults;
        private final Map<String, MetricsConfiguration.MetricQueryConfig> allConfigurations;
        private final Instant discoveryTime;

        public MetricsDiscoveryResponse(String serviceName, List<String> availableMetrics,
                                      Map<String, Boolean> testResults,
                                      Map<String, MetricsConfiguration.MetricQueryConfig> allConfigurations,
                                      Instant discoveryTime) {
            this.serviceName = serviceName;
            this.availableMetrics = availableMetrics;
            this.testResults = testResults;
            this.allConfigurations = allConfigurations;
            this.discoveryTime = discoveryTime;
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public List<String> getAvailableMetrics() { return availableMetrics; }
        public Map<String, Boolean> getTestResults() { return testResults; }
        public Map<String, MetricsConfiguration.MetricQueryConfig> getAllConfigurations() { return allConfigurations; }
        public Instant getDiscoveryTime() { return discoveryTime; }
    }
}