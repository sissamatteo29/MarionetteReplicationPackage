package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

import java.util.List;

import org.marionette.controlplane.usecases.domain.configsnapshot.ServiceSnapshot;

public record ServiceMetricsDataPoint(ServiceSnapshot serviceConfiguration, List<AggregateMetric> metrics) {

    public ServiceMetricsDataPoint {
        // Defensive null check
        if (metrics == null) {
            System.out.println("WARNING: metrics list was null, using empty list");
            metrics = List.of();
        } else {
            metrics = List.copyOf(metrics);
        }
    }
}
