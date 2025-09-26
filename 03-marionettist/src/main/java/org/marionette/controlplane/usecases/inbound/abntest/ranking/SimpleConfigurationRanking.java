package org.marionette.controlplane.usecases.inbound.abntest.ranking;

import java.util.List;

import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;

public record SimpleConfigurationRanking(
    int rank,                    // 1 = best
    String configurationId,
    List<AggregateMetric> systemMetrics
) {}
