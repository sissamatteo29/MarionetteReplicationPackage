package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

import java.util.List;

public record AbnTestResultsDTO (
    List<MetricConfigurationDTO> metricConfigs,
    List<SystemConfigurationRankDTO> ranking
) {}
