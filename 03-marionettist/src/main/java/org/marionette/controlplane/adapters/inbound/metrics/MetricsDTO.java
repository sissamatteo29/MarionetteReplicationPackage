package org.marionette.controlplane.adapters.inbound.metrics;

import java.time.Instant;
import java.util.List;

public class MetricsDTO {
    private String serviceName;
    private List<TimeSeriesDataDTO> responseTime;
    private List<TimeSeriesDataDTO> requestRate;
    private List<TimeSeriesDataDTO> errorRate;
    private List<TimeSeriesDataDTO> cpuUsage;
    private List<TimeSeriesDataDTO> memoryUsage;
    private Instant startTime;
    private Instant endTime;

    public MetricsDTO(String serviceName, 
                     List<TimeSeriesDataDTO> responseTime,
                     List<TimeSeriesDataDTO> requestRate, 
                     List<TimeSeriesDataDTO> errorRate,
                     List<TimeSeriesDataDTO> cpuUsage,
                     List<TimeSeriesDataDTO> memoryUsage,
                     Instant startTime, 
                     Instant endTime) {
        this.serviceName = serviceName;
        this.responseTime = responseTime;
        this.requestRate = requestRate;
        this.errorRate = errorRate;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<TimeSeriesDataDTO> getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(List<TimeSeriesDataDTO> responseTime) {
        this.responseTime = responseTime;
    }

    public List<TimeSeriesDataDTO> getRequestRate() {
        return requestRate;
    }

    public void setRequestRate(List<TimeSeriesDataDTO> requestRate) {
        this.requestRate = requestRate;
    }

    public List<TimeSeriesDataDTO> getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(List<TimeSeriesDataDTO> errorRate) {
        this.errorRate = errorRate;
    }

    public List<TimeSeriesDataDTO> getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(List<TimeSeriesDataDTO> cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public List<TimeSeriesDataDTO> getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(List<TimeSeriesDataDTO> memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}


