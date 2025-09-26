package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.domain;

import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.OptimizationDirection;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.ServiceAggregator;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.TimeAggregator;

public class PrometheusMetricConfig {

    private String query;
    private TimeAggregator timeAggregator;
    private ServiceAggregator serviceAggregator;
    private OptimizationDirection direction;
    private int order;


    // UI and visual
    private String displayName;   // unique
    private String unit;
    private String description;


    public PrometheusMetricConfig() {
    }


    public PrometheusMetricConfig(String query, TimeAggregator timeAggregator, ServiceAggregator serviceAggregator,
            String displayName, String unit, String description) {
        this.query = query;
        this.timeAggregator = timeAggregator;
        this.serviceAggregator = serviceAggregator;
        this.displayName = displayName;
        this.unit = unit;
        this.description = description;
    }


    @Override
    public String toString() {
        return String.format("%s {\n" +
            "      query: \"%s\"\n" +
            "      timeAggregator: %s\n" +
            "      serviceAggregator: %s\n" +
            "      unit: %s\n" +
            "      description: \"%s\"\n" +
            "    }",
            displayName != null ? displayName : "Unnamed Metric",
            query,
            timeAggregator,
            serviceAggregator,
            unit != null ? unit : "none",
            description != null ? description : "No description");
    }


    public String getQuery() {
        return query;
    }


    public TimeAggregator getTimeAggregator() {
        return timeAggregator;
    }


    public ServiceAggregator getServiceAggregator() {
        return serviceAggregator;
    }


    public String getDisplayName() {
        return displayName;
    }


    public String getUnit() {
        return unit;
    }


    public String getDescription() {
        return description;
    }


    public void setQuery(String query) {
        this.query = query;
    }


    public void setTimeAggregator(TimeAggregator timeAggregator) {
        this.timeAggregator = timeAggregator;
    }


    public void setServiceAggregator(ServiceAggregator serviceAggregator) {
        this.serviceAggregator = serviceAggregator;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    public void setUnit(String unit) {
        this.unit = unit;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public OptimizationDirection getDirection() {
        return direction;
    }


    public void setDirection(OptimizationDirection direction) {
        this.direction = direction;
    }


    public int getOrder() {
        return order;
    }


    public void setOrder(int order) {
        this.order = order;
    }

    
}
