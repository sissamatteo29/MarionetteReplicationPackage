package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

public record MetricValueDTO (
    String metricName,
    double value,
    String unit
) {}
