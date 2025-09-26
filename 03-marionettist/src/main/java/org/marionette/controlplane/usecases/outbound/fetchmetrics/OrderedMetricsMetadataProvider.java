package org.marionette.controlplane.usecases.outbound.fetchmetrics;

import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.MetricsConfiguration;

public interface OrderedMetricsMetadataProvider {

    public MetricsConfiguration loadMetrics();
    
}
