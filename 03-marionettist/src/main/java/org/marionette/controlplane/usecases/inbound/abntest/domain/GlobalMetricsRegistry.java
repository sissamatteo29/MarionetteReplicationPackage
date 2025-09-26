package org.marionette.controlplane.usecases.inbound.abntest.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.marionette.controlplane.usecases.domain.configsnapshot.SystemConfigurationSnapshot;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.SystemMetricsDataPoint;

public class GlobalMetricsRegistry {

    // String identifiers like conf-1, conf-2...
    private final String keyPattern = "conf-";
    private final Map<String, SystemConfigurationSnapshot> globalConfigs = new ConcurrentHashMap<>();
    private final Map<String, SystemMetricsDataPoint> globalMetrics = new ConcurrentHashMap<>();

    private final AtomicInteger globalConfigCounter = new AtomicInteger(0);

    public synchronized void putSystemMetrics(SystemConfigurationSnapshot systemConfigSnapshot,
            SystemMetricsDataPoint dataPoint) {
        String identifier = keyPattern + globalConfigCounter.getAndIncrement();
        globalConfigs.put(identifier, systemConfigSnapshot);
        globalMetrics.put(identifier, dataPoint);
    }

    public SystemMetricsDataPoint getSystemDataPoint(int index) {
        return globalMetrics.get(keyPattern + index);
    }

    public SystemMetricsDataPoint getSystemDataPoint(String configId) {
        return globalMetrics.get(configId);
    }

    public SystemConfigurationSnapshot getSystemConfig(int index) {
        return globalConfigs.get(keyPattern + index);
    }

    public SystemConfigurationSnapshot getSystemConfig(String configId) {
        return globalConfigs.get(configId);
    }

    public Map<String, SystemMetricsDataPoint> getAllMetrics() {
        return globalMetrics;
    }

}
