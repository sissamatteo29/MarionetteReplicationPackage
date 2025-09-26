package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

import java.util.List;

public record SystemMetricsDataPoint(List<ServiceMetricsDataPoint> serviceMetrics) {
    public SystemMetricsDataPoint {
        // Defensive null check
        if (serviceMetrics == null) {
            System.out.println("WARNING: serviceMetrics list was null, using empty list");
            serviceMetrics = List.of();
        } else {
            serviceMetrics = List.copyOf(serviceMetrics);
        }
    }
}
