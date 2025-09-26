package org.marionette.controlplane.domain.entities.abntest;

import java.util.List;

import org.marionette.controlplane.usecases.inbound.abntest.domain.GlobalMetricsRegistry;
import org.marionette.controlplane.usecases.inbound.abntest.ranking.SimpleConfigurationRanking;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.MetricsConfiguration;

public record SingleAbnTestResult (
    MetricsConfiguration metricsConfiguration,
    GlobalMetricsRegistry metricsRegistry,
    List<SimpleConfigurationRanking> ranking
) {}
