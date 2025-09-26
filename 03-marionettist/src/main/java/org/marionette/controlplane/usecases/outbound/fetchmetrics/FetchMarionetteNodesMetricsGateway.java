package org.marionette.controlplane.usecases.outbound.fetchmetrics;

import java.time.Duration;
import java.util.List;

import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;

public interface FetchMarionetteNodesMetricsGateway {

    public List<AggregateMetric> fetchMetricsForService(String serviceName, Duration timeSpan, Duration samplingPeriod);
    
}
