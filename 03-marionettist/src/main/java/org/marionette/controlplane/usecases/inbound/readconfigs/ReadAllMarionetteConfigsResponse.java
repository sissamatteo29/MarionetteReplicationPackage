package org.marionette.controlplane.usecases.inbound.readconfigs;

import java.util.List;

import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;

public record ReadAllMarionetteConfigsResponse (List<ServiceConfigData> serviceConfigs) {}
