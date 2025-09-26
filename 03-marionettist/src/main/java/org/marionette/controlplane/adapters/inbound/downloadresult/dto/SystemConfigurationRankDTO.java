package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

import java.util.List;

public record SystemConfigurationRankDTO (
    int position,
    List<ServiceConfigSnapshotDTO> systemConfig,
    List<MetricValueDTO> systemResults,
    List<ServiceLevelResultsDTO> serviceResults

) {}
