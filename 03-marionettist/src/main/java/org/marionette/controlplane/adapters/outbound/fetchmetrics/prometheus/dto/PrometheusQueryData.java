package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.dto;

import java.util.List;

public class PrometheusQueryData {
    private String resultType; // "matrix" | "vector" | "scalar" | "string"
    private List<PrometheusResult> result;
    
    // getters, setters
    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }
    
    public List<PrometheusResult> getResult() { return result; }
    public void setResult(List<PrometheusResult> result) { this.result = result; }
}
