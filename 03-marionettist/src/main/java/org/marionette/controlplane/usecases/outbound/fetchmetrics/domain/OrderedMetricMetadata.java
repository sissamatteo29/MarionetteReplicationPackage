package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

public record OrderedMetricMetadata (String metricName, int order, OptimizationDirection direction, String unit) {}
