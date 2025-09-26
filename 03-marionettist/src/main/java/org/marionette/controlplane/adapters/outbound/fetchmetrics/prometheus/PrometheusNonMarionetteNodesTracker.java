package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus;

import java.util.HashSet;
import java.util.Set;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration.PrometheusConfiguration;
import org.marionette.controlplane.usecases.inbound.abntest.engine.NonMarionetteNodesTracker;

public class PrometheusNonMarionetteNodesTracker implements NonMarionetteNodesTracker {

    private final PrometheusConfiguration config;

    public PrometheusNonMarionetteNodesTracker(PrometheusConfiguration config) {
        this.config = config;
    }

    @Override
    public Set<String> retrieveNonMarionetteNodeNames() {
        return new HashSet<>(config.getIncludedServices());
    }
    
}
