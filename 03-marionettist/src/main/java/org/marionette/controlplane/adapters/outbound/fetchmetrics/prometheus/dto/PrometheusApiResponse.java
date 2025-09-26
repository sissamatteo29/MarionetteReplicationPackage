package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.dto;

import java.util.List;

public class PrometheusApiResponse<T> {
    private String status;
    private T data;
    private String errorType;
    private String error;
    private List<String> warnings;
    private List<String> infos;
    
    // constructors, getters, setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public List<String> getInfos() { return infos; }
    public void setInfos(List<String> infos) { this.infos = infos; }
}