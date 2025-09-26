package org.marionette.controlplane.usecases.outbound.fetchmetrics.domain;

import java.util.Iterator;
import java.util.List;

public class MetricsConfiguration implements Iterable<OrderedMetricMetadata> {

    private final List<OrderedMetricMetadata> metricsConfig;

    public MetricsConfiguration(List<OrderedMetricMetadata> metricsConfig) {
        this.metricsConfig = metricsConfig
                .stream()
                .sorted((el1, el2) -> Integer.compare(el1.order(), el2.order()))
                .toList();
    }

    public List<OrderedMetricMetadata> getMetricsConfig() {
        return metricsConfig;
    }

    @Override
    public Iterator<OrderedMetricMetadata> iterator() {
        return metricsConfig.iterator();
    }

    @Override
    public String toString() {
        if (metricsConfig.isEmpty()) {
            return "MetricsConfiguration{empty}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("MetricsConfiguration{\n");
        sb.append(String.format("  totalMetrics: %d\n", metricsConfig.size()));
        sb.append("  priorityOrder: [\n");

        for (int i = 0; i < metricsConfig.size(); i++) {
            OrderedMetricMetadata metric = metricsConfig.get(i);
            sb.append(String.format("    [%d] order=%d, name='%s', direction=%s",
                    i + 1,
                    metric.order(),
                    metric.metricName(),
                    metric.direction()));

            if (i < metricsConfig.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

}
