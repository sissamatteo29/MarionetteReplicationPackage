package org.marionette.controlplane.usecases.inbound.abntest.ranking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.SystemMetricsDataPoint;

public class SystemMetricsAggregator {

    /**
     * Aggregates service metrics to system level using average
     */
    public List<AggregateMetric> aggregateByAverage(SystemMetricsDataPoint systemData) {
        if (systemData.serviceMetrics().isEmpty()) {
            return List.of();
        }
        
        System.out.println("Aggregating metrics from " + systemData.serviceMetrics().size() + " services");
        
        // Group all metrics by name across all services
        Map<String, List<AggregateMetric>> metricsByName = systemData.serviceMetrics().stream()
            .flatMap(service -> service.metrics().stream())
            .collect(Collectors.groupingBy(AggregateMetric::name));
        
        List<AggregateMetric> aggregatedMetrics = new ArrayList<>();
        
        for (Map.Entry<String, List<AggregateMetric>> entry : metricsByName.entrySet()) {
            String metricName = entry.getKey();
            List<AggregateMetric> metricsForName = entry.getValue();
            
            if (!metricsForName.isEmpty()) {
                AggregateMetric aggregated = aggregateMetricsWithSameName(metricName, metricsForName);
                aggregatedMetrics.add(aggregated);
                
                System.out.println("  " + metricName + ": averaged " + metricsForName.size() + 
                                 " values, result = " + aggregated.value());
            }
        }
        
        System.out.println("Aggregated to " + aggregatedMetrics.size() + " system-level metrics");
        return aggregatedMetrics;
    }
    
    /**
     * Aggregates multiple metrics with the same name using average
     */
    private AggregateMetric aggregateMetricsWithSameName(String metricName, List<AggregateMetric> metrics) {
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate empty metrics list");
        }
        
        if (metrics.size() == 1) {
            return metrics.get(0); // No aggregation needed
        }
        
        // Calculate average value
        double averageValue = metrics.stream()
            .mapToDouble(AggregateMetric::value)
            .average()
            .orElse(0.0);
        
        // Use most recent timestamp
        Instant latestTimestamp = metrics.stream()
            .map(AggregateMetric::timestamp)
            .max(Instant::compareTo)
            .orElse(Instant.now());
        
        // Use unit from first metric (assuming all metrics with same name have same unit)
        String unit = metrics.get(0).unit();
        
        // Validate units are consistent (optional but recommended)
        boolean inconsistentUnits = metrics.stream()
            .anyMatch(m -> !Objects.equals(m.unit(), unit));
        
        if (inconsistentUnits) {
            System.out.println("WARNING: Inconsistent units for metric " + metricName);
        }
        
        return new AggregateMetric(metricName, averageValue, latestTimestamp, unit);
    }
    
    /**
     * Alternative aggregation strategies (for future extensibility)
     */
    public List<AggregateMetric> aggregateBySum(SystemMetricsDataPoint systemData) {
        // Implementation for sum aggregation
        // Useful for metrics like "total requests" across services
        return List.of(); // Placeholder
    }
    
    public List<AggregateMetric> aggregateByMax(SystemMetricsDataPoint systemData) {
        // Implementation for max aggregation  
        // Useful for metrics like "worst response time" across services
        return List.of(); // Placeholder
    }
    
    public List<AggregateMetric> aggregateByMin(SystemMetricsDataPoint systemData) {
        // Implementation for min aggregation
        // Useful for metrics like "best response time" across services
        return List.of(); // Placeholder
    }
}
