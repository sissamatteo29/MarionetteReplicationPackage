package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

import java.util.List;

public record ServiceLevelResultsDTO (String serviceName, List<MetricValueDTO> results) {}
