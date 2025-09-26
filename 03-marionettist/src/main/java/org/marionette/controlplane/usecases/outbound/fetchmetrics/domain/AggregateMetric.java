package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

import java.time.Instant;

public record AggregateMetric (String name, double value, Instant timestamp, String unit) {}
