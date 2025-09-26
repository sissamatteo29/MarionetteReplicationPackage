package org.marionette.controlplane.adapters.inbound.controllers;

import org.springframework.web.bind.annotation.*;
import org.marionette.controlplane.adapters.inbound.metrics.MetricsConfiguration;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.FetchMarionetteNodesMetricsGateway;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugConfigurationController {

    private final MetricsConfiguration metricsConfig;
    private final FetchMarionetteNodesMetricsGateway gateway;

    public DebugConfigurationController(MetricsConfiguration metricsConfig, FetchMarionetteNodesMetricsGateway gateway) {
        this.gateway = gateway;
        this.metricsConfig = metricsConfig;
    }

    /**
     * Debug endpoint to see exactly what configuration was loaded
     */
    @GetMapping("/metrics-config")
    public ResponseEntity<Map<String, Object>> getMetricsConfiguration() {
        Map<String, Object> response = new HashMap<>();
        
        // Basic configuration
        response.put("enabled", metricsConfig.isEnabled());
        response.put("defaultTimeRangeMinutes", metricsConfig.getDefaultTimeRangeMinutes());
        response.put("defaultStep", metricsConfig.getDefaultStep());
        
        // Queries configuration
        response.put("totalQueries", metricsConfig.getQueries().size());
        response.put("enabledQueries", metricsConfig.getQueries().values().stream()
            .mapToInt(q -> q.isEnabled() ? 1 : 0).sum());
        
        // All query details
        Map<String, Object> queryDetails = new HashMap<>();
        metricsConfig.getQueries().forEach((key, config) -> {
            Map<String, Object> details = new HashMap<>();
            details.put("displayName", config.getDisplayName());
            details.put("query", config.getQuery());
            details.put("unit", config.getUnit());
            details.put("description", config.getDescription());
            details.put("enabled", config.isEnabled());
            queryDetails.put(key, details);
        });
        response.put("queries", queryDetails);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint to see environment variables
     */
    @GetMapping("/env-vars")
    public ResponseEntity<Map<String, String>> getEnvironmentVariables() {
        // Filter only MARIONETTE_METRICS related variables
        Map<String, String> marionetteVars = System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("MARIONETTE_METRICS"))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                TreeMap::new // Sorted for easier reading
            ));
        
        return ResponseEntity.ok(marionetteVars);
    }

    /**
     * Debug endpoint to see configuration precedence
     */
    @GetMapping("/config-sources")
    public ResponseEntity<Map<String, Object>> getConfigurationSources() {
        Map<String, Object> response = new HashMap<>();
        
        // Environment variables
        Map<String, String> envVars = System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("MARIONETTE_METRICS"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        response.put("environmentVariables", envVars);
        
        // System properties
        Map<String, String> systemProps = System.getProperties().entrySet().stream()
            .filter(entry -> entry.getKey().toString().startsWith("marionette.metrics"))
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString(),
                entry -> entry.getValue().toString()
            ));
        response.put("systemProperties", systemProps);
        
        // Final resolved configuration
        response.put("resolvedConfiguration", Map.of(
            "enabled", metricsConfig.isEnabled(),
            "queriesCount", metricsConfig.getQueries().size()
        ));
        
        return ResponseEntity.ok(response);
    }


    @GetMapping("/fetch-metrics/{serviceName}/{duration}")
    public ResponseEntity<List<AggregateMetric>> tryAdapterToPrometheus(
        @PathVariable("serviceName") String serviceName,
        @PathVariable("duration") String duration
    ) {
        
        Duration javaDuration = Duration.parse(duration);
        Duration samplingPeriod = Duration.ofSeconds(1);
        return ResponseEntity.ok(gateway.fetchMetricsForService(serviceName, javaDuration, samplingPeriod));

    }


























}