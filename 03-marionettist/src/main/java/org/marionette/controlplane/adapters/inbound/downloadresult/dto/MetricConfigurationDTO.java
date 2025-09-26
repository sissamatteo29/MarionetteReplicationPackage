package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

public record MetricConfigurationDTO (
    String metricName,
    int order,
    String unit,
    String direction
) {}
