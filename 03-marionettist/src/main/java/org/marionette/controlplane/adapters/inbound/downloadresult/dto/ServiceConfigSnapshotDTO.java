package org.marionette.controlplane.adapters.inbound.downloadresult.dto;

import java.util.List;

public record ServiceConfigSnapshotDTO (
    String serviceName,
    List<ClassConfigSnapshotDTO> classConfigs
) {}
