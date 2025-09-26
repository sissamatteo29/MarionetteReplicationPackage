package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus;

import java.util.List;
import java.util.stream.Collectors;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration.PrometheusConfiguration;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.OrderedMetricsMetadataProvider;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.MetricsConfiguration;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.OrderedMetricMetadata;

public class PrometheusOrderedMetricsMetadataAdapter implements OrderedMetricsMetadataProvider {

    private final PrometheusConfiguration prometheusConfiguration;

    public PrometheusOrderedMetricsMetadataAdapter(PrometheusConfiguration prometheusConfiguration) {
        this.prometheusConfiguration = prometheusConfiguration;
    }

    @Override
    public MetricsConfiguration loadMetrics() {
        
        List<OrderedMetricMetadata> domainMetricsModel = prometheusConfiguration.getMetrics()
            .stream()
            .map(el -> new OrderedMetricMetadata(el.getDisplayName(), el.getOrder(), el.getDirection(), el.getUnit()))
            .collect(Collectors.toList());

        return new MetricsConfiguration(domainMetricsModel);
    }
    
}
