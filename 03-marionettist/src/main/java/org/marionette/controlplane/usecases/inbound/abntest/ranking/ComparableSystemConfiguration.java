package org.marionette.controlplane.usecases.inbound.abntest.ranking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.OptimizationDirection;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.OrderedMetricMetadata;

public class ComparableSystemConfiguration implements Comparable<ComparableSystemConfiguration> {

    private final String configurationId;
    private final List<AggregateMetric> systemMetrics;
    private final List<OrderedMetricMetadata> metricOrder;
    
    // Create lookup map for fast metric access
    private final Map<String, AggregateMetric> metricsByName;
    
    public ComparableSystemConfiguration(
            String configurationId,
            List<AggregateMetric> systemMetrics, 
            List<OrderedMetricMetadata> metricOrder) {
        
        this.configurationId = configurationId;
        this.systemMetrics = List.copyOf(systemMetrics);
        this.metricOrder = List.copyOf(metricOrder);
        
        // Create lookup map for O(1) metric access
        this.metricsByName = systemMetrics.stream()
            .collect(Collectors.toMap(AggregateMetric::name, metric -> metric));
    }
    
    /**
     * Lexicographic comparison based on metric priority order
     */
    @Override
    public int compareTo(ComparableSystemConfiguration other) {
        
        // Compare metric by metric in priority order (order 1, then 2, then 3...)
        for (OrderedMetricMetadata metricMetadata : metricOrder) {
            String metricName = metricMetadata.metricName();
            OptimizationDirection direction = metricMetadata.direction();
            
            // Get metric values for both configurations
            AggregateMetric thisMetric = this.metricsByName.get(metricName);
            AggregateMetric otherMetric = other.metricsByName.get(metricName);
            
            // Handle missing metrics
            if (thisMetric == null && otherMetric == null) {
                continue; // Both missing, try next metric
            }
            if (thisMetric == null) {
                return 1; // This configuration is worse (missing metric)
            }
            if (otherMetric == null) {
                return -1; // Other configuration is worse (missing metric)
            }
            
            // Compare raw values directly based on optimization direction
            double thisValue = thisMetric.value();
            double otherValue = otherMetric.value();
            
            int comparison;
            if (direction == OptimizationDirection.HIGHER_IS_BETTER) {
                // Higher value is better - sort in descending order
                comparison = Double.compare(otherValue, thisValue);
            } else { // LOWER_IS_BETTER
                // Lower value is better - sort in ascending order
                comparison = Double.compare(thisValue, otherValue);
            }
            
            if (comparison != 0) {
                // Tie broken at this priority level
                return comparison;
            }
            
            // Values are equal, continue to next priority metric
        }
        
        return 0; // Complete tie across all metrics
    }
    
    /**
     * Get the value of a specific metric for this configuration
     */
    public Optional<Double> getMetricValue(String metricName) {
        AggregateMetric metric = metricsByName.get(metricName);
        return metric != null ? Optional.of(metric.value()) : Optional.empty();
    }
    
    // Getters
    public String getConfigurationId() { return configurationId; }
    public List<AggregateMetric> getSystemMetrics() { return systemMetrics; }
    
    @Override
    public String toString() {
        return String.format("Configuration[id=%s, metrics=%s]", 
            configurationId, 
            systemMetrics.stream()
                .map(m -> m.name() + "=" + m.value())
                .collect(Collectors.joining(", "))
        );
    }
    
}
