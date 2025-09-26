package org.marionette.controlplane.adapters.inbound.metrics;

import java.util.List;

// Supporting DTO for time series data
public class TimeSeriesDataDTO {
    private String metricName;
    private List<DataPoint> dataPoints;

    public TimeSeriesDataDTO(String metricName, List<DataPoint> dataPoints) {
        this.metricName = metricName;
        this.dataPoints = dataPoints;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public static class DataPoint {
        private long timestamp;
        private double value;

        public DataPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }
    }
}