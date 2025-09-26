package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.dto;

import java.util.List;
import java.util.Map;

public class PrometheusResult {
    private Map<String, String> metric;
    private List<Object[]> values; // For range queries (matrix)
    private Object[] value;        // For instant queries (vector)
    private List<Object[]> histograms; // For histogram data
    private Object[] histogram;    // For instant histogram data
    
    // getters, setters
    public Map<String, String> getMetric() { return metric; }
    public void setMetric(Map<String, String> metric) { this.metric = metric; }
    
    public List<Object[]> getValues() { return values; }
    public void setValues(List<Object[]> values) { this.values = values; }
    
    public Object[] getValue() { return value; }
    public void setValue(Object[] value) { this.value = value; }
    public List<Object[]> getHistograms() {
        return histograms;
    }
    public void setHistograms(List<Object[]> histograms) {
        this.histograms = histograms;
    }
    public Object[] getHistogram() {
        return histogram;
    }
    public void setHistogram(Object[] histogram) {
        this.histogram = histogram;
    }

}
